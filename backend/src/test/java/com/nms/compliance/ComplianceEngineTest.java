package com.nms.compliance;

import com.nms.compliance.compliance.ComplianceEngine;
import com.nms.compliance.compliance.ComplianceScanResult;
import com.nms.compliance.entity.Policy;
import com.nms.compliance.entity.PolicyRule;
import com.nms.compliance.network.model.NormalizedConfiguration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceEngineTest {

    private final ComplianceEngine engine = new ComplianceEngine();

    @Test
    void testAllRulesPass() {
        Policy policy = new Policy();
        policy.setRules(List.of(
                PolicyRule.builder().parameter("ssh").expectedValue("true").operator(PolicyRule.RuleOperator.EQUALS).build(),
                PolicyRule.builder().parameter("telnet").expectedValue("false").operator(PolicyRule.RuleOperator.EQUALS).build()
        ));

        NormalizedConfiguration config = new NormalizedConfiguration(1L, "R1", LocalDateTime.now(), null,
                Map.of("ssh", "true", "telnet", "false"));

        ComplianceScanResult result = engine.evaluate(config, policy);
        assertEquals(0, result.getScore().compareTo(BigDecimal.valueOf(100.00)));
        assertEquals("COMPLIANT", result.getStatus().name());
    }

    @Test
    void testPartialPass() {
        Policy policy = new Policy();
        policy.setRules(List.of(
                PolicyRule.builder().parameter("ssh").expectedValue("true").operator(PolicyRule.RuleOperator.EQUALS).build(),
                PolicyRule.builder().parameter("telnet").expectedValue("false").operator(PolicyRule.RuleOperator.EQUALS).build(),
                PolicyRule.builder().parameter("ntp").expectedValue("true").operator(PolicyRule.RuleOperator.EQUALS).build()
        ));

        NormalizedConfiguration config = new NormalizedConfiguration(1L, "R1", LocalDateTime.now(), null,
                Map.of("ssh", "true", "telnet", "true", "ntp", "true"));

        ComplianceScanResult result = engine.evaluate(config, policy);
        assertEquals(0, result.getScore().compareTo(BigDecimal.valueOf(66.67)));
        assertEquals("MAJOR_DRIFT", result.getStatus().name());
    }

    @Test
    void testGreaterThanOperator() {
        Policy policy = new Policy();
        policy.setRules(List.of(
                PolicyRule.builder().parameter("timeout").expectedValue("30").operator(PolicyRule.RuleOperator.GREATER_THAN).build()
        ));

        NormalizedConfiguration configPass = new NormalizedConfiguration(1L, "R1", LocalDateTime.now(), null,
                Map.of("timeout", "45"));
        NormalizedConfiguration configFail = new NormalizedConfiguration(1L, "R1", LocalDateTime.now(), null,
                Map.of("timeout", "20"));

        assertTrue(engine.evaluate(configPass, policy).getResults().get(0).isPassed());
        assertFalse(engine.evaluate(configFail, policy).getResults().get(0).isPassed());
    }
}
