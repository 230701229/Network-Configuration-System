package com.nms.compliance.controller;

import com.nms.compliance.dto.DashboardSummaryDTO;
import com.nms.compliance.dto.DashboardTrendsDTO;
import com.nms.compliance.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    public ResponseEntity<DashboardTrendsDTO> getTrends() {
        return ResponseEntity.ok(dashboardService.getTrends());
    }
}
