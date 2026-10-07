package com.nms.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationDiffDTO {
    private Long deviceId;
    private String hostname;
    private boolean hasDrift;
    private int totalChanges;
    private LocalDateTime baselineTime;
    private LocalDateTime currentTime;
    private String baselineHash;
    private String currentHash;
    private List<ParameterDiff> changes;
    private Map<String, String> baselineConfig;
    private Map<String, String> currentConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDiff {
        private String parameter;
        private String baselineValue;
        private String currentValue;
        private String changeType; // MODIFIED, ADDED, REMOVED
        private String severity;   // CRITICAL, HIGH, MEDIUM, LOW
        private String remediation;
    }
}
