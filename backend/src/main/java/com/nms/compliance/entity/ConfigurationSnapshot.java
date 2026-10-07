package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="configuration_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="device_id")
    private Device device;
    
    @Column(columnDefinition="jsonb")
    private String snapshotData;
    
    private String configHash;
    
    @Enumerated(EnumType.STRING)
    private Device.ManagementProtocol retrievalProtocol;
    
    private LocalDateTime retrievedAt;
    private String notes;
}
