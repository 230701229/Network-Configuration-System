package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeviceDTO {
    private Long id;
    private String hostname;
    private String ipAddress;
    private String deviceType;
    private String vendor;
    private String model;
    private String managementProtocol;
    private Integer netconfPort;
    private Integer restconfPort;
    private String username;
    private String status;
    private LocalDateTime lastSeen;
    private String description;
}
