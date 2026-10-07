package com.nms.compliance.controller;

import com.nms.compliance.dto.ConfigurationSnapshotDTO;
import com.nms.compliance.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{deviceId}/snapshots")
    public ResponseEntity<List<ConfigurationSnapshotDTO>> getSnapshots(@PathVariable Long deviceId) {
        return ResponseEntity.ok(historyService.getConfigurationSnapshots(deviceId).stream().map(s -> {
            ConfigurationSnapshotDTO dto = new ConfigurationSnapshotDTO();
            dto.setId(s.getId());
            dto.setDeviceId(s.getDevice().getId());
            dto.setSnapshotData(s.getSnapshotData());
            dto.setConfigHash(s.getConfigHash());
            dto.setRetrievedAt(s.getRetrievedAt());
            return dto;
        }).collect(Collectors.toList()));
    }
}
