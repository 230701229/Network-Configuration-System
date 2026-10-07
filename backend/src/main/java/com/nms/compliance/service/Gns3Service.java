package com.nms.compliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nms.compliance.dto.CreateDeviceRequest;
import com.nms.compliance.dto.DeviceDTO;
import com.nms.compliance.entity.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class Gns3Service {

    private static final Logger log = LoggerFactory.getLogger(Gns3Service.class);

    @Value("${gns3.server.url:http://localhost:3080}")
    private String defaultGns3Url;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DeviceService deviceService;

    public Gns3Service(ObjectMapper objectMapper, DeviceService deviceService) {
        this.objectMapper = objectMapper;
        this.deviceService = deviceService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Map<String, Object> testGns3Connection(String serverUrl) {
        String url = (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : defaultGns3Url;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/v2/version"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - start;

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                result.put("connected", true);
                result.put("version", json.path("version").asText("2.x"));
                result.put("url", url);
                result.put("responseTimeMs", responseTime);
                result.put("message", "Connected successfully to GNS3 Server v" + json.path("version").asText());
            } else {
                result.put("connected", false);
                result.put("url", url);
                result.put("statusCode", response.statusCode());
                result.put("message", "GNS3 server returned HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            result.put("connected", false);
            result.put("url", url);
            result.put("responseTimeMs", responseTime);
            result.put("message", "Could not reach GNS3 server at " + url + " (" + e.getMessage() + ")");
        }

        return result;
    }

    public List<Map<String, Object>> getGns3Projects(String serverUrl) {
        String url = (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : defaultGns3Url;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        List<Map<String, Object>> projects = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/v2/projects"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode array = objectMapper.readTree(response.body());
                if (array.isArray()) {
                    for (JsonNode p : array) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("projectId", p.path("project_id").asText());
                        item.put("name", p.path("name").asText());
                        item.put("status", p.path("status").asText());
                        projects.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve GNS3 projects: {}", e.getMessage());
        }
        return projects;
    }

    public List<Map<String, Object>> getGns3Nodes(String serverUrl, String projectId) {
        String url = (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : defaultGns3Url;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        List<Map<String, Object>> nodes = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/v2/projects/" + projectId + "/nodes"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode array = objectMapper.readTree(response.body());
                if (array.isArray()) {
                    for (JsonNode n : array) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("nodeId", n.path("node_id").asText());
                        item.put("name", n.path("name").asText());
                        item.put("nodeType", n.path("node_type").asText());
                        item.put("status", n.path("status").asText());
                        item.put("consolePort", n.path("console").asInt());
                        nodes.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve GNS3 nodes: {}", e.getMessage());
        }
        return nodes;
    }

    public DeviceDTO importGns3Node(Map<String, String> request) {
        String name = request.getOrDefault("name", "GNS3-Router");
        String ip = request.getOrDefault("ipAddress", "192.168.122.10");
        String protocolStr = request.getOrDefault("managementProtocol", "NETCONF");
        Device.ManagementProtocol protocol = Device.ManagementProtocol.valueOf(protocolStr.toUpperCase());
        String deviceTypeStr = request.getOrDefault("deviceType", "ROUTER");
        Device.DeviceType deviceType = Device.DeviceType.valueOf(deviceTypeStr.toUpperCase());
        int port = Integer.parseInt(request.getOrDefault("port", protocol == Device.ManagementProtocol.NETCONF ? "830" : (protocol == Device.ManagementProtocol.RESTCONF ? "443" : "22")));

        CreateDeviceRequest createReq = new CreateDeviceRequest();
        createReq.setHostname(name);
        createReq.setIpAddress(ip);
        createReq.setDeviceType(deviceType);
        createReq.setVendor("Cisco");
        createReq.setModel("CSR1000v / IOS-XE");
        createReq.setManagementProtocol(protocol);
        createReq.setNetconfPort(port);
        createReq.setRestconfPort(port);
        createReq.setUsername(request.getOrDefault("username", "admin"));
        createReq.setPassword(request.getOrDefault("password", "Admin123!"));
        createReq.setDescription("Imported from GNS3 virtual topology");

        return deviceService.createDevice(createReq);
    }

    public Map<String, String> getCiscoConfigurations(String hostname, String ip) {
        String h = hostname != null ? hostname : "Router-GNS3-01";
        
        String netconfConfig = """
                ! === Cisco IOS-XE NETCONF & SSH Configuration ===
                hostname %s
                ip domain-name nms.local
                crypto key generate rsa modulus 2048
                ip ssh version 2
                ip ssh time-out 60
                
                ! Admin user
                username admin privilege 15 secret Admin123!
                
                ! Enable NETCONF subsystem (port 830)
                netconf-yang
                netconf-yang cisco-odm polling-enable
                
                ! VTY Lines
                line vty 0 4
                 transport input ssh
                 login local
                exit
                """.formatted(h);

        String restconfConfig = """
                ! === Cisco IOS-XE RESTCONF & HTTPS Configuration ===
                hostname %s
                ip http secure-server
                ip http authentication local
                restconf
                
                ! Verify: curl -k -u admin:Admin123! https://<router-ip>/restconf/data
                """.formatted(h);

        String compliantBaseline = """
                ! === Enterprise Compliance Baseline Configuration ===
                service password-encryption
                no service telnet
                ip ssh version 2
                ntp server 10.0.0.1
                snmp-server community PublicReadOnly RO
                router ospf 1
                 network 192.168.0.0 0.0.255.255 area 0
                no ip http server
                logging buffered 64000
                """.formatted();

        String driftInjectionScript = """
                ! === Configuration Drift Injection (Test Drift Detection) ===
                ! Run these on GNS3 router console to cause intentional drift:
                conf t
                service telnet
                no service password-encryption
                no ntp server 10.0.0.1
                end
                ! Now click 'Scan for Drift' in NMS to see instant detection!
                """.formatted();

        return Map.of(
                "netconfConfig", netconfConfig,
                "restconfConfig", restconfConfig,
                "compliantBaseline", compliantBaseline,
                "driftInjectionScript", driftInjectionScript
        );
    }
}
