package com.nms.compliance.service;

import com.nms.compliance.dto.AlertDTO;
import com.nms.compliance.dto.DashboardSummaryDTO;
import com.nms.compliance.dto.DashboardTrendsDTO;
import com.nms.compliance.dto.DeviceComplianceStatusDTO;
import com.nms.compliance.entity.Alert;
import com.nms.compliance.entity.ComplianceScan;
import com.nms.compliance.entity.Device;
import com.nms.compliance.entity.DriftEvent;
import com.nms.compliance.repository.AlertRepository;
import com.nms.compliance.repository.ComplianceScanRepository;
import com.nms.compliance.repository.DeviceRepository;
import com.nms.compliance.repository.DriftEventRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final DriftEventRepository driftEventRepository;
    private final AlertRepository alertRepository;
    private final ComplianceScanRepository scanRepository;

    public DashboardService(DeviceRepository deviceRepository, DriftEventRepository driftEventRepository,
                            AlertRepository alertRepository, ComplianceScanRepository scanRepository) {
        this.deviceRepository = deviceRepository;
        this.driftEventRepository = driftEventRepository;
        this.alertRepository = alertRepository;
        this.scanRepository = scanRepository;
    }

    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        // Device counts
        summary.setTotalDevices(deviceRepository.count());

        List<DriftEvent> openDrifts = driftEventRepository.findByStatus(DriftEvent.DriftStatus.OPEN);
        long devicesWithDrift = openDrifts.stream()
                .map(d -> d.getDevice().getId()).distinct().count();
        summary.setDevicesWithDrift(devicesWithDrift);
        summary.setCompliantDevices(summary.getTotalDevices() - devicesWithDrift);

        // Critical violations (open drift events that are CRITICAL severity)
        long criticalViolations = openDrifts.stream()
                .filter(d -> d.getSeverity() != null && "CRITICAL".equals(d.getSeverity().name()))
                .count();
        summary.setCriticalViolations(criticalViolations);

        // Average compliance score from latest scans per device
        List<Device> devices = deviceRepository.findAll();
        double avgScore = devices.stream()
                .mapToDouble(device -> {
                    Optional<ComplianceScan> latest = scanRepository.findTop1ByDeviceIdOrderByScannedAtDesc(device.getId());
                    return latest.map(s -> s.getComplianceScore().doubleValue()).orElse(100.0);
                })
                .average()
                .orElse(100.0);
        summary.setAverageComplianceScore(Math.round(avgScore * 100.0) / 100.0);

        // Recent alerts — last 5 open alerts as full objects
        List<Alert> openAlerts = alertRepository.findByStatusOrderByDetectedAtDesc(Alert.AlertStatus.OPEN);
        List<AlertDTO> recentAlerts = openAlerts.stream()
                .limit(5)
                .map(this::mapAlertToDTO)
                .collect(Collectors.toList());
        summary.setRecentAlerts(recentAlerts);

        return summary;
    }

    public DashboardTrendsDTO getTrends() {
        DashboardTrendsDTO trends = new DashboardTrendsDTO();

        // Build device compliance list from latest scans
        List<Device> devices = deviceRepository.findAll();
        List<DeviceComplianceStatusDTO> deviceComplianceList = devices.stream()
                .map(device -> {
                    DeviceComplianceStatusDTO dto = new DeviceComplianceStatusDTO();
                    dto.setDeviceId(device.getId());
                    dto.setHostname(device.getHostname());
                    Optional<ComplianceScan> latest = scanRepository.findTop1ByDeviceIdOrderByScannedAtDesc(device.getId());
                    if (latest.isPresent()) {
                        ComplianceScan scan = latest.get();
                        dto.setLatestScore(scan.getComplianceScore());
                        dto.setStatus(scan.getComplianceStatus().name());
                        dto.setLastScanned(scan.getScannedAt());
                    } else {
                        // No scan yet — show as unknown
                        dto.setLatestScore(java.math.BigDecimal.ZERO);
                        dto.setStatus("UNKNOWN");
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        trends.setDeviceComplianceList(deviceComplianceList);
        trends.setDailyScores(new ArrayList<>());
        trends.setDriftTrend(new ArrayList<>());
        return trends;
    }

    private AlertDTO mapAlertToDTO(Alert alert) {
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        dto.setSeverity(alert.getSeverity() != null ? alert.getSeverity().name() : "INFO");
        dto.setStatus(alert.getStatus() != null ? alert.getStatus().name() : "OPEN");
        dto.setDetectedAt(alert.getDetectedAt());
        dto.setAcknowledgedAt(alert.getAcknowledgedAt());
        if (alert.getDevice() != null) {
            dto.setDeviceId(alert.getDevice().getId());
            dto.setDeviceHostname(alert.getDevice().getHostname());
        }
        if (alert.getScan() != null) {
            dto.setScanId(alert.getScan().getId());
        }
        if (alert.getDriftEvent() != null) {
            dto.setDriftEventId(alert.getDriftEvent().getId());
        }
        return dto;
    }
}
