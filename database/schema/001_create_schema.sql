-- =====================================================================
-- Network Configuration Compliance System - Database Schema
-- PostgreSQL 15+
-- =====================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================================
-- ENUM TYPES
-- =====================================================================

CREATE TYPE user_role AS ENUM ('ADMIN', 'NETWORK_ADMIN');

CREATE TYPE device_type AS ENUM ('ROUTER', 'SWITCH', 'FIREWALL');

CREATE TYPE device_status AS ENUM ('ONLINE', 'OFFLINE', 'UNKNOWN', 'UNREACHABLE');

CREATE TYPE management_protocol AS ENUM ('NETCONF', 'RESTCONF', 'SSH', 'MOCK');

CREATE TYPE severity AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO');

CREATE TYPE rule_operator AS ENUM (
    'EQUALS', 'NOT_EQUALS', 'CONTAINS', 'NOT_CONTAINS',
    'GREATER_THAN', 'LESS_THAN'
);

CREATE TYPE compliance_status AS ENUM (
    'COMPLIANT', 'MINOR_DRIFT', 'MAJOR_DRIFT', 'CRITICAL_NON_COMPLIANT'
);

CREATE TYPE drift_status AS ENUM ('OPEN', 'ACKNOWLEDGED', 'RESOLVED');

CREATE TYPE alert_status AS ENUM ('OPEN', 'ACKNOWLEDGED', 'RESOLVED');

-- =====================================================================
-- TABLE: users
-- =====================================================================

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        user_role NOT NULL DEFAULT 'NETWORK_ADMIN',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: yang_models
-- =====================================================================

CREATE TABLE yang_models (
    id              BIGSERIAL PRIMARY KEY,
    model_name      VARCHAR(100) NOT NULL,
    namespace       VARCHAR(255) NOT NULL,
    revision        VARCHAR(20),
    device_type     device_type NOT NULL,
    description     TEXT,
    model_content   TEXT,
    is_valid        BOOLEAN DEFAULT FALSE,
    validation_error TEXT,
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: policies
-- =====================================================================

CREATE TABLE policies (
    id              BIGSERIAL PRIMARY KEY,
    policy_name     VARCHAR(100) NOT NULL,
    device_type     device_type NOT NULL,
    vendor          VARCHAR(50),
    version         VARCHAR(20) NOT NULL DEFAULT '1.0',
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: policy_rules
-- =====================================================================

CREATE TABLE policy_rules (
    id              BIGSERIAL PRIMARY KEY,
    policy_id       BIGINT NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    parameter       VARCHAR(100) NOT NULL,
    operator        rule_operator NOT NULL DEFAULT 'EQUALS',
    expected_value  VARCHAR(255) NOT NULL,
    severity        severity NOT NULL DEFAULT 'MEDIUM',
    description     TEXT,
    remediation     TEXT,
    rule_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: devices
-- =====================================================================

CREATE TABLE devices (
    id                  BIGSERIAL PRIMARY KEY,
    hostname            VARCHAR(100) NOT NULL,
    ip_address          VARCHAR(45) NOT NULL,
    device_type         device_type NOT NULL,
    vendor              VARCHAR(50),
    model               VARCHAR(100),
    management_protocol management_protocol NOT NULL DEFAULT 'MOCK',
    netconf_port        INTEGER DEFAULT 830,
    restconf_port       INTEGER DEFAULT 443,
    username            VARCHAR(100),
    encrypted_password  VARCHAR(500),
    status              device_status NOT NULL DEFAULT 'UNKNOWN',
    last_seen           TIMESTAMP,
    applied_policy_id   BIGINT REFERENCES policies(id),
    yang_model_id       BIGINT REFERENCES yang_models(id),
    description         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: configuration_snapshots
-- =====================================================================

CREATE TABLE configuration_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    snapshot_data       JSONB NOT NULL,
    config_hash         VARCHAR(64) NOT NULL,
    retrieval_protocol  management_protocol NOT NULL,
    retrieved_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    notes               TEXT
);

-- =====================================================================
-- TABLE: compliance_scans
-- =====================================================================

CREATE TABLE compliance_scans (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    policy_id           BIGINT NOT NULL REFERENCES policies(id),
    snapshot_id         BIGINT REFERENCES configuration_snapshots(id),
    compliance_score    DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    compliance_status   compliance_status NOT NULL,
    total_rules         INTEGER NOT NULL DEFAULT 0,
    passed_rules        INTEGER NOT NULL DEFAULT 0,
    failed_rules        INTEGER NOT NULL DEFAULT 0,
    critical_violations INTEGER NOT NULL DEFAULT 0,
    high_violations     INTEGER NOT NULL DEFAULT 0,
    medium_violations   INTEGER NOT NULL DEFAULT 0,
    low_violations      INTEGER NOT NULL DEFAULT 0,
    triggered_by        VARCHAR(50) DEFAULT 'MANUAL',
    scanned_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: compliance_results (per-rule results)
-- =====================================================================

CREATE TABLE compliance_results (
    id              BIGSERIAL PRIMARY KEY,
    scan_id         BIGINT NOT NULL REFERENCES compliance_scans(id) ON DELETE CASCADE,
    rule_id         BIGINT NOT NULL REFERENCES policy_rules(id),
    parameter       VARCHAR(100) NOT NULL,
    expected_value  VARCHAR(255) NOT NULL,
    actual_value    VARCHAR(255),
    passed          BOOLEAN NOT NULL DEFAULT FALSE,
    severity        severity NOT NULL,
    description     TEXT,
    remediation     TEXT
);

-- =====================================================================
-- TABLE: drift_events
-- =====================================================================

CREATE TABLE drift_events (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    scan_id         BIGINT NOT NULL REFERENCES compliance_scans(id),
    policy_id       BIGINT NOT NULL REFERENCES policies(id),
    parameter       VARCHAR(100) NOT NULL,
    expected_value  VARCHAR(255) NOT NULL,
    actual_value    VARCHAR(255),
    severity        severity NOT NULL,
    status          drift_status NOT NULL DEFAULT 'OPEN',
    description     TEXT,
    remediation     TEXT,
    detected_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP,
    resolved_by     BIGINT REFERENCES users(id)
);

-- =====================================================================
-- TABLE: ai_analyses
-- =====================================================================

CREATE TABLE ai_analyses (
    id              BIGSERIAL PRIMARY KEY,
    scan_id         BIGINT NOT NULL REFERENCES compliance_scans(id) ON DELETE CASCADE,
    device_id       BIGINT NOT NULL REFERENCES devices(id),
    summary         TEXT,
    analysis        TEXT,
    recommendations JSONB,
    priority        VARCHAR(20),
    model_used      VARCHAR(100),
    prompt_tokens   INTEGER,
    completion_tokens INTEGER,
    ai_available    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- TABLE: alerts
-- =====================================================================

CREATE TABLE alerts (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    scan_id         BIGINT REFERENCES compliance_scans(id),
    drift_event_id  BIGINT REFERENCES drift_events(id),
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    severity        severity NOT NULL,
    status          alert_status NOT NULL DEFAULT 'OPEN',
    detected_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMP,
    acknowledged_by BIGINT REFERENCES users(id)
);

-- =====================================================================
-- INDEXES
-- =====================================================================

CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_type ON devices(device_type);
CREATE INDEX idx_devices_policy ON devices(applied_policy_id);
CREATE INDEX idx_config_snapshots_device ON configuration_snapshots(device_id);
CREATE INDEX idx_config_snapshots_hash ON configuration_snapshots(config_hash);
CREATE INDEX idx_compliance_scans_device ON compliance_scans(device_id);
CREATE INDEX idx_compliance_scans_scanned_at ON compliance_scans(scanned_at DESC);
CREATE INDEX idx_compliance_results_scan ON compliance_results(scan_id);
CREATE INDEX idx_drift_events_device ON drift_events(device_id);
CREATE INDEX idx_drift_events_status ON drift_events(status);
CREATE INDEX idx_drift_events_severity ON drift_events(severity);
CREATE INDEX idx_drift_events_detected_at ON drift_events(detected_at DESC);
CREATE INDEX idx_alerts_device ON alerts(device_id);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_policy_rules_policy ON policy_rules(policy_id);
