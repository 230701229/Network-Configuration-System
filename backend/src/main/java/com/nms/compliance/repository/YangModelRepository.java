package com.nms.compliance.repository;

import com.nms.compliance.entity.YangModel;
import com.nms.compliance.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface YangModelRepository extends JpaRepository<YangModel, Long> {
    List<YangModel> findByDeviceType(Device.DeviceType deviceType);
}
