package com.nms.compliance.network.model;

import com.nms.compliance.entity.Device;
import java.time.LocalDateTime;
import java.util.Map;

public record NormalizedConfiguration(
    Long deviceId,
    String hostname,
    LocalDateTime retrievedAt,
    Device.ManagementProtocol protocol,
    Map<String, String> parameters
) {}
