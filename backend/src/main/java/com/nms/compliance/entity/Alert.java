package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="device_id")
    private Device device;
    
    @ManyToOne
    @JoinColumn(name="scan_id")
    private ComplianceScan scan;
    
    @ManyToOne
    @JoinColumn(name="drift_event_id")
    private DriftEvent driftEvent;
    
    private String title;
    
    @Column(columnDefinition="TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    private PolicyRule.Severity severity;
    
    @Enumerated(EnumType.STRING)
    private AlertStatus status;
    
    private LocalDateTime detectedAt;
    private LocalDateTime acknowledgedAt;
    
    @ManyToOne
    @JoinColumn(name="acknowledged_by")
    private User acknowledgedBy;

    public enum AlertStatus { OPEN, ACKNOWLEDGED, RESOLVED }
}
