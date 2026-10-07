-- =====================================================================
-- Seed Data for Demo
-- Network Configuration Compliance System
-- =====================================================================

-- =====================================================================
-- DEFAULT ADMIN USERS
-- Password for both: admin123 (bcrypt hashed)
-- =====================================================================

INSERT INTO users (username, email, password_hash, role) VALUES
('admin', 'admin@nms.local', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFhpGzqA2Gx1Ki.', 'ADMIN'),
('netadmin', 'netadmin@nms.local', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFhpGzqA2Gx1Ki.', 'NETWORK_ADMIN');

-- =====================================================================
-- YANG MODELS
-- =====================================================================

INSERT INTO yang_models (model_name, namespace, revision, device_type, description, model_content, is_valid, created_by) VALUES
(
    'network-device',
    'urn:network-device',
    '2024-01-01',
    'ROUTER',
    'Standard YANG model for router configuration compliance',
    'module network-device {
  namespace "urn:network-device";
  prefix nd;

  organization "NMS Project";
  contact "admin@nms.local";
  description "YANG model for network device configuration compliance";
  revision 2024-01-01 {
    description "Initial revision";
  }

  container system {
    description "System-level configuration parameters";

    leaf hostname {
      type string;
      description "Device hostname";
    }

    leaf ssh-enabled {
      type boolean;
      description "Whether SSH is enabled";
    }

    leaf telnet-enabled {
      type boolean;
      description "Whether Telnet is enabled (should be false)";
    }

    leaf ntp-enabled {
      type boolean;
      description "Whether NTP is enabled";
    }

    leaf snmp-enabled {
      type boolean;
      description "Whether SNMP is enabled";
    }

    leaf ospf-enabled {
      type boolean;
      description "Whether OSPF routing is enabled";
    }

    leaf http-enabled {
      type boolean;
      description "Whether HTTP management is enabled (should be false)";
    }

    leaf https-enabled {
      type boolean;
      description "Whether HTTPS management is enabled";
    }

    leaf logging-enabled {
      type boolean;
      description "Whether syslog logging is enabled";
    }

    leaf banner-configured {
      type boolean;
      description "Whether login banner is configured";
    }

    leaf password-encryption {
      type boolean;
      description "Whether password encryption service is enabled";
    }

    leaf max-login-attempts {
      type uint8 {
        range "1..10";
      }
      description "Maximum failed login attempts before lockout";
    }
  }
}',
    TRUE,
    1
),
(
    'switch-config',
    'urn:switch-config',
    '2024-01-01',
    'SWITCH',
    'Standard YANG model for switch configuration compliance',
    'module switch-config {
  namespace "urn:switch-config";
  prefix sc;

  organization "NMS Project";
  description "YANG model for network switch configuration compliance";
  revision 2024-01-01 {
    description "Initial revision";
  }

  container switch {
    description "Switch configuration";

    leaf hostname {
      type string;
    }

    leaf ssh-enabled {
      type boolean;
    }

    leaf telnet-enabled {
      type boolean;
    }

    leaf ntp-enabled {
      type boolean;
    }

    leaf snmp-enabled {
      type boolean;
    }

    leaf spanning-tree-enabled {
      type boolean;
    }

    leaf port-security-enabled {
      type boolean;
    }

    leaf dhcp-snooping-enabled {
      type boolean;
    }
  }
}',
    TRUE,
    1
);

-- =====================================================================
-- POLICIES
-- =====================================================================

INSERT INTO policies (policy_name, device_type, vendor, version, description, is_active, created_by) VALUES
(
    'Standard Router Security Policy',
    'ROUTER',
    'Cisco',
    '2.0',
    'Standard security compliance policy for all enterprise routers. Enforces SSH, disables Telnet, ensures NTP/SNMP/OSPF are properly configured.',
    TRUE,
    1
),
(
    'Standard Switch Security Policy',
    'SWITCH',
    'Cisco',
    '1.0',
    'Standard security compliance policy for enterprise switches. Enforces SSH, spanning tree, port security, and DHCP snooping.',
    TRUE,
    1
),
(
    'Strict Router Security Policy',
    'ROUTER',
    NULL,
    '3.0',
    'Strict security policy for high-security router deployments. All parameters must match exactly.',
    TRUE,
    1
);

-- =====================================================================
-- POLICY RULES — Standard Router Security Policy (id=1)
-- =====================================================================

INSERT INTO policy_rules (policy_id, parameter, operator, expected_value, severity, description, remediation, rule_order) VALUES
(1, 'ssh',              'EQUALS', 'true',  'HIGH',     'SSH must be enabled for secure remote management',              'Enable SSH: ip ssh version 2',                              1),
(1, 'telnet',           'EQUALS', 'false', 'CRITICAL', 'Telnet must be disabled as it transmits credentials in plaintext', 'Disable Telnet: no service telnet',                       2),
(1, 'ntp',              'EQUALS', 'true',  'MEDIUM',   'NTP must be enabled for accurate time synchronization',         'Enable NTP: ntp server <ntp-server-ip>',                    3),
(1, 'snmp',             'EQUALS', 'true',  'MEDIUM',   'SNMP must be enabled for network monitoring',                   'Enable SNMP: snmp-server community <community> RO',         4),
(1, 'ospf',             'EQUALS', 'true',  'HIGH',     'OSPF must be enabled for dynamic routing',                      'Enable OSPF: router ospf 1',                                5),
(1, 'http',             'EQUALS', 'false', 'HIGH',     'HTTP management interface must be disabled',                    'Disable HTTP: no ip http server',                           6),
(1, 'logging',          'EQUALS', 'true',  'MEDIUM',   'Syslog logging must be enabled',                                'Enable logging: logging on',                                7),
(1, 'password_encryption', 'EQUALS', 'true', 'HIGH',  'Password encryption service must be enabled',                   'Enable: service password-encryption',                       8);

-- =====================================================================
-- POLICY RULES — Standard Switch Security Policy (id=2)
-- =====================================================================

INSERT INTO policy_rules (policy_id, parameter, operator, expected_value, severity, description, remediation, rule_order) VALUES
(2, 'ssh',              'EQUALS', 'true',  'HIGH',     'SSH must be enabled',                                    'Enable SSH: ip ssh version 2',                         1),
(2, 'telnet',           'EQUALS', 'false', 'CRITICAL', 'Telnet must be disabled',                                'Disable Telnet: no service telnet',                    2),
(2, 'ntp',              'EQUALS', 'true',  'MEDIUM',   'NTP must be enabled',                                    'Enable NTP',                                           3),
(2, 'snmp',             'EQUALS', 'true',  'MEDIUM',   'SNMP must be enabled for monitoring',                    'Enable SNMP',                                          4),
(2, 'spanning_tree',    'EQUALS', 'true',  'HIGH',     'Spanning Tree must be enabled to prevent loops',         'Enable STP: spanning-tree mode rapid-pvst',            5),
(2, 'port_security',    'EQUALS', 'true',  'MEDIUM',   'Port security must be enabled',                          'Enable port security on access ports',                 6),
(2, 'dhcp_snooping',    'EQUALS', 'true',  'MEDIUM',   'DHCP snooping must be enabled',                          'Enable: ip dhcp snooping',                             7);

-- =====================================================================
-- POLICY RULES — Strict Router Security Policy (id=3)
-- =====================================================================

INSERT INTO policy_rules (policy_id, parameter, operator, expected_value, severity, description, remediation, rule_order) VALUES
(3, 'ssh',              'EQUALS', 'true',  'CRITICAL', 'SSH must be enabled',                'Enable SSH: ip ssh version 2',             1),
(3, 'telnet',           'EQUALS', 'false', 'CRITICAL', 'Telnet must be disabled',            'Disable Telnet',                           2),
(3, 'ntp',              'EQUALS', 'true',  'HIGH',     'NTP must be enabled',                'Enable NTP',                               3),
(3, 'snmp',             'EQUALS', 'true',  'HIGH',     'SNMP must be enabled',               'Enable SNMP',                              4),
(3, 'ospf',             'EQUALS', 'true',  'HIGH',     'OSPF must be enabled',               'Enable OSPF',                              5),
(3, 'http',             'EQUALS', 'false', 'CRITICAL', 'HTTP must be disabled',              'Disable HTTP',                             6),
(3, 'https',            'EQUALS', 'true',  'HIGH',     'HTTPS management must be enabled',   'Enable HTTPS: ip http secure-server',      7),
(3, 'logging',          'EQUALS', 'true',  'HIGH',     'Logging must be enabled',            'Enable logging',                           8),
(3, 'banner_configured','EQUALS', 'true',  'MEDIUM',   'Login banner must be configured',    'Configure banner: banner motd',            9),
(3, 'password_encryption','EQUALS','true', 'HIGH',     'Password encryption must be enabled','Enable: service password-encryption',      10);

-- =====================================================================
-- DEVICES
-- =====================================================================

INSERT INTO devices (hostname, ip_address, device_type, vendor, model, management_protocol, netconf_port, restconf_port, username, encrypted_password, status, applied_policy_id, description) VALUES
('Router-01', '192.168.1.1',  'ROUTER', 'Cisco', 'ISR4331', 'MOCK', 830, 443, 'admin', 'bW9jaw==', 'ONLINE',  1, 'Core router - fully compliant'),
('Router-02', '192.168.1.2',  'ROUTER', 'Cisco', 'ISR4331', 'MOCK', 830, 443, 'admin', 'bW9jaw==', 'ONLINE',  1, 'Distribution router - telnet enabled, NTP missing'),
('Router-03', '192.168.1.3',  'ROUTER', 'Cisco', 'ISR4451', 'MOCK', 830, 443, 'admin', 'bW9jaw==', 'ONLINE',  1, 'Branch router - severely non-compliant'),
('Switch-01', '192.168.2.1',  'SWITCH', 'Cisco', 'C9300',   'MOCK', 830, 443, 'admin', 'bW9jaw==', 'ONLINE',  2, 'Core switch - mostly compliant'),
('Switch-02', '192.168.2.2',  'SWITCH', 'Cisco', 'C9300',   'MOCK', 830, 443, 'admin', 'bW9jaw==', 'ONLINE',  2, 'Access switch - fully compliant');
