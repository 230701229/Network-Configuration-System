package com.nms.compliance.dto;

import lombok.Data;

@Data
public class ComplianceResultDTO {
    private Long id;
    private Long ruleId;
    private String parameter;
    private String expectedValue;
    private String actualValue;
    private boolean passed;
    private String severity;
    private String description;
    private String remediation;
}
