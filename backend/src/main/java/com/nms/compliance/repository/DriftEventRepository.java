package com.nms.compliance.repository;

import com.nms.compliance.entity.DriftEvent;
import com.nms.compliance.entity.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriftEventRepository extends JpaRepository<DriftEvent, Long> {
    List<DriftEvent> findByDeviceIdAndStatus(Long deviceId, DriftEvent.DriftStatus status);
    List<DriftEvent> findByStatus(DriftEvent.DriftStatus status);
    List<DriftEvent> findByDeviceIdOrderByDetectedAtDesc(Long deviceId);
    List<DriftEvent> findAllByOrderByDetectedAtDesc();
    long countByStatusAndSeverity(DriftEvent.DriftStatus status, PolicyRule.Severity severity);
    long countByStatus(DriftEvent.DriftStatus status);
}
