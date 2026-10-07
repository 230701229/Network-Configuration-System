package com.nms.compliance.network.provider;

import com.nms.compliance.entity.Device;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.network.mock.DriftSimulationState;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MockDeviceProvider implements NetworkDeviceProvider {

    private final DriftSimulationState driftState;

    public MockDeviceProvider(DriftSimulationState driftState) {
        this.driftState = driftState;
    }

    @Override
    public NormalizedConfiguration getConfiguration(Device device) {
        Map<String, String> params = new HashMap<>();
        String hostname = device.getHostname() != null ? device.getHostname() : "Unknown";

        if (hostname.equals("Router-01")) {
            params.put("ssh", "true"); params.put("telnet", "false");
            params.put("ntp", "true"); params.put("snmp", "true");
            params.put("ospf", "true"); params.put("http", "false");
            params.put("logging", "true"); params.put("password_encryption", "true");
        } else if (hostname.equals("Router-02")) {
            params.put("ssh", "true"); params.put("telnet", "true");
            params.put("ntp", "false"); params.put("snmp", "true");
            params.put("ospf", "true"); params.put("http", "false");
            params.put("logging", "true"); params.put("password_encryption", "true");
        } else if (hostname.equals("Router-03")) {
            params.put("ssh", "false"); params.put("telnet", "true");
            params.put("ntp", "false"); params.put("snmp", "false");
            params.put("ospf", "false"); params.put("http", "true");
            params.put("logging", "false"); params.put("password_encryption", "false");
        } else if (hostname.equals("Switch-01")) {
            params.put("ssh", "true"); params.put("telnet", "false");
            params.put("ntp", "true"); params.put("snmp", "true");
            params.put("spanning_tree", "true"); params.put("port_security", "false");
            params.put("dhcp_snooping", "false");
        } else if (hostname.equals("Switch-02")) {
            params.put("ssh", "true"); params.put("telnet", "false");
            params.put("ntp", "true"); params.put("snmp", "true");
            params.put("spanning_tree", "true"); params.put("port_security", "true");
            params.put("dhcp_snooping", "true");
        } else {
            params.put("ssh", "true");
        }

        Map<String, String> overrides = driftState.getDriftOverrides(device.getId());
        if (overrides != null) {
            params.putAll(overrides);
        }

        synchronizeKeys(params);

        return new NormalizedConfiguration(
            device.getId(),
            hostname,
            LocalDateTime.now(),
            Device.ManagementProtocol.MOCK,
            params
        );
    }

    private void synchronizeKeys(Map<String, String> map) {
        String[] keys = {"ssh", "telnet", "ntp", "snmp", "ospf", "http", "logging"};
        for (String k : keys) {
            String yang = k + "-enabled";
            if (map.containsKey(k) && !map.containsKey(yang)) {
                map.put(yang, map.get(k));
            } else if (map.containsKey(yang) && !map.containsKey(k)) {
                map.put(k, map.get(yang));
            }
        }
        if (map.containsKey("password_encryption") && !map.containsKey("password-encryption")) {
            map.put("password-encryption", map.get("password_encryption"));
        } else if (map.containsKey("password-encryption") && !map.containsKey("password_encryption")) {
            map.put("password_encryption", map.get("password-encryption"));
        }
    }

    @Override
    public boolean testConnection(Device device) {
        return true;
    }

    @Override
    public void disconnect(Device device) {
        // No-op for mock
    }
}
