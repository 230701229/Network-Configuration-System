package com.nms.compliance.dto;

import com.nms.compliance.entity.Device.DeviceType;
import lombok.Data;

@Data
public class CreateYangModelRequest {
    private String modelName;
    private String namespace;
    private String revision;
    private DeviceType deviceType;
    private String description;
    private String modelContent;
}
