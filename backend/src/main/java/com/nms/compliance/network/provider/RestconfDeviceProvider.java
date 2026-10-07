package com.nms.compliance.network.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nms.compliance.entity.Device;
import com.nms.compliance.exception.DeviceConnectionException;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestconfDeviceProvider implements NetworkDeviceProvider {

    private static final Logger log = LoggerFactory.getLogger(RestconfDeviceProvider.class);
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RestconfDeviceProvider(EncryptionService encryptionService, ObjectMapper objectMapper) {
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.httpClient = createTrustingHttpClient();
    }

    @Override
    public NormalizedConfiguration getConfiguration(Device device) {
        int port = device.getRestconfPort() != null ? device.getRestconfPort() : 443;
        String scheme = port == 80 || port == 8080 ? "http" : "https";
        String password = resolvePassword(device);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (device.getUsername() + ":" + password).getBytes(StandardCharsets.UTF_8)
        );

        // Try Cisco IOS-XE native first, then fallback to general restconf root
        String[] endpoints = new String[]{
                scheme + "://" + device.getIpAddress() + ":" + port + "/restconf/data/Cisco-IOS-XE-native:native",
                scheme + "://" + device.getIpAddress() + ":" + port + "/restconf/data"
        };

        String responseBody = null;
        for (String endpoint : endpoints) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", authHeader)
                        .header("Accept", "application/yang-data+json, application/json")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    responseBody = response.body();
                    break;
                }
            } catch (Exception e) {
                log.debug("RESTCONF probe failed for {}: {}", endpoint, e.getMessage());
            }
        }

        if (responseBody == null) {
            throw new DeviceConnectionException("Failed to retrieve RESTCONF data from " + device.getHostname());
        }

        Map<String, String> parameters = parseRestconfJson(responseBody);
        return new NormalizedConfiguration(
                device.getId(),
                device.getHostname(),
                LocalDateTime.now(),
                device.getManagementProtocol(),
                parameters
        );
    }

    @Override
    public boolean testConnection(Device device) {
        int port = device.getRestconfPort() != null ? device.getRestconfPort() : 443;
        String scheme = port == 80 || port == 8080 ? "http" : "https";
        String password = resolvePassword(device);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (device.getUsername() + ":" + password).getBytes(StandardCharsets.UTF_8)
        );

        String testUri = scheme + "://" + device.getIpAddress() + ":" + port + "/restconf/data";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUri))
                    .header("Authorization", authHeader)
                    .header("Accept", "application/yang-data+json, application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 404; // 404 means server replied with RESTCONF active
        } catch (Exception e) {
            log.warn("RESTCONF test failed for {}: {}", device.getHostname(), e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect(Device device) {
        // HTTP connections are managed per request
    }

    public Map<String, String> parseRestconfJson(String json) {
        Map<String, String> params = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            String text = root.toString().toLowerCase();

            // SSH
            boolean ssh = text.contains("\"ssh\"") || text.contains("ip-ssh") || text.contains("\"version\":2") || text.contains("\"input\":[\"ssh\"]");
            params.put("ssh", String.valueOf(ssh));
            params.put("ssh-enabled", String.valueOf(ssh));

            // Telnet
            boolean hasTelnetKeyword = text.contains("\"telnet\"") || text.contains("\"all\"") || text.contains("service-telnet");
            boolean sshOnly = (text.contains("\"input\":[\"ssh\"]") || text.contains("\"input\":\"ssh\"")) 
                    && !text.contains("\"telnet\"") && !text.contains("\"all\"");
            boolean telnet = hasTelnetKeyword && !sshOnly && !text.contains("\"no-telnet\"");
            params.put("telnet", String.valueOf(telnet));
            params.put("telnet-enabled", String.valueOf(telnet));

            // NTP
            boolean ntp = text.contains("\"ntp\"") || text.contains("\"peer\"") || text.contains("\"server\"");
            params.put("ntp", String.valueOf(ntp));
            params.put("ntp-enabled", String.valueOf(ntp));

            // SNMP
            boolean snmp = text.contains("\"snmp\"") || text.contains("\"community\"");
            params.put("snmp", String.valueOf(snmp));
            params.put("snmp-enabled", String.valueOf(snmp));

            // OSPF
            boolean ospf = text.contains("\"ospf\"") || text.contains("\"router-ospf\"");
            params.put("ospf", String.valueOf(ospf));
            params.put("ospf-enabled", String.valueOf(ospf));

            // HTTP
            boolean http = text.contains("\"http\"") && !text.contains("\"secure-server\"");
            params.put("http", String.valueOf(http));
            params.put("http-enabled", String.valueOf(http));

            // Logging
            boolean logging = text.contains("\"logging\"") || text.contains("\"syslog\"");
            params.put("logging", String.valueOf(logging));
            params.put("logging-enabled", String.valueOf(logging));

            // Password encryption
            boolean passEnc = text.contains("password-encryption") || text.contains("service-password-encryption");
            params.put("password_encryption", String.valueOf(passEnc));
            params.put("password-encryption", String.valueOf(passEnc));

            // Switch settings
            boolean stp = text.contains("spanning-tree") || text.contains("stp");
            params.put("spanning_tree", String.valueOf(stp));
            params.put("spanning-tree-enabled", String.valueOf(stp));

            boolean portSec = text.contains("port-security");
            params.put("port_security", String.valueOf(portSec));
            params.put("port-security-enabled", String.valueOf(portSec));

            boolean dhcpSnoop = text.contains("dhcp-snooping");
            params.put("dhcp_snooping", String.valueOf(dhcpSnoop));
            params.put("dhcp-snooping-enabled", String.valueOf(dhcpSnoop));

        } catch (Exception e) {
            log.warn("Failed to parse RESTCONF JSON: {}", e.getMessage());
        }
        return params;
    }

    private HttpClient createTrustingHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sc)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } catch (Exception e) {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
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
