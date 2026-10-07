package com.nms.compliance.repository;

import com.nms.compliance.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByDeviceType(Device.DeviceType deviceType);
    List<Device> findByStatus(Device.DeviceStatus status);
    List<Device> findByAppliedPolicyId(Long policyId);
}
