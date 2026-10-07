package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertDTO {
    private Long id;
    private Long deviceId;
    private String deviceHostname;
    private Long scanId;
    private Long driftEventId;
    private String title;
    private String message;
    private String severity;
    private String status;
    private LocalDateTime detectedAt;
    private LocalDateTime acknowledgedAt;
}
