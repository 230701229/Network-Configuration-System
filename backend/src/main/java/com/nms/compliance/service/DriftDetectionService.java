package com.nms.compliance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nms.compliance.dto.ConfigurationDiffDTO;
import com.nms.compliance.dto.DriftEventDTO;
import com.nms.compliance.entity.*;
import com.nms.compliance.exception.ResourceNotFoundException;
import com.nms.compliance.network.factory.DeviceProviderFactory;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.network.provider.NetworkDeviceProvider;
import com.nms.compliance.repository.ConfigurationSnapshotRepository;
import com.nms.compliance.repository.DeviceRepository;
import com.nms.compliance.repository.DriftEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DriftDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DriftDetectionService.class);

    private final DriftEventRepository driftEventRepository;
    private final ConfigurationSnapshotRepository snapshotRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceProviderFactory providerFactory;
    private final AlertService alertService;
    private final ComplianceService complianceService;
    private final SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    public DriftDetectionService(DriftEventRepository driftEventRepository,
                                 ConfigurationSnapshotRepository snapshotRepository,
                                 DeviceRepository deviceRepository,
                                 DeviceProviderFactory providerFactory,
                                 @Lazy AlertService alertService,
                                 @Lazy ComplianceService complianceService,
                                 SseNotificationService sseNotificationService,
                                 ObjectMapper objectMapper) {
        this.driftEventRepository = driftEventRepository;
        this.snapshotRepository = snapshotRepository;
        this.deviceRepository = deviceRepository;
        this.providerFactory = providerFactory;
        this.alertService = alertService;
        this.complianceService = complianceService;
        this.sseNotificationService = sseNotificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Automated Live Configuration Drift Detection for a single device.
     * Compares live configuration against the previous snapshot in time.
     * Prevents duplicate events when configuration is unchanged (Req 18).
     */
    @Transactional
    public List<DriftEvent> detectDeviceConfigurationDrift(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        NormalizedConfiguration currentConfig;
        try {
            NetworkDeviceProvider provider = providerFactory.getProvider(device.getManagementProtocol());
            currentConfig = provider.getConfiguration(device);
        } catch (Exception e) {
            log.warn("Drift check: Could not reach device {} ({}): {}", device.getHostname(), device.getIpAddress(), e.getMessage());
            if (device.getStatus() != Device.DeviceStatus.UNREACHABLE) {
                device.setStatus(Device.DeviceStatus.UNREACHABLE);
                deviceRepository.save(device);
                sseNotificationService.broadcastDeviceStatus(device.getId(), device.getHostname(), "UNREACHABLE");
            }
            return Collections.emptyList();
        }

        List<ConfigurationSnapshot> snapshots = snapshotRepository.findByDeviceIdOrderByRetrievedAtDesc(deviceId);
        String currentJson;
        try {
            currentJson = objectMapper.writeValueAsString(currentConfig.parameters());
        } catch (Exception e) {
            log.error("Failed to serialize config for device {}: {}", device.getHostname(), e.getMessage());
            return Collections.emptyList();
        }

        String currentHash = sha256(currentJson);

        // If no prior snapshot exists, save this as the baseline snapshot and run initial scan
        if (snapshots.isEmpty()) {
            ConfigurationSnapshot baseline = ConfigurationSnapshot.builder()
                    .device(device)
                    .snapshotData(currentJson)
                    .configHash(currentHash)
                    .retrievalProtocol(device.getManagementProtocol())
                    .retrievedAt(LocalDateTime.now())
                    .notes("INITIAL_BASELINE")
                    .build();
            ConfigurationSnapshot savedBaseline = snapshotRepository.save(baseline);

            device.setLastSeen(LocalDateTime.now());
            device.setStatus(Device.DeviceStatus.ONLINE);
            deviceRepository.save(device);

            complianceService.evaluateAndSaveScan(device, currentConfig, savedBaseline, "INITIAL_BASELINE");
            return Collections.emptyList();
        }

        ConfigurationSnapshot previousSnapshot = snapshots.get(0);
        boolean hashChanged = !Objects.equals(currentHash, previousSnapshot.getConfigHash());

        // Requirement 18: If configuration has not changed from the previous snapshot, avoid duplicates!
        if (!hashChanged) {
            device.setLastSeen(LocalDateTime.now());
            if (device.getStatus() != Device.DeviceStatus.ONLINE) {
                device.setStatus(Device.DeviceStatus.ONLINE);
                deviceRepository.save(device);
                sseNotificationService.broadcastDeviceStatus(device.getId(), device.getHostname(), "ONLINE");
            }
            return Collections.emptyList();
        }

        // Configuration HAS changed! Save new monitored snapshot
        ConfigurationSnapshot newSnapshot = ConfigurationSnapshot.builder()
                .device(device)
                .snapshotData(currentJson)
                .configHash(currentHash)
                .retrievalProtocol(device.getManagementProtocol())
                .retrievedAt(LocalDateTime.now())
                .notes("MONITORED")
                .build();
        ConfigurationSnapshot savedSnapshot = snapshotRepository.save(newSnapshot);

        device.setLastSeen(LocalDateTime.now());
        device.setStatus(Device.DeviceStatus.ONLINE);
        deviceRepository.save(device);

        Map<String, String> previousParams = parseJsonMap(previousSnapshot.getSnapshotData());
        Map<String, String> currentParams = currentConfig.parameters();

        // 1. Run deterministic compliance evaluation on the new configuration (Req 14)
        ComplianceScan previousScan = complianceService.getLatestScanForDevice(deviceId);
        BigDecimal previousScore = (previousScan != null) ? previousScan.getComplianceScore() : BigDecimal.valueOf(100.0);

        ComplianceScan currentScan = complianceService.evaluateAndSaveScan(device, currentConfig, savedSnapshot, "DRIFT_MONITOR");
        BigDecimal currentScore = (currentScan != null) ? currentScan.getComplianceScore() : previousScore;

        String complianceImpact = String.format("Compliance score: %.1f%% → %.1f%% (%s)",
                previousScore.doubleValue(),
                currentScore.doubleValue(),
                currentScan != null && currentScan.getComplianceStatus() != null ? currentScan.getComplianceStatus().name() : "UPDATED");

        // 2. Identify exact changed parameters
        Set<String> changedParameters = findChangedParameters(previousParams, currentParams);
        List<DriftEvent> detectedDrifts = new ArrayList<>();
        List<DriftEvent> openDrifts = driftEventRepository.findByDeviceIdAndStatus(deviceId, DriftEvent.DriftStatus.OPEN);

        for (String param : changedParameters) {
            String prevVal = previousParams.get(param);
            String currVal = currentParams.get(param);

            PolicyRule.Severity severity = determineSeverity(param);

            // Format human-readable description: telnet-enabled: false → true
            String prevDisplay = prevVal != null ? prevVal : "<absent>";
            String currDisplay = currVal != null ? currVal : "<removed>";
            String description = String.format("%s: %s → %s. %s", param, prevDisplay, currDisplay, complianceImpact);

            // Check if there is already an open drift on this parameter
            Optional<DriftEvent> existingOpenOpt = openDrifts.stream()
                    .filter(d -> d.getParameter().equalsIgnoreCase(param))
                    .findFirst();

            DriftEvent drift;
            if (existingOpenOpt.isPresent()) {
                drift = existingOpenOpt.get();
                drift.setScan(currentScan);
                drift.setExpectedValue(prevDisplay);
                drift.setActualValue(currDisplay);
                drift.setDescription(description);
                drift.setDetectedAt(LocalDateTime.now());
            } else {
                drift = DriftEvent.builder()
                        .device(device)
                        .scan(currentScan)
                        .policy(device.getAppliedPolicy())
                        .parameter(param)
                        .expectedValue(prevDisplay)
                        .actualValue(currDisplay)
                        .severity(severity)
                        .status(DriftEvent.DriftStatus.OPEN)
                        .description(description)
                        .remediation(suggestRemediation(param, prevVal))
                        .detectedAt(LocalDateTime.now())
                        .build();
            }

            DriftEvent saved = driftEventRepository.save(drift);
            detectedDrifts.add(saved);

            // Raise alert for important drift (HIGH/CRITICAL)
            alertService.createDriftAlert(device, saved);

            // Broadcast real-time SSE drift event to React dashboard
            sseNotificationService.broadcastDriftEvent(
                    device.getId(),
                    device.getHostname(),
                    String.format("Drift detected on %s: %s (%s → %s)", device.getHostname(), param, prevDisplay, currDisplay),
                    mapToDTO(saved)
            );
        }

        // 3. Auto-resolve drifts for parameters that have returned to baseline
        ConfigurationSnapshot baselineSnapshot = snapshots.stream()
                .filter(s -> "INITIAL_BASELINE".equals(s.getNotes()) || "BASELINE".equals(s.getNotes()))
                .findFirst()
                .orElse(snapshots.get(snapshots.size() - 1));
        Map<String, String> baselineParams = parseJsonMap(baselineSnapshot.getSnapshotData());

        for (DriftEvent openDrift : openDrifts) {
            String param = openDrift.getParameter();
            String baseVal = baselineParams.get(param);
            String currVal = currentParams.get(param);
            if (baseVal != null && Objects.equals(baseVal, currVal)) {
                openDrift.setStatus(DriftEvent.DriftStatus.RESOLVED);
                openDrift.setResolvedAt(LocalDateTime.now());
                driftEventRepository.save(openDrift);
                log.info("Drift on parameter {} for device {} automatically marked RESOLVED", param, device.getHostname());
            }
        }

        log.info("Drift detection completed for {}: {} change(s) detected. Impact: {}",
                device.getHostname(), detectedDrifts.size(), complianceImpact);

        return detectedDrifts;
    }

    /**
     * Automated Live Configuration Drift Detection for ALL devices.
     */
    @Transactional
    public List<DriftEvent> detectAllDevicesDrift() {
        List<DriftEvent> allNewDrifts = new ArrayList<>();
        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            try {
                List<DriftEvent> drifts = detectDeviceConfigurationDrift(device.getId());
                allNewDrifts.addAll(drifts);
            } catch (Exception e) {
                log.warn("Drift check failed for device {}: {}", device.getHostname(), e.getMessage());
            }
        }
        return allNewDrifts;
    }

    /**
     * Get detailed baseline vs current diff for a device.
     */
    public ConfigurationDiffDTO getConfigurationDiff(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        List<ConfigurationSnapshot> snapshots = snapshotRepository.findByDeviceIdOrderByRetrievedAtDesc(deviceId);
        if (snapshots.isEmpty()) {
            return ConfigurationDiffDTO.builder()
                    .deviceId(deviceId)
                    .hostname(device.getHostname())
                    .hasDrift(false)
                    .totalChanges(0)
                    .changes(Collections.emptyList())
                    .baselineConfig(Collections.emptyMap())
                    .currentConfig(Collections.emptyMap())
                    .build();
        }

        ConfigurationSnapshot latestSnapshot = snapshots.get(0);
        ConfigurationSnapshot baselineSnapshot = snapshots.stream()
                .filter(s -> "INITIAL_BASELINE".equals(s.getNotes()) || "BASELINE".equals(s.getNotes()))
                .findFirst()
                .orElse(snapshots.get(snapshots.size() - 1));

        Map<String, String> baseMap = parseJsonMap(baselineSnapshot.getSnapshotData());
        Map<String, String> currMap = parseJsonMap(latestSnapshot.getSnapshotData());

        List<ConfigurationDiffDTO.ParameterDiff> diffs = new ArrayList<>();
        Set<String> changedKeys = findChangedParameters(baseMap, currMap);

        for (String key : changedKeys) {
            String b = baseMap.get(key);
            String c = currMap.get(key);

            String type = (b == null) ? "ADDED" : (c == null ? "REMOVED" : "MODIFIED");
            diffs.add(ConfigurationDiffDTO.ParameterDiff.builder()
                    .parameter(key)
                    .baselineValue(b != null ? b : "<absent>")
                    .currentValue(c != null ? c : "<absent>")
                    .changeType(type)
                    .severity(determineSeverity(key).name())
                    .remediation(suggestRemediation(key, b))
                    .build());
        }

        return ConfigurationDiffDTO.builder()
                .deviceId(deviceId)
                .hostname(device.getHostname())
                .hasDrift(!diffs.isEmpty())
                .totalChanges(diffs.size())
                .baselineTime(baselineSnapshot.getRetrievedAt())
                .currentTime(latestSnapshot.getRetrievedAt())
                .baselineHash(baselineSnapshot.getConfigHash())
                .currentHash(latestSnapshot.getConfigHash())
                .changes(diffs)
                .baselineConfig(baseMap)
                .currentConfig(currMap)
                .build();
    }

    /**
     * Mark current configuration as new golden baseline.
     */
    @Transactional
    public void setBaseline(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        List<ConfigurationSnapshot> snapshots = snapshotRepository.findByDeviceIdOrderByRetrievedAtDesc(deviceId);
        if (!snapshots.isEmpty()) {
            ConfigurationSnapshot latest = snapshots.get(0);
            latest.setNotes("BASELINE");
            snapshotRepository.save(latest);

            // Resolve existing open drifts because baseline is accepted
            List<DriftEvent> openDrifts = driftEventRepository.findByDeviceIdAndStatus(deviceId, DriftEvent.DriftStatus.OPEN);
            for (DriftEvent drift : openDrifts) {
                drift.setStatus(DriftEvent.DriftStatus.RESOLVED);
                drift.setResolvedAt(LocalDateTime.now());
                driftEventRepository.save(drift);
            }

            sseNotificationService.broadcast(com.nms.compliance.dto.RealtimeNotificationDTO.builder()
                    .eventType(com.nms.compliance.dto.RealtimeNotificationDTO.EventType.DRIFT_RESOLVED)
                    .deviceId(deviceId)
                    .hostname(device.getHostname())
                    .message("Configuration accepted as golden baseline for " + device.getHostname())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    @Transactional
    public DriftEvent updateDriftStatus(Long driftId, DriftEvent.DriftStatus status) {
        DriftEvent drift = driftEventRepository.findById(driftId)
                .orElseThrow(() -> new ResourceNotFoundException("Drift event not found: " + driftId));
        drift.setStatus(status);
        if (status == DriftEvent.DriftStatus.RESOLVED) {
            drift.setResolvedAt(LocalDateTime.now());
        }
        return driftEventRepository.save(drift);
    }

    public List<DriftEvent> getDriftEventsForDevice(Long deviceId) {
        return driftEventRepository.findByDeviceIdOrderByDetectedAtDesc(deviceId);
    }

    public List<DriftEvent> getAllDriftEvents() {
        return driftEventRepository.findAllByOrderByDetectedAtDesc();
    }

    public List<DriftEvent> getAllOpenDriftEvents() {
        return driftEventRepository.findByStatus(DriftEvent.DriftStatus.OPEN);
    }

    public Set<String> findChangedParameters(Map<String, String> prev, Map<String, String> curr) {
        Set<String> allKeys = new TreeSet<>();
        if (prev != null) allKeys.addAll(prev.keySet());
        if (curr != null) allKeys.addAll(curr.keySet());

        Set<String> rawChanges = new LinkedHashSet<>();
        for (String k : allKeys) {
            String pVal = (prev != null) ? prev.get(k) : null;
            String cVal = (curr != null) ? curr.get(k) : null;
            if (!Objects.equals(pVal, cVal)) {
                rawChanges.add(k);
            }
        }

        // Deduplicate pairs like 'telnet' and 'telnet-enabled' -> prioritize 'telnet-enabled'
        Set<String> deduped = new LinkedHashSet<>();
        for (String k : rawChanges) {
            if (k.endsWith("-enabled")) {
                deduped.add(k);
            } else if (rawChanges.contains(k + "-enabled")) {
                // skip short form because *-enabled exists
                continue;
            } else if ("password_encryption".equals(k) && rawChanges.contains("password-encryption")) {
                continue;
            } else {
                deduped.add(k);
            }
        }
        return deduped;
    }

    private PolicyRule.Severity determineSeverity(String param) {
        String p = param.toLowerCase();
        if (p.contains("telnet") || p.contains("password") || p.contains("secret") || p.contains("firewall")) {
            return PolicyRule.Severity.CRITICAL;
        } else if (p.contains("ssh") || p.contains("http") || p.contains("snmp") || p.contains("ospf")) {
            return PolicyRule.Severity.HIGH;
        } else if (p.contains("ntp") || p.contains("logging") || p.contains("spanning")) {
            return PolicyRule.Severity.MEDIUM;
        }
        return PolicyRule.Severity.LOW;
    }

    private String suggestRemediation(String param, String baselineValue) {
        String p = param.toLowerCase();
        if (p.contains("telnet")) return "Disable Telnet: transport input ssh / no service telnet";
        if (p.contains("password-encryption") || p.contains("password_encryption")) return "Enable service password-encryption";
        if (p.contains("ssh")) return "Enable SSH: ip ssh version 2 / crypto key generate rsa modulus 2048";
        if (p.contains("ntp")) return "Configure NTP server: ntp server 10.0.0.1";
        if (p.contains("http")) return "Disable HTTP server: no ip http server";
        if (p.contains("snmp")) return "Configure secure SNMP: snmp-server community <string> RO";
        return baselineValue != null ? "Restore parameter '" + param + "' to '" + baselineValue + "'" : "Review parameter " + param;
    }

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(data.hashCode());
        }
    }

    public DriftEventDTO mapToDTO(DriftEvent d) {
        DriftEventDTO dto = new DriftEventDTO();
        dto.setId(d.getId());
        if (d.getDevice() != null) {
            dto.setDeviceId(d.getDevice().getId());
            dto.setDeviceHostname(d.getDevice().getHostname());
        }
        if (d.getScan() != null) {
            dto.setScanId(d.getScan().getId());
        }
        if (d.getPolicy() != null) {
            dto.setPolicyId(d.getPolicy().getId());
        }
        dto.setParameter(d.getParameter());
        dto.setExpectedValue(d.getExpectedValue());
        dto.setActualValue(d.getActualValue());
        dto.setSeverity(d.getSeverity() != null ? d.getSeverity().name() : "MEDIUM");
        dto.setStatus(d.getStatus() != null ? d.getStatus().name() : "OPEN");
        dto.setDescription(d.getDescription());
        dto.setRemediation(d.getRemediation());
        dto.setDetectedAt(d.getDetectedAt());
        dto.setResolvedAt(d.getResolvedAt());
        return dto;
    }
}
