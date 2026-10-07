package com.nms.compliance.repository;

import com.nms.compliance.entity.AIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, Long> {
    List<AIAnalysis> findByScanId(Long scanId);
    List<AIAnalysis> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
