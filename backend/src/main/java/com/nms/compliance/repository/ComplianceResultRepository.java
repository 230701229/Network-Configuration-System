package com.nms.compliance.repository;

import com.nms.compliance.entity.ComplianceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceResultRepository extends JpaRepository<ComplianceResult, Long> {
    List<ComplianceResult> findByScanId(Long scanId);
}
