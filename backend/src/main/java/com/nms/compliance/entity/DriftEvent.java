package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="drift_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftEvent {
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
    @JoinColumn(name="policy_id")
    private Policy policy;
    
    private String parameter;
    private String expectedValue;
    private String actualValue;
    
    @Enumerated(EnumType.STRING)
    private PolicyRule.Severity severity;
    
    @Enumerated(EnumType.STRING)
    private DriftStatus status;
    
    private String description;
    private String remediation;
    
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    
    @ManyToOne
    @JoinColumn(name="resolved_by")
    private User resolvedBy;

    public enum DriftStatus { OPEN, ACKNOWLEDGED, RESOLVED }
}
