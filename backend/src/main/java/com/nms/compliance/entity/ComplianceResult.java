package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="compliance_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="scan_id")
    private ComplianceScan scan;
    
    @ManyToOne
    @JoinColumn(name="rule_id")
    private PolicyRule rule;
    
    private String parameter;
    private String expectedValue;
    private String actualValue;
    private boolean passed;
    
    @Enumerated(EnumType.STRING)
    private PolicyRule.Severity severity;
    
    private String description;
    private String remediation;
}
