package com.nms.compliance.config;

import com.nms.compliance.entity.*;
import com.nms.compliance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            PolicyRepository policyRepository,
            PolicyRuleRepository ruleRepository,
            DeviceRepository deviceRepository,
            YangModelRepository yangModelRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Checking database initialization state...");

            // 1. Ensure Admin and NetAdmin users exist with valid BCrypt hashes
            User admin = userRepository.findByUsername("admin").orElse(null);
            if (admin == null) {
                admin = User.builder()
                        .username("admin")
                        .email("admin@nms.local")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(User.UserRole.ADMIN)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                admin = userRepository.save(admin);
                log.info("Initialized default 'admin' user (password: admin123)");
            } else {
                // Ensure password hash is valid
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setEnabled(true);
                userRepository.save(admin);
            }

            User netadmin = userRepository.findByUsername("netadmin").orElse(null);
            if (netadmin == null) {
                netadmin = User.builder()
                        .username("netadmin")
                        .email("netadmin@nms.local")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(User.UserRole.NETWORK_ADMIN)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                netadmin = userRepository.save(netadmin);
                log.info("Initialized default 'netadmin' user (password: admin123)");
            } else {
                netadmin.setPasswordHash(passwordEncoder.encode("admin123"));
                netadmin.setEnabled(true);
                userRepository.save(netadmin);
            }

            // 2. Initialize default policies and rules if none exist
            if (policyRepository.count() == 0) {
                log.info("Seeding default compliance policies and rules...");

                Policy routerPolicy = Policy.builder()
                        .policyName("Standard Router Security Baseline")
                        .deviceType(Device.DeviceType.ROUTER)
                        .vendor("Cisco")
                        .version("1.0")
                        .description("Security baseline for enterprise edge and core routers")
                        .isActive(true)
                        .createdBy(admin)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .rules(new ArrayList<>())
                        .build();
                routerPolicy = policyRepository.save(routerPolicy);

                List<PolicyRule> routerRules = List.of(
                        createRule(routerPolicy, "ssh", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "SSH must be enabled for secure remote management", "Enable SSH v2: ip ssh version 2", 1),
                        createRule(routerPolicy, "telnet", PolicyRule.RuleOperator.EQUALS, "false", PolicyRule.Severity.CRITICAL, "Insecure Telnet protocol must be disabled", "Disable Telnet: transport input ssh", 2),
                        createRule(routerPolicy, "ntp", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.MEDIUM, "Network Time Protocol must be enabled", "Enable NTP: ntp server 10.0.0.1", 3),
                        createRule(routerPolicy, "snmp", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.MEDIUM, "SNMP monitoring must be enabled", "Enable SNMPv3: snmp-server group ...", 4),
                        createRule(routerPolicy, "ospf", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "OSPF routing process must be running", "Enable OSPF: router ospf 1", 5),
                        createRule(routerPolicy, "http", PolicyRule.RuleOperator.EQUALS, "false", PolicyRule.Severity.HIGH, "Insecure HTTP server must be disabled", "Disable HTTP: no ip http server", 6),
                        createRule(routerPolicy, "logging", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.LOW, "Syslog remote logging must be configured", "Enable logging: logging host 10.0.0.50", 7),
                        createRule(routerPolicy, "password_encryption", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "Password encryption service must be enabled", "Enable: service password-encryption", 8)
                );
                ruleRepository.saveAll(routerRules);

                Policy switchPolicy = Policy.builder()
                        .policyName("Standard Switch Hardening Baseline")
                        .deviceType(Device.DeviceType.SWITCH)
                        .vendor("Cisco")
                        .version("1.0")
                        .description("Security hardening policy for access and distribution switches")
                        .isActive(true)
                        .createdBy(admin)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .rules(new ArrayList<>())
                        .build();
                switchPolicy = policyRepository.save(switchPolicy);

                List<PolicyRule> switchRules = List.of(
                        createRule(switchPolicy, "ssh", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "SSH must be enabled", "Enable SSH: ip ssh version 2", 1),
                        createRule(switchPolicy, "telnet", PolicyRule.RuleOperator.EQUALS, "false", PolicyRule.Severity.CRITICAL, "Telnet must be disabled", "Disable Telnet: transport input ssh", 2),
                        createRule(switchPolicy, "spanning_tree", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "Spanning Tree Protocol must be enabled to prevent loops", "Enable STP: spanning-tree mode rapid-pvst", 3),
                        createRule(switchPolicy, "port_security", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.HIGH, "Port security must be active on access ports", "Enable port security: switchport port-security", 4),
                        createRule(switchPolicy, "dhcp_snooping", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.MEDIUM, "DHCP Snooping must be active to prevent rogue DHCP", "Enable: ip dhcp snooping", 5),
                        createRule(switchPolicy, "snmp", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.MEDIUM, "SNMP monitoring must be active", "Enable SNMPv3", 6),
                        createRule(switchPolicy, "ntp", PolicyRule.RuleOperator.EQUALS, "true", PolicyRule.Severity.MEDIUM, "NTP synchronization must be active", "Enable NTP", 7)
                );
                ruleRepository.saveAll(switchRules);

                // 3. Initialize Demo Devices
                if (deviceRepository.count() == 0) {
                    log.info("Seeding initial network devices...");
                    List<Device> devices = List.of(
                            Device.builder()
                                    .hostname("Router-01")
                                    .ipAddress("192.168.1.1")
                                    .deviceType(Device.DeviceType.ROUTER)
                                    .vendor("Cisco")
                                    .model("CSR1000v")
                                    .managementProtocol(Device.ManagementProtocol.MOCK)
                                    .netconfPort(830)
                                    .restconfPort(443)
                                    .username("cisco")
                                    .encryptedPassword("cisco123")
                                    .status(Device.DeviceStatus.ONLINE)
                                    .appliedPolicy(routerPolicy)
                                    .description("Core Gateway Router - Headquarters (100% Compliant Demo)")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build(),
                            Device.builder()
                                    .hostname("Router-02")
                                    .ipAddress("192.168.1.2")
                                    .deviceType(Device.DeviceType.ROUTER)
                                    .vendor("Cisco")
                                    .model("CSR1000v")
                                    .managementProtocol(Device.ManagementProtocol.MOCK)
                                    .netconfPort(830)
                                    .restconfPort(443)
                                    .username("cisco")
                                    .encryptedPassword("cisco123")
                                    .status(Device.DeviceStatus.ONLINE)
                                    .appliedPolicy(routerPolicy)
                                    .description("Branch Office Router - Branch 1 (Minor Drift: Telnet enabled)")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build(),
                            Device.builder()
                                    .hostname("Router-03")
                                    .ipAddress("192.168.1.3")
                                    .deviceType(Device.DeviceType.ROUTER)
                                    .vendor("Cisco")
                                    .model("CSR1000v")
                                    .managementProtocol(Device.ManagementProtocol.MOCK)
                                    .netconfPort(830)
                                    .restconfPort(443)
                                    .username("cisco")
                                    .encryptedPassword("cisco123")
                                    .status(Device.DeviceStatus.ONLINE)
                                    .appliedPolicy(routerPolicy)
                                    .description("Legacy Branch Router - Branch 2 (Critical Non-Compliant)")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build(),
                            Device.builder()
                                    .hostname("Switch-01")
                                    .ipAddress("192.168.2.1")
                                    .deviceType(Device.DeviceType.SWITCH)
                                    .vendor("Cisco")
                                    .model("Catalyst 9300")
                                    .managementProtocol(Device.ManagementProtocol.MOCK)
                                    .netconfPort(830)
                                    .restconfPort(443)
                                    .username("cisco")
                                    .encryptedPassword("cisco123")
                                    .status(Device.DeviceStatus.ONLINE)
                                    .appliedPolicy(switchPolicy)
                                    .description("Distribution Switch - Building A (Port Security / DHCP Drift)")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build(),
                            Device.builder()
                                    .hostname("Switch-02")
                                    .ipAddress("192.168.2.2")
                                    .deviceType(Device.DeviceType.SWITCH)
                                    .vendor("Cisco")
                                    .model("Catalyst 9300")
                                    .managementProtocol(Device.ManagementProtocol.MOCK)
                                    .netconfPort(830)
                                    .restconfPort(443)
                                    .username("cisco")
                                    .encryptedPassword("cisco123")
                                    .status(Device.DeviceStatus.ONLINE)
                                    .appliedPolicy(switchPolicy)
                                    .description("Access Switch - Server Room (100% Compliant)")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build()
                    );
                    deviceRepository.saveAll(devices);
                    log.info("Seeded 5 demo network devices.");
                }
            }

            log.info("Database initialization completed successfully.");
        };
    }

    private PolicyRule createRule(Policy policy, String param, PolicyRule.RuleOperator op, String expected,
                                  PolicyRule.Severity sev, String desc, String remed, int order) {
        return PolicyRule.builder()
                .policy(policy)
                .parameter(param)
                .operator(op)
                .expectedValue(expected)
                .severity(sev)
                .description(desc)
                .remediation(remed)
                .ruleOrder(order)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
