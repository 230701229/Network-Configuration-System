package com.nms.compliance.dto;

import com.nms.compliance.entity.Device.DeviceType;
import lombok.Data;
import java.util.List;

@Data
public class CreatePolicyRequest {
    private String policyName;
    private DeviceType deviceType;
    private String vendor;
    private String version;
    private String description;
    private boolean isActive;
    private List<PolicyRuleDTO> rules;
}
