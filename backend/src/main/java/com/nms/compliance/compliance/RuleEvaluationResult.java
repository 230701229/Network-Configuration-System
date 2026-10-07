package com.nms.compliance.compliance;

import com.nms.compliance.entity.PolicyRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationResult {
    private PolicyRule rule;
    private String actualValue;
    private boolean passed;
}
