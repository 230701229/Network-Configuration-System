package com.nms.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nms.compliance.compliance.ComplianceEngine;
import com.nms.compliance.entity.Device;
import com.nms.compliance.entity.Policy;
import com.nms.compliance.entity.PolicyRule;
import com.nms.compliance.network.mock.DriftSimulationState;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.network.provider.MockDeviceProvider;
import com.nms.compliance.service.DriftDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DriftDetectionTest {

    private DriftSimulationState driftState;
    private MockDeviceProvider mockProvider;
    private ComplianceEngine complianceEngine;
    private DriftDetectionService driftService;
    private Policy routerPolicy;
    private Device router01;

    @BeforeEach
    void setup() {
        driftState = new DriftSimulationState();
        mockProvider = new MockDeviceProvider(driftState);
        complianceEngine = new ComplianceEngine();
        driftService = new DriftDetectionService(null, null, null, null, null, null, null, new ObjectMapper());

        router01 = Device.builder()
                .id(1L)
                .hostname("Router-01")
                .deviceType(Device.DeviceType.ROUTER)
                .managementProtocol(Device.ManagementProtocol.MOCK)
                .build();

        routerPolicy = Policy.builder()
                .id(1L)
                .policyName("Router Security Baseline")
                .deviceType(Device.DeviceType.ROUTER)
                .rules(new ArrayList<>())
                .build();

        routerPolicy.getRules().add(PolicyRule.builder()
                .parameter("ssh-enabled")
                .operator(PolicyRule.RuleOperator.EQUALS)
                .expectedValue("true")
                .severity(PolicyRule.Severity.HIGH)
                .build());

        routerPolicy.getRules().add(PolicyRule.builder()
                .parameter("telnet-enabled")
                .operator(PolicyRule.RuleOperator.EQUALS)
                .expectedValue("false")
                .severity(PolicyRule.Severity.CRITICAL)
                .build());

        routerPolicy.getRules().add(PolicyRule.builder()
                .parameter("ntp-enabled")
                .operator(PolicyRule.RuleOperator.EQUALS)
                .expectedValue("true")
                .severity(PolicyRule.Severity.MEDIUM)
                .build());

        router01.setAppliedPolicy(routerPolicy);
    }

    @Test
    void testInitialCompliantState() {
        NormalizedConfiguration config = mockProvider.getConfiguration(router01);
        assertEquals("true", config.parameters().get("ssh"));
        assertEquals("true", config.parameters().get("ssh-enabled"));
        assertEquals("false", config.parameters().get("telnet"));
        assertEquals("false", config.parameters().get("telnet-enabled"));

        var result = complianceEngine.evaluate(config, routerPolicy);
        assertEquals(100.0, result.getScore().doubleValue());
        assertEquals(com.nms.compliance.entity.ComplianceScan.ComplianceStatus.COMPLIANT, result.getStatus());
    }

    @Test
    void testDriftDetectionAndScoreDrop() {
        // Initial state is 100% compliant
        NormalizedConfiguration initialConfig = mockProvider.getConfiguration(router01);
        var initialResult = complianceEngine.evaluate(initialConfig, routerPolicy);
        assertEquals(100.0, initialResult.getScore().doubleValue());

        // Apply drift: telnet-enabled changed from false to true
        driftState.applyDrift(1L, "telnet", "true");
        driftState.applyDrift(1L, "telnet-enabled", "true");

        NormalizedConfiguration driftedConfig = mockProvider.getConfiguration(router01);
        assertEquals("true", driftedConfig.parameters().get("telnet-enabled"));

        // Evaluate compliance: score drops deterministically
        var driftedResult = complianceEngine.evaluate(driftedConfig, routerPolicy);
        assertTrue(driftedResult.getScore().doubleValue() < 100.0);

        // Test parameter diff detection
        Set<String> changes = driftService.findChangedParameters(initialConfig.parameters(), driftedConfig.parameters());
        assertTrue(changes.contains("telnet-enabled"));
        assertFalse(changes.contains("telnet"), "Short and enabled keys should be deduplicated");
    }

    @Test
    void testNoChangesProduceEmptyDiff() {
        NormalizedConfiguration config1 = mockProvider.getConfiguration(router01);
        NormalizedConfiguration config2 = mockProvider.getConfiguration(router01);

        Set<String> changes = driftService.findChangedParameters(config1.parameters(), config2.parameters());
        assertTrue(changes.isEmpty(), "Unchanged configurations must yield no drift events");
    }
}
