package com.nms.compliance.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardSummaryDTO {
    private long totalDevices;
    private long compliantDevices;
    private long devicesWithDrift;
    private long criticalViolations;
    private double averageComplianceScore;
    private List<AlertDTO> recentAlerts;
}
