package com.nms.compliance.network.provider;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.nms.compliance.entity.Device;
import com.nms.compliance.exception.DeviceConnectionException;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class NetconfDeviceProvider implements NetworkDeviceProvider {

    private static final Logger log = LoggerFactory.getLogger(NetconfDeviceProvider.class);
    private static final String NETCONF_DELIMITER = "]]>]]>";
    private final EncryptionService encryptionService;

    public NetconfDeviceProvider(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public NormalizedConfiguration getConfiguration(Device device) {
        int port = device.getNetconfPort() != null ? device.getNetconfPort() : 830;
        String password = resolvePassword(device);

        Connection conn = new Connection(device.getIpAddress(), port);
        try {
            conn.connect(null, 6000, 6000);
            boolean auth = conn.authenticateWithPassword(device.getUsername(), password);
            if (!auth) {
                throw new DeviceConnectionException("NETCONF Authentication failed for " + device.getHostname());
            }

            Session session = conn.openSession();
            session.startSubSystem("netconf");

            OutputStream out = session.getStdin();
            InputStream in = session.getStdout();

            // 1. Send client hello
            String clientHello = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
                    + "  <capabilities>\n"
                    + "    <capability>urn:ietf:params:netconf:base:1.0</capability>\n"
                    + "  </capabilities>\n"
                    + "</hello>\n" + NETCONF_DELIMITER;
            out.write(clientHello.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // 2. Read server hello
            String serverHello = readUntilDelimiter(in, 6000);
            log.debug("Received NETCONF server hello from {}: {} bytes", device.getHostname(), serverHello.length());

            // 3. Send get-config RPC
            String getRpc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<rpc message-id=\"101\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
                    + "  <get-config>\n"
                    + "    <source>\n"
                    + "      <running/>\n"
                    + "    </source>\n"
                    + "  </get-config>\n"
                    + "</rpc>\n" + NETCONF_DELIMITER;
            out.write(getRpc.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // 4. Read get-config response
            String rpcResponse = readUntilDelimiter(in, 10000);
            session.close();

            Map<String, String> parameters = parseNetconfXml(rpcResponse);
            return new NormalizedConfiguration(
                    device.getId(),
                    device.getHostname(),
                    LocalDateTime.now(),
                    device.getManagementProtocol(),
                    parameters
            );
        } catch (Exception e) {
            log.error("NETCONF retrieval failed for device {}: {}", device.getHostname(), e.getMessage());
            throw new DeviceConnectionException("NETCONF error: " + e.getMessage());
        } finally {
            conn.close();
        }
    }

    @Override
    public boolean testConnection(Device device) {
        int port = device.getNetconfPort() != null ? device.getNetconfPort() : 830;
        String password = resolvePassword(device);

        Connection conn = new Connection(device.getIpAddress(), port);
        try {
            conn.connect(null, 4000, 4000);
            boolean auth = conn.authenticateWithPassword(device.getUsername(), password);
            if (!auth) return false;

            Session session = conn.openSession();
            session.startSubSystem("netconf");
            session.close();
            return true;
        } catch (Exception e) {
            log.warn("NETCONF test failed for {}: {}", device.getHostname(), e.getMessage());
            return false;
        } finally {
            conn.close();
        }
    }

    @Override
    public void disconnect(Device device) {
        // Closed per request
    }

    public Map<String, String> parseNetconfXml(String xml) {
        Map<String, String> params = new HashMap<>();
        String lower = xml.toLowerCase();

        // SSH
        boolean ssh = lower.contains("<ssh>") || lower.contains("<ip-ssh>") || lower.contains("netconf-yang") 
                || lower.contains("<version>2</version>") || lower.contains("<transport><input><ssh");
        params.put("ssh", String.valueOf(ssh));
        params.put("ssh-enabled", String.valueOf(ssh));

        // Telnet: Check if transport input includes telnet or all, or service telnet enabled
        boolean telnetConfigured = lower.contains("<transport><input><telnet") 
                || lower.contains("<transport><input><all")
                || (lower.contains("transport") && lower.contains("input") && lower.contains("telnet"))
                || lower.contains("<service-telnet>") || lower.contains("<service><telnet")
                || (lower.contains("service telnet") && !lower.contains("no service telnet"));
        boolean sshOnlyTransport = lower.contains("<transport><input><ssh") 
                && !lower.contains("<transport><input><telnet") 
                && !lower.contains("<transport><input><all");
        boolean telnet = telnetConfigured || (!sshOnlyTransport && lower.contains("telnet") && !lower.contains("no telnet"));
        params.put("telnet", String.valueOf(telnet));
        params.put("telnet-enabled", String.valueOf(telnet));

        // NTP
        boolean ntp = lower.contains("<ntp>") || lower.contains("<peer>") || lower.contains("<server>");
        params.put("ntp", String.valueOf(ntp));
        params.put("ntp-enabled", String.valueOf(ntp));

        // SNMP
        boolean snmp = lower.contains("<snmp-server>") || lower.contains("<snmp>") || lower.contains("<community>");
        params.put("snmp", String.valueOf(snmp));
        params.put("snmp-enabled", String.valueOf(snmp));

        // OSPF
        boolean ospf = lower.contains("<router><ospf>") || lower.contains("<ospf>") || lower.contains("cisco-ios-xe-ospf");
        params.put("ospf", String.valueOf(ospf));
        params.put("ospf-enabled", String.valueOf(ospf));

        // HTTP (insecure HTTP server)
        boolean http = lower.contains("<http><server>true</server>") || (lower.contains("<http><server>") && !lower.contains("<secure-server>"));
        params.put("http", String.valueOf(http));
        params.put("http-enabled", String.valueOf(http));

        // Logging
        boolean logging = lower.contains("<logging>") || lower.contains("<syslog>");
        params.put("logging", String.valueOf(logging));
        params.put("logging-enabled", String.valueOf(logging));

        // Password encryption
        boolean passEnc = lower.contains("<password-encryption>") || lower.contains("service password-encryption");
        params.put("password_encryption", String.valueOf(passEnc));
        params.put("password-encryption", String.valueOf(passEnc));

        // Switch features
        boolean stp = lower.contains("<spanning-tree>") || lower.contains("<stp>");
        params.put("spanning_tree", String.valueOf(stp));
        params.put("spanning-tree-enabled", String.valueOf(stp));

        boolean portSec = lower.contains("<port-security>") || lower.contains("port-security");
        params.put("port_security", String.valueOf(portSec));
        params.put("port-security-enabled", String.valueOf(portSec));

        boolean dhcpSnooping = lower.contains("<dhcp-snooping>") || lower.contains("dhcp-snooping");
        params.put("dhcp_snooping", String.valueOf(dhcpSnooping));
        params.put("dhcp-snooping-enabled", String.valueOf(dhcpSnooping));

        return params;
    }

    private String readUntilDelimiter(InputStream in, long timeoutMs) throws Exception {
        StringBuilder buffer = new StringBuilder();
        byte[] chunk = new byte[2048];
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            int available = in.available();
            if (available > 0) {
                int read = in.read(chunk, 0, Math.min(available, chunk.length));
                if (read > 0) {
                    buffer.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                    if (buffer.indexOf(NETCONF_DELIMITER) != -1) {
                        break;
                    }
                }
            } else {
                Thread.sleep(50);
            }
        }
        return buffer.toString();
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
