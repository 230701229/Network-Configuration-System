package com.nms.compliance.dto;

import lombok.Data;
import java.util.List;

@Data
public class PolicyDTO {
    private Long id;
    private String policyName;
    private String deviceType;
    private String vendor;
    private String version;
    private String description;
    private boolean isActive;
    private List<PolicyRuleDTO> rules;
}
