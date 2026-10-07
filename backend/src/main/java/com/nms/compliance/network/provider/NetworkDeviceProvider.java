package com.nms.compliance.network.provider;

import com.nms.compliance.entity.Device;
import com.nms.compliance.network.model.NormalizedConfiguration;

public interface NetworkDeviceProvider {
    NormalizedConfiguration getConfiguration(Device device);
    boolean testConnection(Device device);
    void disconnect(Device device);
}
