package com.nms.compliance.controller;

import com.nms.compliance.dto.AlertDTO;
import com.nms.compliance.entity.Alert;
import com.nms.compliance.repository.AlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public ResponseEntity<List<AlertDTO>> getOpenAlerts() {
        return ResponseEntity.ok(alertRepository.findByStatus(Alert.AlertStatus.OPEN).stream().map(a -> {
            AlertDTO dto = new AlertDTO();
            dto.setId(a.getId());
            dto.setDeviceId(a.getDevice().getId());
            dto.setDeviceHostname(a.getDevice().getHostname());
            dto.setMessage(a.getMessage());
            dto.setSeverity(a.getSeverity().name());
            dto.setStatus(a.getStatus().name());
            return dto;
        }).collect(Collectors.toList()));
    }
}
