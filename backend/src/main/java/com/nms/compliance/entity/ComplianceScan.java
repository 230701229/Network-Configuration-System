package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="compliance_scans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceScan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="device_id")
    private Device device;
    
    @ManyToOne
    @JoinColumn(name="policy_id")
    private Policy policy;
    
    @ManyToOne
    @JoinColumn(name="snapshot_id")
    private ConfigurationSnapshot snapshot;
    
    private BigDecimal complianceScore;
    
    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;
    
    private int totalRules;
    private int passedRules;
    private int failedRules;
    
    private int criticalViolations;
    private int highViolations;
    private int mediumViolations;
    private int lowViolations;
    
    @Builder.Default
    private String triggeredBy = "MANUAL";
    
    private LocalDateTime scannedAt;
    
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL)
    private List<ComplianceResult> results;

    public enum ComplianceStatus { COMPLIANT, MINOR_DRIFT, MAJOR_DRIFT, CRITICAL_NON_COMPLIANT }
}
