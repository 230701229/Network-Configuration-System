package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DriftEventDTO {
    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private Long scanId;
    private Long policyId;
    private String parameter;
    private String expectedValue;
    private String actualValue;
    private String severity;
    private String status;
    private String description;
    private String remediation;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}
