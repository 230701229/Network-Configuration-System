package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ai_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="scan_id")
    private ComplianceScan scan;
    
    @ManyToOne
    @JoinColumn(name="device_id")
    private Device device;
    
    @Column(columnDefinition="TEXT")
    private String summary;
    
    @Column(columnDefinition="TEXT")
    private String analysis;
    
    @Column(columnDefinition="jsonb")
    private String recommendations;
    
    private String priority;
    private String modelUsed;
    private Integer promptTokens;
    private Integer completionTokens;
    private boolean aiAvailable;
    
    private LocalDateTime createdAt;
}
