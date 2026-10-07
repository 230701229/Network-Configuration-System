package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="policy_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="policy_id")
    private Policy policy;
    
    private String parameter;
    
    @Enumerated(EnumType.STRING)
    private RuleOperator operator;
    
    private String expectedValue;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    private String description;
    private String remediation;
    private int ruleOrder;
    
    private LocalDateTime createdAt;

    public enum RuleOperator { EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, GREATER_THAN, LESS_THAN }
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
}
