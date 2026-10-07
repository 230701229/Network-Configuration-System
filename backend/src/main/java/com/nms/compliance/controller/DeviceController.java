package com.nms.compliance.controller;

import com.nms.compliance.dto.*;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.service.ComplianceService;
import com.nms.compliance.service.DeviceService;
import com.nms.compliance.service.DriftDetectionService;
import com.nms.compliance.service.HistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final ComplianceService complianceService;
    private final DriftDetectionService driftDetectionService;
    private final HistoryService historyService;

    public DeviceController(DeviceService deviceService, ComplianceService complianceService,
                            DriftDetectionService driftDetectionService, HistoryService historyService) {
        this.deviceService = deviceService;
        this.complianceService = complianceService;
        this.driftDetectionService = driftDetectionService;
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@Valid @RequestBody CreateDeviceRequest request) {
        return ResponseEntity.ok(deviceService.createDevice(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @Valid @RequestBody UpdateDeviceRequest request) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<TestConnectionResponse> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.testConnection(id));
    }

    @GetMapping("/{id}/configuration")
    public ResponseEntity<NormalizedConfiguration> getConfiguration(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.retrieveConfiguration(id));
    }

    @PostMapping("/{id}/configuration/retrieve")
    public ResponseEntity<NormalizedConfiguration> retrieveConfiguration(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.retrieveConfiguration(id));
    }

    @PostMapping("/{id}/scan")
    public ResponseEntity<ComplianceScanDTO> scanDevice(@PathVariable Long id) {
        return ResponseEntity.ok(complianceService.scanDevice(id));
    }

    @GetMapping("/{id}/compliance")
    public ResponseEntity<List<ComplianceScanDTO>> getDeviceCompliance(@PathVariable Long id) {
        // Returning history as compliance scans for simplicity
        return ResponseEntity.ok(historyService.getDeviceHistory(id).stream().map(s -> {
            ComplianceScanDTO dto = new ComplianceScanDTO();
            dto.setId(s.getId());
            dto.setComplianceScore(s.getComplianceScore());
            dto.setComplianceStatus(s.getComplianceStatus().name());
            dto.setScannedAt(s.getScannedAt());
            return dto;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/drift")
    public ResponseEntity<List<DriftEventDTO>> getDeviceDrift(@PathVariable Long id) {
        return ResponseEntity.ok(driftDetectionService.getDriftEventsForDevice(id).stream().map(d -> {
            DriftEventDTO dto = new DriftEventDTO();
            dto.setId(d.getId());
            if (d.getDevice() != null) {
                dto.setDeviceId(d.getDevice().getId());
                dto.setDeviceHostname(d.getDevice().getHostname());
            }
            dto.setParameter(d.getParameter());
            dto.setExpectedValue(d.getExpectedValue());
            dto.setActualValue(d.getActualValue());
            dto.setSeverity(d.getSeverity() != null ? d.getSeverity().name() : "MEDIUM");
            dto.setStatus(d.getStatus() != null ? d.getStatus().name() : "OPEN");
            dto.setDescription(d.getDescription());
            dto.setRemediation(d.getRemediation());
            dto.setDetectedAt(d.getDetectedAt());
            dto.setResolvedAt(d.getResolvedAt());
            return dto;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplianceScanDTO>> getHistory(@PathVariable Long id) {
        return getDeviceCompliance(id);
    }

    @PostMapping("/{id}/simulate-drift")
    public ResponseEntity<String> simulateDrift(@PathVariable Long id) {
        deviceService.simulateDrift(id);
        return ResponseEntity.ok("{\"message\": \"Drift simulated\"}");
    }

    @PostMapping("/{id}/reset-drift")
    public ResponseEntity<String> resetDrift(@PathVariable Long id) {
        deviceService.resetDrift(id);
        return ResponseEntity.ok("{\"message\": \"Drift reset\"}");
    }
}
