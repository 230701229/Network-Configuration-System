package com.nms.compliance.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardTrendsDTO {
    private List<Double> dailyScores;
    private List<Integer> driftTrend;
    private List<DeviceComplianceStatusDTO> deviceComplianceList;
}
