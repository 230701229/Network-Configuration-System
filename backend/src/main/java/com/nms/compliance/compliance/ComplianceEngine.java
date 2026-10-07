package com.nms.compliance.compliance;

import com.nms.compliance.entity.Policy;
import com.nms.compliance.entity.PolicyRule;
import com.nms.compliance.entity.ComplianceScan.ComplianceStatus;
import com.nms.compliance.network.model.NormalizedConfiguration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ComplianceEngine {

    public ComplianceScanResult evaluate(NormalizedConfiguration config, Policy policy) {
        List<RuleEvaluationResult> results = new ArrayList<>();
        
        for (PolicyRule rule : policy.getRules()) {
            String actualValue = resolveParameterValue(config.parameters(), rule.getParameter());
            boolean passed = evaluateRule(rule, actualValue);
            results.add(new RuleEvaluationResult(rule, actualValue, passed));
        }
        
        long passed = results.stream().filter(RuleEvaluationResult::isPassed).count();
        long total = results.size();
        BigDecimal score = total > 0 
            ? BigDecimal.valueOf(passed * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        ComplianceStatus status = determineStatus(score);
        return new ComplianceScanResult(score, status, results);
    }

    private boolean evaluateRule(PolicyRule rule, String actualValue) {
        if (actualValue == null) return false;
        String expected = rule.getExpectedValue().trim().toLowerCase();
        String actual = actualValue.trim().toLowerCase();
        return switch (rule.getOperator()) {
            case EQUALS -> actual.equals(expected);
            case NOT_EQUALS -> !actual.equals(expected);
            case CONTAINS -> actual.contains(expected);
            case NOT_CONTAINS -> !actual.contains(expected);
            case GREATER_THAN -> parseDouble(actual) > parseDouble(expected);
            case LESS_THAN -> parseDouble(actual) < parseDouble(expected);
        };
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String resolveParameterValue(java.util.Map<String, String> params, String paramName) {
        if (params == null || paramName == null) return null;
        if (params.containsKey(paramName)) {
            return params.get(paramName);
        }
        String lower = paramName.toLowerCase().replace('_', '-');
        if (params.containsKey(lower)) return params.get(lower);

        if (lower.endsWith("-enabled")) {
            String stripped = lower.substring(0, lower.length() - "-enabled".length());
            if (params.containsKey(stripped)) return params.get(stripped);
            if (params.containsKey(stripped.replace('-', '_'))) return params.get(stripped.replace('-', '_'));
        } else {
            String withEnabled = lower + "-enabled";
            if (params.containsKey(withEnabled)) return params.get(withEnabled);
        }

        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey().toLowerCase().replace('_', '-');
            if (k.equals(lower) || k.equals(lower + "-enabled") || (lower.endsWith("-enabled") && k.equals(lower.substring(0, lower.length() - 8)))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ComplianceStatus determineStatus(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 100) return ComplianceStatus.COMPLIANT;
        if (s >= 80)  return ComplianceStatus.MINOR_DRIFT;
        if (s >= 50)  return ComplianceStatus.MAJOR_DRIFT;
        return ComplianceStatus.CRITICAL_NON_COMPLIANT;
    }
}
