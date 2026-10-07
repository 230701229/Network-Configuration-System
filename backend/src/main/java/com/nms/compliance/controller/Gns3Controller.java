package com.nms.compliance.controller;

import com.nms.compliance.dto.DeviceDTO;
import com.nms.compliance.service.Gns3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gns3")
public class Gns3Controller {

    private final Gns3Service gns3Service;

    public Gns3Controller(Gns3Service gns3Service) {
        this.gns3Service = gns3Service;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody(required = false) Map<String, String> body) {
        String serverUrl = body != null ? body.get("serverUrl") : null;
        return ResponseEntity.ok(gns3Service.testGns3Connection(serverUrl));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjects(@RequestParam(required = false) String serverUrl) {
        return ResponseEntity.ok(gns3Service.getGns3Projects(serverUrl));
    }

    @GetMapping("/projects/{projectId}/nodes")
    public ResponseEntity<List<Map<String, Object>>> getNodes(
            @PathVariable String projectId,
            @RequestParam(required = false) String serverUrl) {
        return ResponseEntity.ok(gns3Service.getGns3Nodes(serverUrl, projectId));
    }

    @PostMapping("/import-node")
    public ResponseEntity<DeviceDTO> importNode(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(gns3Service.importGns3Node(request));
    }

    @GetMapping("/config-templates")
    public ResponseEntity<Map<String, String>> getConfigTemplates(
            @RequestParam(required = false, defaultValue = "Router-GNS3-01") String hostname,
            @RequestParam(required = false, defaultValue = "192.168.122.10") String ip) {
        return ResponseEntity.ok(gns3Service.getCiscoConfigurations(hostname, ip));
    }
}
