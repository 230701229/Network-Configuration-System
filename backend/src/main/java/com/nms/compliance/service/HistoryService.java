package com.nms.compliance.service;

import com.nms.compliance.entity.ComplianceScan;
import com.nms.compliance.entity.ConfigurationSnapshot;
import com.nms.compliance.repository.ComplianceScanRepository;
import com.nms.compliance.repository.ConfigurationSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryService {

    private final ComplianceScanRepository scanRepository;
    private final ConfigurationSnapshotRepository snapshotRepository;

    public HistoryService(ComplianceScanRepository scanRepository, ConfigurationSnapshotRepository snapshotRepository) {
        this.scanRepository = scanRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public List<ComplianceScan> getDeviceHistory(Long deviceId) {
        return scanRepository.findByDeviceIdOrderByScannedAtDesc(deviceId);
    }

    public List<ConfigurationSnapshot> getConfigurationSnapshots(Long deviceId) {
        return snapshotRepository.findByDeviceIdOrderByRetrievedAtDesc(deviceId);
    }
}
