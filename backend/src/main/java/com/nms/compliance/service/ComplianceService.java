package com.nms.compliance.service;

import com.nms.compliance.compliance.ComplianceEngine;
import com.nms.compliance.compliance.ComplianceScanResult;
import com.nms.compliance.compliance.RuleEvaluationResult;
import com.nms.compliance.dto.AIAnalysisDTO;
import com.nms.compliance.dto.ComplianceScanDTO;
import com.nms.compliance.entity.*;
import com.nms.compliance.exception.ResourceNotFoundException;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;
    private final PolicyService policyService;
    private final ComplianceEngine complianceEngine;
    private final ComplianceScanRepository scanRepository;
    private final ComplianceResultRepository resultRepository;
    private final AlertService alertService;
    private final AIServiceClient aiServiceClient;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final ConfigurationSnapshotRepository snapshotRepository;
    private final SseNotificationService sseNotificationService;

    public ComplianceService(DeviceService deviceService,
                             DeviceRepository deviceRepository,
                             PolicyService policyService,
                             ComplianceEngine complianceEngine,
                             ComplianceScanRepository scanRepository,
                             ComplianceResultRepository resultRepository,
                             AlertService alertService,
                             AIServiceClient aiServiceClient,
                             AIAnalysisRepository aiAnalysisRepository,
                             ConfigurationSnapshotRepository snapshotRepository,
                             SseNotificationService sseNotificationService) {
        this.deviceService = deviceService;
        this.deviceRepository = deviceRepository;
        this.policyService = policyService;
        this.complianceEngine = complianceEngine;
        this.scanRepository = scanRepository;
        this.resultRepository = resultRepository;
        this.alertService = alertService;
        this.aiServiceClient = aiServiceClient;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.snapshotRepository = snapshotRepository;
        this.sseNotificationService = sseNotificationService;
    }

    public ComplianceScan getLatestScanForDevice(Long deviceId) {
        return scanRepository.findTop1ByDeviceIdOrderByScannedAtDesc(deviceId).orElse(null);
    }

    @Transactional
    public ComplianceScanDTO scanDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        NormalizedConfiguration config = deviceService.retrieveConfiguration(deviceId);
        ConfigurationSnapshot snapshot = snapshotRepository.findByDeviceIdOrderByRetrievedAtDesc(deviceId)
                .stream().findFirst().orElse(null);

        ComplianceScan savedScan = evaluateAndSaveScan(device, config, snapshot, "MANUAL");
        if (savedScan == null) {
            throw new RuntimeException("No active policy found for device");
        }

        return mapToDTO(savedScan);
    }

    @Transactional
    public ComplianceScan evaluateAndSaveScan(Device device,
                                              NormalizedConfiguration config,
                                              ConfigurationSnapshot snapshot,
                                              String triggeredBy) {
        Policy policy = policyService.findApplicablePolicy(device);
        if (policy == null) {
            log.warn("No active policy found for device {}", device.getHostname());
            return null;
        }

        ComplianceScanResult engineResult = complianceEngine.evaluate(config, policy);

        ComplianceScan scan = ComplianceScan.builder()
                .device(device)
                .policy(policy)
                .snapshot(snapshot)
                .complianceScore(engineResult.getScore())
                .complianceStatus(engineResult.getStatus())
                .totalRules(engineResult.getResults().size())
                .passedRules((int) engineResult.getResults().stream().filter(RuleEvaluationResult::isPassed).count())
                .failedRules((int) engineResult.getResults().stream().filter(r -> !r.isPassed()).count())
                .triggeredBy(triggeredBy != null ? triggeredBy : "AUTOMATED")
                .scannedAt(LocalDateTime.now())
                .build();

        countViolations(engineResult.getResults(), scan);
        ComplianceScan savedScan = scanRepository.save(scan);

        List<ComplianceResult> savedResults = new ArrayList<>();
        for (RuleEvaluationResult res : engineResult.getResults()) {
            ComplianceResult cr = ComplianceResult.builder()
                    .scan(savedScan)
                    .rule(res.getRule())
                    .parameter(res.getRule().getParameter())
                    .expectedValue(res.getRule().getExpectedValue())
                    .actualValue(res.getActualValue())
                    .passed(res.isPassed())
                    .severity(res.getRule().getSeverity())
                    .description(res.getRule().getDescription())
                    .remediation(res.getRule().getRemediation())
                    .build();
            savedResults.add(resultRepository.save(cr));
        }

        // Asynchronously call AI service to explain violations (deterministic rule engine set score)
        CompletableFuture.runAsync(() -> {
            try {
                List<ComplianceResult> violations = savedResults.stream().filter(r -> !r.isPassed()).toList();
                if (!violations.isEmpty()) {
                    AIAnalysisDTO dto = aiServiceClient.analyzeViolations(savedScan, violations);
                    AIAnalysis entity = AIAnalysis.builder()
                            .scan(savedScan)
                            .device(savedScan.getDevice())
                            .summary(dto.getSummary())
                            .analysis(dto.getAnalysis())
                            .recommendations(dto.getRecommendations())
                            .priority(dto.getPriority())
                            .modelUsed(dto.getModelUsed())
                            .aiAvailable(dto.isAiAvailable())
                            .createdAt(LocalDateTime.now())
                            .build();
                    aiAnalysisRepository.save(entity);
                }
            } catch (Exception e) {
                log.warn("Async AI call failed: {}", e.getMessage());
            }
        });

        // Broadcast real-time scan update via SSE
        ComplianceScanDTO dto = mapToDTO(savedScan);
        sseNotificationService.broadcastScanCompleted(
                device.getId(),
                device.getHostname(),
                String.format("Compliance evaluated for %s: %s%% (%s)",
                        device.getHostname(),
                        savedScan.getComplianceScore(),
                        savedScan.getComplianceStatus().name()),
                dto
        );

        return savedScan;
    }

    private void countViolations(List<RuleEvaluationResult> results, ComplianceScan scan) {
        int critical = 0, high = 0, medium = 0, low = 0;
        for (RuleEvaluationResult res : results) {
            if (!res.isPassed()) {
                switch (res.getRule().getSeverity()) {
                    case CRITICAL -> critical++;
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW -> low++;
                }
            }
        }
        scan.setCriticalViolations(critical);
        scan.setHighViolations(high);
        scan.setMediumViolations(medium);
        scan.setLowViolations(low);
    }

    public List<ComplianceScanDTO> scanAllDevices() {
        List<ComplianceScanDTO> results = new ArrayList<>();
        deviceService.getAllDevices().forEach(d -> {
            try {
                results.add(scanDevice(d.getId()));
            } catch (Exception e) {
                log.warn("Failed to scan device {}: {}", d.getId(), e.getMessage());
            }
        });
        return results;
    }

    public ComplianceScanDTO mapToDTO(ComplianceScan scan) {
        ComplianceScanDTO dto = new ComplianceScanDTO();
        dto.setId(scan.getId());
        dto.setDeviceId(scan.getDevice().getId());
        dto.setDeviceHostname(scan.getDevice().getHostname());
        dto.setPolicyId(scan.getPolicy() != null ? scan.getPolicy().getId() : null);
        dto.setPolicyName(scan.getPolicy() != null ? scan.getPolicy().getPolicyName() : null);
        dto.setComplianceScore(scan.getComplianceScore());
        dto.setComplianceStatus(scan.getComplianceStatus() != null ? scan.getComplianceStatus().name() : null);
        dto.setTotalRules(scan.getTotalRules());
        dto.setPassedRules(scan.getPassedRules());
        dto.setFailedRules(scan.getFailedRules());
        dto.setCriticalViolations(scan.getCriticalViolations());
        dto.setHighViolations(scan.getHighViolations());
        dto.setMediumViolations(scan.getMediumViolations());
        dto.setLowViolations(scan.getLowViolations());
        dto.setTriggeredBy(scan.getTriggeredBy());
        dto.setScannedAt(scan.getScannedAt());
        return dto;
    }
}
