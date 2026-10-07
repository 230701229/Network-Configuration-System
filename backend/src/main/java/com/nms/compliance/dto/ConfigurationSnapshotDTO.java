package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConfigurationSnapshotDTO {
    private Long id;
    private Long deviceId;
    private String snapshotData;
    private String configHash;
    private String retrievalProtocol;
    private LocalDateTime retrievedAt;
    private String notes;
}
