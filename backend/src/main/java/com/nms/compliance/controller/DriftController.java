package com.nms.compliance.controller;

import com.nms.compliance.dto.ConfigurationDiffDTO;
import com.nms.compliance.dto.DriftEventDTO;
import com.nms.compliance.entity.DriftEvent;
import com.nms.compliance.service.DeviceService;
import com.nms.compliance.service.DriftDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drift")
public class DriftController {

    private final DriftDetectionService driftDetectionService;
    private final DeviceService deviceService;

    public DriftController(DriftDetectionService driftDetectionService, DeviceService deviceService) {
        this.driftDetectionService = driftDetectionService;
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<List<DriftEventDTO>> getAllDriftEvents(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String status) {
        
        List<DriftEvent> events;
        if (deviceId != null) {
            events = driftDetectionService.getDriftEventsForDevice(deviceId);
        } else {
            events = driftDetectionService.getAllDriftEvents();
        }

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            events = events.stream()
                    .filter(e -> e.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(events.stream().map(this::mapToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/open")
    public ResponseEntity<List<DriftEventDTO>> getOpenDriftEvents() {
        return ResponseEntity.ok(driftDetectionService.getAllOpenDriftEvents().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<DriftEventDTO>> getDeviceDrift(@PathVariable Long deviceId) {
        return ResponseEntity.ok(driftDetectionService.getDriftEventsForDevice(deviceId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DriftEventDTO> updateDriftStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        DriftEvent.DriftStatus status = DriftEvent.DriftStatus.valueOf(statusStr.toUpperCase());
        DriftEvent updated = driftDetectionService.updateDriftStatus(id, status);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @PostMapping("/detect-all")
    public ResponseEntity<List<DriftEventDTO>> detectAllDrift() {
        List<DriftEvent> newDrifts = driftDetectionService.detectAllDevicesDrift();
        return ResponseEntity.ok(newDrifts.stream().map(this::mapToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/device/{deviceId}/detect")
    public ResponseEntity<List<DriftEventDTO>> detectDeviceDrift(@PathVariable Long deviceId) {
        List<DriftEvent> newDrifts = driftDetectionService.detectDeviceConfigurationDrift(deviceId);
        return ResponseEntity.ok(newDrifts.stream().map(this::mapToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/device/{deviceId}/diff")
    public ResponseEntity<ConfigurationDiffDTO> getConfigurationDiff(@PathVariable Long deviceId) {
        return ResponseEntity.ok(driftDetectionService.getConfigurationDiff(deviceId));
    }

    @PostMapping("/device/{deviceId}/set-baseline")
    public ResponseEntity<Map<String, String>> setBaseline(@PathVariable Long deviceId) {
        driftDetectionService.setBaseline(deviceId);
        return ResponseEntity.ok(Map.of("message", "Current configuration marked as golden baseline."));
    }

    @PostMapping("/device/{deviceId}/simulate")
    public ResponseEntity<Map<String, String>> simulateDrift(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "telnet") String param,
            @RequestParam(defaultValue = "true") String value) {
        deviceService.simulateDrift(deviceId, param, value);
        // Immediately run drift detection on the simulated change
        List<DriftEvent> drifts = driftDetectionService.detectDeviceConfigurationDrift(deviceId);
        return ResponseEntity.ok(Map.of(
                "message", "Simulated drift applied on device " + deviceId,
                "driftsDetected", String.valueOf(drifts.size())
        ));
    }

    @PostMapping("/device/{deviceId}/reset-drift")
    public ResponseEntity<Map<String, String>> resetDrift(@PathVariable Long deviceId) {
        deviceService.resetDrift(deviceId);
        driftDetectionService.detectDeviceConfigurationDrift(deviceId);
        return ResponseEntity.ok(Map.of("message", "Drift reset and synchronized"));
    }

    private DriftEventDTO mapToDTO(DriftEvent d) {
        DriftEventDTO dto = new DriftEventDTO();
        dto.setId(d.getId());
        if (d.getDevice() != null) {
            dto.setDeviceId(d.getDevice().getId());
            dto.setDeviceHostname(d.getDevice().getHostname());
        }
        if (d.getScan() != null) {
            dto.setScanId(d.getScan().getId());
        }
        if (d.getPolicy() != null) {
            dto.setPolicyId(d.getPolicy().getId());
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
    }
}
