package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String policyName;
    
    @Enumerated(EnumType.STRING)
    private Device.DeviceType deviceType;
    
    private String vendor;
    private String version;
    private String description;
    private boolean isActive;
    
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PolicyRule> rules;
    
    @ManyToOne
    @JoinColumn(name="created_by")
    private User createdBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
