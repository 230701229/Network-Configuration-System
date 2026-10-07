# GNS3 Real-Time Network Configuration Drift Detection Setup

This guide provides end-to-end instructions for configuring virtual network devices in **GNS3** (Cisco IOS-XE / IOSv) and integrating them with the **Network Configuration Compliance System** over **NETCONF** and **RESTCONF** with real-time configuration drift detection.

---

## 1. Network Topology Architecture

```
         +------------------------------------+
         |   Host Machine running NMS Backend |
         |   (http://localhost:8080)          |
         +------------------------------------+
                           |
                     (NAT / Bridge)
                           |
                   [ GNS3 NAT Cloud ]
                           |
                   [ Ethernet Switch ]
                     /           \
   [Router-GNS3-01]                 [Router-GNS3-02]
   Cisco IOS-XE (CSR1000v)          Cisco IOSv
   IP: 192.168.122.10               IP: 192.168.122.11
   NETCONF: Port 830                NETCONF: Port 830
   RESTCONF: Port 443               RESTCONF: Port 443
```

### Topology Components:
- **GNS3 NAT Node**: Connects virtual devices to your host PC's virtual NAT network adapter (e.g. `192.168.122.0/24` or local DHCP bridge).
- **Virtual Routers**: Cisco IOS-XE (CSR1000v / Catalyst 8000v / IOSv-L3).
- **Management Protocol**:
  - **NETCONF**: SSH subsystem running on port `830` using standard IETF RFC 6241 and Cisco native YANG models.
  - **RESTCONF**: HTTP/HTTPS server running on port `443` using RFC 8040 and `application/yang-data+json`.

---

## 2. Cisco IOS-XE Virtual Router Configuration

Access the console of your virtual router in GNS3 (e.g., via PuTTY/SuperPutty/Solar-PuTTY) and apply the following configuration:

```cisco
enable
configure terminal

! 1. Hostname and Domain
hostname Router-GNS3-01
ip domain-name nms.local

! 2. Generate RSA Key Pair (required for SSH & NETCONF)
crypto key generate rsa modulus 2048
ip ssh version 2
ip ssh time-out 60
ip ssh authentication-retries 3

! 3. Administrative Credentials (used by NMS backend)
username admin privilege 15 secret Admin123!

! 4. Management Interface Configuration
interface GigabitEthernet1
 description Management-Interface-To-NMS
 ip address dhcp
 no shutdown
exit

! 5. Line VTY Configuration (Secure Remote Access)
line vty 0 4
 transport input ssh
 login local
exit

! 6. Enable NETCONF Subsystem (Port 830)
netconf-yang
netconf-yang cisco-odm polling-enable

! 7. Enable RESTCONF Subsystem (Port 443 HTTPS)
ip http secure-server
ip http authentication local
restconf

! 8. Baseline Security and Compliance Configuration
service password-encryption
no service telnet
ntp server 10.0.0.1
logging buffered 64000

end
write memory
```

---

## 3. Verifying Device Connectivity from Host PC

Before adding the device to the dashboard, verify network connectivity from your host terminal:

### A. Test Ping and SSH
```bash
ping 192.168.122.10
ssh -p 22 admin@192.168.122.10
```

### B. Test NETCONF (Port 830)
```bash
ssh -p 830 admin@192.168.122.10 -s netconf
```
*Expected Output:*
You should receive the XML `<hello>` capability advertisement from the router:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
  <capabilities>
    <capability>urn:ietf:params:netconf:base:1.0</capability>
    <capability>http://cisco.com/ns/yang/Cisco-IOS-XE-native</capability>
  </capabilities>
  <session-id>12</session-id>
</hello>
]]>]]>
```

### C. Test RESTCONF (Port 443)
```bash
curl -k -u admin:Admin123! \
  https://192.168.122.10/restconf/data/Cisco-IOS-XE-native:native
```

---

## 4. Registering Device in NMS Dashboard

1. Log into the NMS Dashboard (`http://localhost:5173` or `http://localhost:80`) using `admin` / `admin123`.
2. Go to **Devices** → **Add Device**.
3. Fill in the connection settings:
   - **Hostname**: `Router-GNS3-01`
   - **IP Address**: `192.168.122.10`
   - **Device Type**: `ROUTER`
   - **Management Protocol**: `NETCONF` (or `RESTCONF`)
   - **NETCONF Port**: `830` (or RESTCONF Port `443`)
   - **Username**: `admin`
   - **Password**: `Admin123!`
4. Click **Test Connection** — A green checkmark confirms the connection.
5. Click **Add Device**.

---

## 5. Live Demonstration: Real-Time Drift Detection Workflow

### Phase 1: Initial Baseline Verification
1. The background monitor polls the router (every 5 seconds).
2. The initial configuration is retrieved and saved as `INITIAL_BASELINE`.
3. Deterministic compliance evaluation runs automatically:
   - `ssh-enabled`: `true`
   - `telnet-enabled`: `false`
   - `ntp-enabled`: `true`
   - `logging-enabled`: `true`
4. The dashboard displays:
   - Status: **COMPLIANT**
   - Score: **100%**
   - Live Monitoring: **Active (5s)**

---

### Phase 2: Inject Configuration Drift in GNS3
On the GNS3 router console, simulate an unauthorized security change by enabling Telnet:

```cisco
Router-GNS3-01# configure terminal
Router-GNS3-01(config)# line vty 0 4
Router-GNS3-01(config-line)# transport input telnet ssh
Router-GNS3-01(config-line)# exit
Router-GNS3-01(config)# end
```

---

### Phase 3: Automated Real-Time Detection (< 5–10 seconds)
1. Within 5 seconds, the background polling thread retrieves the updated configuration.
2. The system compares the new configuration snapshot with the **previous snapshot**:
   - SHA-256 hash has changed!
   - Diff detected:
     ```
     telnet-enabled: false → true
     ```
3. The deterministic compliance engine automatically re-evaluates:
   - Score drops: **100% → 87.5%** (or lower based on policy rules).
   - Compliance status changes to **MINOR_DRIFT** or **CRITICAL_NON_COMPLIANT**.
4. The backend generates a **Drift Event**:
   - Parameter: `telnet-enabled`
   - Previous Value: `false`
   - Current Value: `true`
   - Severity: `CRITICAL`
   - Impact: `Compliance score: 100% → 87.5%`
5. An alert is created and the AI service generates an explanation:
   - *Explanation*: Telnet transmits credentials in unencrypted plaintext across the network, exposing the management plane to credential harvesting and man-in-the-middle attacks.
   - *Remediation*: Configure `transport input ssh` on line VTY 0 to 4.
6. The React Dashboard updates in **real-time via Server-Sent Events (SSE)** without requiring a page refresh!

---

### Phase 4: Remediation Verification
Return to the router console and fix the violation:

```cisco
Router-GNS3-01# configure terminal
Router-GNS3-01(config)# line vty 0 4
Router-GNS3-01(config-line)# transport input ssh
Router-GNS3-01(config-line)# exit
Router-GNS3-01(config)# end
```

Within 5 seconds:
- The drift event on `telnet-enabled` is automatically marked **RESOLVED**.
- Compliance score returns to **100% COMPLIANT**.
- Real-time notification appears on the dashboard.
