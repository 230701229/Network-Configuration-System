package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="yang_models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YangModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String modelName;
    private String namespace;
    private String revision;
    
    @Enumerated(EnumType.STRING)
    private Device.DeviceType deviceType;
    
    private String description;
    
    @Column(columnDefinition="TEXT")
    private String modelContent;
    
    private boolean isValid;
    private String validationError;
    
    @ManyToOne
    @JoinColumn(name="created_by")
    private User createdBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
