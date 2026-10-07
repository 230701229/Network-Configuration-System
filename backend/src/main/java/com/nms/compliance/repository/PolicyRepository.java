package com.nms.compliance.repository;

import com.nms.compliance.entity.Policy;
import com.nms.compliance.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByDeviceTypeAndIsActive(Device.DeviceType deviceType, boolean isActive);
}
