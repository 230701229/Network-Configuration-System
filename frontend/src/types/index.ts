export type UserRole = 'ADMIN' | 'NETWORK_ADMIN';
export type DeviceType = 'ROUTER' | 'SWITCH' | 'FIREWALL';
export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN' | 'UNREACHABLE';
export type ManagementProtocol = 'NETCONF' | 'RESTCONF' | 'SSH' | 'MOCK';
export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
export type ComplianceStatus = 'COMPLIANT' | 'MINOR_DRIFT' | 'MAJOR_DRIFT' | 'CRITICAL_NON_COMPLIANT';
export type DriftStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
export type RuleOperator = 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'NOT_CONTAINS' | 'GREATER_THAN' | 'LESS_THAN';

export interface LoginRequest { username: string; password: string; }
export interface LoginResponse { token: string; username: string; role: UserRole; expiresAt: string; }

export interface Device {
  id: number;
  hostname: string;
  ipAddress: string;
  deviceType: DeviceType;
  vendor?: string;
  model?: string;
  managementProtocol: ManagementProtocol;
  netconfPort?: number;
  restconfPort?: number;
  username?: string;
  status: DeviceStatus;
  lastSeen?: string;
  appliedPolicyId?: number;
  appliedPolicyName?: string;
  yangModelId?: number;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDeviceRequest {
  hostname: string;
  ipAddress: string;
  deviceType: DeviceType;
  vendor?: string;
  model?: string;
  managementProtocol: ManagementProtocol;
  netconfPort?: number;
  restconfPort?: number;
  username?: string;
  password?: string;
  appliedPolicyId?: number;
  description?: string;
}

export interface PolicyRule {
  id: number;
  parameter: string;
  operator: RuleOperator;
  expectedValue: string;
  severity: Severity;
  description?: string;
  remediation?: string;
  ruleOrder: number;
}

export interface Policy {
  id: number;
  policyName: string;
  deviceType: DeviceType;
  vendor?: string;
  version: string;
  description?: string;
  isActive: boolean;
  rules: PolicyRule[];
  createdAt: string;
  updatedAt: string;
}

export interface NormalizedConfiguration {
  deviceId: number;
  hostname: string;
  retrievedAt: string;
  protocol: ManagementProtocol;
  parameters: Record<string, string>;
}

export interface ComplianceResult {
  id: number;
  parameter: string;
  expectedValue: string;
  actualValue?: string;
  passed: boolean;
  severity: Severity;
  description?: string;
  remediation?: string;
}

export interface ComplianceScan {
  id: number;
  deviceId: number;
  deviceHostname: string;
  policyId: number;
  policyName: string;
  complianceScore: number;
  complianceStatus: ComplianceStatus;
  totalRules: number;
  passedRules: number;
  failedRules: number;
  criticalViolations: number;
  highViolations: number;
  mediumViolations: number;
  lowViolations: number;
  triggeredBy: string;
  scannedAt: string;
  results: ComplianceResult[];
}

export interface DriftEvent {
  id: number;
  deviceId: number;
  deviceHostname: string;
  parameter: string;
  expectedValue: string;
  actualValue?: string;
  severity: Severity;
  status: DriftStatus;
  description?: string;
  remediation?: string;
  detectedAt: string;
  resolvedAt?: string;
}

export interface AIAnalysis {
  id: number;
  scanId: number;
  deviceId: number;
  summary: string;
  analysis: string;
  recommendations: string[];
  priority: string;
  impact?: string;
  aiAvailable: boolean;
  modelUsed?: string;
  createdAt: string;
}

export interface Alert {
  id: number;
  deviceId: number;
  deviceHostname: string;
  title: string;
  message: string;
  severity: Severity;
  status: AlertStatus;
  detectedAt: string;
  acknowledgedAt?: string;
}

export interface DashboardSummary {
  totalDevices: number;
  compliantDevices: number;
  devicesWithDrift: number;
  criticalViolations: number;
  averageComplianceScore: number;
  recentAlerts: Alert[];
}

export interface DeviceComplianceStatus {
  deviceId: number;
  hostname: string;
  latestScore: number;
  status: ComplianceStatus;
  lastScanned?: string;
}

export interface DashboardTrends {
  dailyScores: { date: string; score: number }[];
  deviceComplianceList: DeviceComplianceStatus[];
  driftTrend: { date: string; count: number }[];
  violationsBySeverity: { severity: string; count: number }[];
}

export interface TestConnectionResponse { success: boolean; message: string; protocol: string; responseTime?: number; }

export interface YangModel {
  id: number;
  modelName: string;
  namespace: string;
  revision?: string;
  deviceType: DeviceType;
  description?: string;
  modelContent?: string;
  isValid: boolean;
  validationError?: string;
  createdAt: string;
}

export interface ApiError { error: string; message: string; details?: Record<string, string>; }

export interface ParameterDiff {
  parameter: string;
  baselineValue: string;
  currentValue: string;
  changeType: 'MODIFIED' | 'ADDED' | 'REMOVED';
  severity: Severity;
  remediation?: string;
}

export interface ConfigurationDiffData {
  deviceId: number;
  hostname: string;
  hasDrift: boolean;
  totalChanges: number;
  baselineTime?: string;
  currentTime?: string;
  baselineHash?: string;
  currentHash?: string;
  changes: ParameterDiff[];
  baselineConfig: Record<string, string>;
  currentConfig: Record<string, string>;
}

export interface Gns3ServerStatus {
  connected: boolean;
  version?: string;
  url: string;
  responseTimeMs?: number;
  message: string;
  statusCode?: number;
}

export interface Gns3Project {
  projectId: string;
  name: string;
  status: string;
}

export interface Gns3Node {
  nodeId: string;
  name: string;
  nodeType: string;
  status: string;
  consolePort?: number;
}
