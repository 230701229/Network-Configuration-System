package com.nms.compliance.repository;

import com.nms.compliance.entity.ComplianceScan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplianceScanRepository extends JpaRepository<ComplianceScan, Long> {
    List<ComplianceScan> findByDeviceIdOrderByScannedAtDesc(Long deviceId);
    Optional<ComplianceScan> findTop1ByDeviceIdOrderByScannedAtDesc(Long deviceId);
    List<ComplianceScan> findByScannedAtBetween(LocalDateTime start, LocalDateTime end);
}
