package com.nms.compliance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComplianceScanDTO {
    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private Long policyId;
    private String policyName;
    private BigDecimal complianceScore;
    private String complianceStatus;
    private int totalRules;
    private int passedRules;
    private int failedRules;
    private int criticalViolations;
    private int highViolations;
    private int mediumViolations;
    private int lowViolations;
    private String triggeredBy;
    private LocalDateTime scannedAt;
    private List<ComplianceResultDTO> results;
}
