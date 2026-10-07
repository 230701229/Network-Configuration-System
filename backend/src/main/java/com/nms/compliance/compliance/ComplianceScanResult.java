package com.nms.compliance.compliance;

import com.nms.compliance.entity.ComplianceScan.ComplianceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceScanResult {
    private BigDecimal score;
    private ComplianceStatus status;
    private List<RuleEvaluationResult> results;
}
