package com.nms.compliance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeviceComplianceStatusDTO {
    private Long deviceId;
    private String hostname;
    private BigDecimal latestScore;
    private String status;
    private LocalDateTime lastScanned;
}
