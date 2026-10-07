package com.nms.compliance.repository;

import com.nms.compliance.entity.ConfigurationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConfigurationSnapshotRepository extends JpaRepository<ConfigurationSnapshot, Long> {
    List<ConfigurationSnapshot> findByDeviceIdOrderByRetrievedAtDesc(Long deviceId);
    List<ConfigurationSnapshot> findByDeviceIdAndConfigHash(Long deviceId, String configHash);
}
