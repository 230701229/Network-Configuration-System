package com.nms.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeNotificationDTO {
    public enum EventType {
        DRIFT_DETECTED,
        DRIFT_RESOLVED,
        SCAN_COMPLETED,
        DEVICE_STATUS_CHANGED,
        ALERT_CREATED,
        HEARTBEAT
    }

    private EventType eventType;
    private Long deviceId;
    private String hostname;
    private String message;
    private Object data;
    private LocalDateTime timestamp;
}
