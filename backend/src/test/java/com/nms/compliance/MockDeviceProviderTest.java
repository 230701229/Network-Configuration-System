package com.nms.compliance;

import com.nms.compliance.entity.Device;
import com.nms.compliance.network.mock.DriftSimulationState;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.network.provider.MockDeviceProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockDeviceProviderTest {

    private final MockDeviceProvider provider = new MockDeviceProvider(new DriftSimulationState());

    @Test
    void testRouter01() {
        Device device = Device.builder().id(1L).hostname("Router-01").build();
        NormalizedConfiguration config = provider.getConfiguration(device);
        assertEquals("true", config.parameters().get("ssh"));
        assertEquals("false", config.parameters().get("telnet"));
    }

    @Test
    void testRouter03() {
        Device device = Device.builder().id(3L).hostname("Router-03").build();
        NormalizedConfiguration config = provider.getConfiguration(device);
        assertEquals("false", config.parameters().get("ssh"));
        assertEquals("true", config.parameters().get("telnet"));
    }
}
