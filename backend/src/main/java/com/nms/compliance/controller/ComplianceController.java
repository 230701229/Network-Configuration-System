package com.nms.compliance.controller;

import com.nms.compliance.dto.ComplianceScanDTO;
import com.nms.compliance.service.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PostMapping("/scan-all")
    public ResponseEntity<List<ComplianceScanDTO>> scanAllDevices() {
        return ResponseEntity.ok(complianceService.scanAllDevices());
    }
}
