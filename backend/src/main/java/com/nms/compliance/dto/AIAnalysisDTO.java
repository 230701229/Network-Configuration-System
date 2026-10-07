package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AIAnalysisDTO {
    private Long id;
    private Long scanId;
    private Long deviceId;
    private String summary;
    private String analysis;
    private String recommendations;
    private String priority;
    private String modelUsed;
    private boolean aiAvailable;
    private LocalDateTime createdAt;
}
