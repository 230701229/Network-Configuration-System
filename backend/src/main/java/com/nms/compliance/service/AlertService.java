package com.nms.compliance.service;

import com.nms.compliance.entity.Alert;
import com.nms.compliance.entity.ComplianceScan;
import com.nms.compliance.entity.DriftEvent;
import com.nms.compliance.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public void checkAndCreateAlerts(ComplianceScan scan, List<DriftEvent> newDrifts) {
        for (DriftEvent drift : newDrifts) {
            if (drift.getSeverity().name().equals("CRITICAL") || drift.getSeverity().name().equals("HIGH")) {
                Alert alert = Alert.builder()
                        .device(scan.getDevice())
                        .scan(scan)
                        .driftEvent(drift)
                        .title("Drift Detected on " + scan.getDevice().getHostname())
                        .message("Parameter: " + drift.getParameter() + " drifted. Expected: " + drift.getExpectedValue() + ", Actual: " + drift.getActualValue())
                        .severity(drift.getSeverity())
                        .status(Alert.AlertStatus.OPEN)
                        .detectedAt(LocalDateTime.now())
                        .build();
                alertRepository.save(alert);
            }
        }
    }

    @Transactional
    public Alert createDriftAlert(com.nms.compliance.entity.Device device, DriftEvent drift) {
        Alert alert = Alert.builder()
                .device(device)
                .scan(drift.getScan())
                .driftEvent(drift)
                .title("Configuration Drift: " + device.getHostname())
                .message("Parameter '" + drift.getParameter() + "' drifted. Baseline: " 
                        + drift.getExpectedValue() + ", Running: " + drift.getActualValue())
                .severity(drift.getSeverity() != null ? drift.getSeverity() : com.nms.compliance.entity.PolicyRule.Severity.MEDIUM)
                .status(Alert.AlertStatus.OPEN)
                .detectedAt(LocalDateTime.now())
                .build();
        return alertRepository.save(alert);
    }
}
