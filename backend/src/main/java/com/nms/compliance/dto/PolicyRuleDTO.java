package com.nms.compliance.dto;

import lombok.Data;

@Data
public class PolicyRuleDTO {
    private Long id;
    private String parameter;
    private String operator;
    private String expectedValue;
    private String severity;
    private String description;
    private String remediation;
    private int ruleOrder;
}
