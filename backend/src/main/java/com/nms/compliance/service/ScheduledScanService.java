package com.nms.compliance.service;

import com.nms.compliance.entity.DriftEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledScanService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledScanService.class);

    private final ComplianceService complianceService;
    private final DriftDetectionService driftDetectionService;

    public ScheduledScanService(ComplianceService complianceService, DriftDetectionService driftDetectionService) {
        this.complianceService = complianceService;
        this.driftDetectionService = driftDetectionService;
    }

    /**
     * Periodic compliance scan across all devices (default 30 mins)
     */
    @Scheduled(fixedDelayString = "${compliance.scan.interval:1800000}")
    public void runScheduledScans() {
        log.info("Starting scheduled compliance scan for all devices...");
        complianceService.scanAllDevices();
        log.info("Scheduled compliance scan completed.");
    }

    /**
     * Automated continuous configuration drift monitoring (configurable, default every 5 seconds)
     */
    @Scheduled(fixedDelayString = "${drift.scan.interval:5000}")
    public void runScheduledDriftMonitoring() {
        log.trace("Running continuous configuration drift monitoring (5s loop)...");
        try {
            List<DriftEvent> newDrifts = driftDetectionService.detectAllDevicesDrift();
            if (!newDrifts.isEmpty()) {
                log.info("Automated drift monitor detected {} new configuration drift event(s) across network!", newDrifts.size());
            }
        } catch (Exception e) {
            log.error("Error during scheduled drift monitoring: {}", e.getMessage());
        }
    }
}
