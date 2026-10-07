package com.nms.compliance.dto;

import com.nms.compliance.entity.Device.DeviceType;
import com.nms.compliance.entity.Device.ManagementProtocol;
import lombok.Data;

@Data
public class CreateDeviceRequest {
    private String hostname;
    private String ipAddress;
    private DeviceType deviceType;
    private String vendor;
    private String model;
    private ManagementProtocol managementProtocol;
    private Integer netconfPort;
    private Integer restconfPort;
    private String username;
    private String password;
    private String description;
}
