package com.nms.compliance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestConnectionResponse {
    private boolean success;
    private String message;
    private String protocol;
    private long responseTime;
}
