package com.nms.compliance.network.provider;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.nms.compliance.entity.Device;
import com.nms.compliance.exception.DeviceConnectionException;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class SshDeviceProvider implements NetworkDeviceProvider {

    private static final Logger log = LoggerFactory.getLogger(SshDeviceProvider.class);
    private final EncryptionService encryptionService;

    public SshDeviceProvider(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public NormalizedConfiguration getConfiguration(Device device) {
        int port = device.getNetconfPort() != null && device.getNetconfPort() != 830 ? device.getNetconfPort() : 22;
        String password = resolvePassword(device);
        
        Connection conn = new Connection(device.getIpAddress(), port);
        try {
            conn.connect(null, 5000, 5000);
            boolean auth = conn.authenticateWithPassword(device.getUsername(), password);
            if (!auth) {
                throw new DeviceConnectionException("SSH Authentication failed for device " + device.getHostname());
            }

            Session session = conn.openSession();
            session.execCommand("terminal length 0\nshow running-config");

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStdout()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            session.close();

            Map<String, String> parameters = parseCiscoRunningConfig(output.toString());
            return new NormalizedConfiguration(
                    device.getId(),
                    device.getHostname(),
                    LocalDateTime.now(),
                    device.getManagementProtocol(),
                    parameters
            );
        } catch (Exception e) {
            log.error("Failed to retrieve config via SSH for device {}: {}", device.getHostname(), e.getMessage());
            throw new DeviceConnectionException("SSH connection failed: " + e.getMessage());
        } finally {
            conn.close();
        }
    }

    @Override
    public boolean testConnection(Device device) {
        int port = device.getNetconfPort() != null && device.getNetconfPort() != 830 ? device.getNetconfPort() : 22;
        String password = resolvePassword(device);
        Connection conn = new Connection(device.getIpAddress(), port);
        try {
            conn.connect(null, 4000, 4000);
            boolean auth = conn.authenticateWithPassword(device.getUsername(), password);
            return auth;
        } catch (Exception e) {
            log.warn("SSH test connection failed for {}: {}", device.getHostname(), e.getMessage());
            return false;
        } finally {
            conn.close();
        }
    }

    @Override
    public void disconnect(Device device) {
        // Stateless connections closed per request
    }

    public Map<String, String> parseCiscoRunningConfig(String config) {
        Map<String, String> params = new HashMap<>();
        String lower = config.toLowerCase();

        // SSH: look for ip ssh version or crypto key
        boolean ssh = lower.contains("ip ssh") || lower.contains("transport input ssh") || lower.contains("crypto key generate rsa");
        params.put("ssh", String.valueOf(ssh));
        params.put("ssh-enabled", String.valueOf(ssh));

        // Telnet: check if transport input includes telnet or service telnet enabled
        boolean telnet = lower.contains("transport input telnet") || lower.contains("transport input all") || lower.contains("service telnet");
        params.put("telnet", String.valueOf(telnet));
        params.put("telnet-enabled", String.valueOf(telnet));

        // NTP: ntp server
        boolean ntp = lower.contains("ntp server") || lower.contains("ntp peer");
        params.put("ntp", String.valueOf(ntp));
        params.put("ntp-enabled", String.valueOf(ntp));

        // SNMP: snmp-server
        boolean snmp = lower.contains("snmp-server community") || lower.contains("snmp-server host");
        params.put("snmp", String.valueOf(snmp));
        params.put("snmp-enabled", String.valueOf(snmp));

        // OSPF: router ospf
        boolean ospf = lower.contains("router ospf");
        params.put("ospf", String.valueOf(ospf));
        params.put("ospf-enabled", String.valueOf(ospf));

        // HTTP: ip http server
        boolean http = lower.contains("ip http server") && !lower.contains("no ip http server");
        params.put("http", String.valueOf(http));
        params.put("http-enabled", String.valueOf(http));

        // Logging: logging host or logging on
        boolean logging = lower.contains("logging host") || lower.contains("logging buffered") || lower.contains("logging on");
        params.put("logging", String.valueOf(logging));
        params.put("logging-enabled", String.valueOf(logging));

        // Password encryption: service password-encryption
        boolean passEnc = lower.contains("service password-encryption") && !lower.contains("no service password-encryption");
        params.put("password_encryption", String.valueOf(passEnc));
        params.put("password-encryption", String.valueOf(passEnc));

        // Switch parameters
        boolean stp = lower.contains("spanning-tree");
        params.put("spanning_tree", String.valueOf(stp));
        params.put("spanning-tree-enabled", String.valueOf(stp));

        boolean portSec = lower.contains("port-security");
        params.put("port_security", String.valueOf(portSec));
        params.put("port-security-enabled", String.valueOf(portSec));

        boolean dhcpSnoop = lower.contains("ip dhcp snooping");
        params.put("dhcp_snooping", String.valueOf(dhcpSnoop));
        params.put("dhcp-snooping-enabled", String.valueOf(dhcpSnoop));

        return params;
    }

    private String resolvePassword(Device device) {
        if (device.getEncryptedPassword() == null || device.getEncryptedPassword().isEmpty()) {
            return "";
        }
        try {
            return encryptionService.decrypt(device.getEncryptedPassword());
        } catch (Exception e) {
            return device.getEncryptedPassword();
        }
    }
}
