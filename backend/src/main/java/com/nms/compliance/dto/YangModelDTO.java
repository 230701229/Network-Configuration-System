package com.nms.compliance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class YangModelDTO {
    private Long id;
    private String modelName;
    private String namespace;
    private String revision;
    private String deviceType;
    private String description;
    private String modelContent;
    private boolean isValid;
    private String validationError;
    private LocalDateTime createdAt;
}
