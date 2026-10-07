package com.nms.compliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String hostname;
    private String ipAddress;
    
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;
    
    private String vendor;
    private String model;
    
    @Enumerated(EnumType.STRING)
    private ManagementProtocol managementProtocol;
    
    @Builder.Default
    private Integer netconfPort = 830;
    
    @Builder.Default
    private Integer restconfPort = 443;
    
    private String username;
    private String encryptedPassword;
    
    @Enumerated(EnumType.STRING)
    private DeviceStatus status;
    
    private LocalDateTime lastSeen;
    
    @ManyToOne
    @JoinColumn(name="applied_policy_id")
    private Policy appliedPolicy;
    
    @ManyToOne
    @JoinColumn(name="yang_model_id")
    private YangModel yangModel;
    
    private String description;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum DeviceType { ROUTER, SWITCH, FIREWALL }
    public enum ManagementProtocol { NETCONF, RESTCONF, SSH, MOCK }
    public enum DeviceStatus { ONLINE, OFFLINE, UNKNOWN, UNREACHABLE }
}
