-- ==============================================================================
-- SupplyGraph Phase 5 Schema Migration: Continuous Intelligence & Adaptive Replanning
-- ==============================================================================

-- 1. Operational Events Table
CREATE TABLE IF NOT EXISTS operational_events (
    id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    source VARCHAR(64) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    received_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    affected_entity_id VARCHAR(128) NOT NULL,
    affected_entity_type VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    payload_json TEXT NOT NULL DEFAULT '{}',
    provenance_json TEXT NOT NULL DEFAULT '{}',
    schema_version INT NOT NULL DEFAULT 1,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'INGESTED',
    rejection_reason VARCHAR(64) NOT NULL DEFAULT 'NONE'
);

CREATE INDEX IF NOT EXISTS idx_op_events_ws_time ON operational_events(workspace_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_op_events_entity ON operational_events(affected_entity_id, affected_entity_type);

-- 2. Monitoring Rules Table
CREATE TABLE IF NOT EXISTS monitoring_rules (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    industry_applicability VARCHAR(64) NOT NULL DEFAULT 'ALL',
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    threshold NUMERIC(19, 4) NOT NULL DEFAULT 0.0,
    trigger_type VARCHAR(32) NOT NULL DEFAULT 'ON_EVENT',
    condition_type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_rules_industry ON monitoring_rules(industry_applicability);

-- 3. Operational Risk Alerts Table
CREATE TABLE IF NOT EXISTS risk_alerts (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    rule_id VARCHAR(64) NOT NULL REFERENCES monitoring_rules(id) ON DELETE CASCADE,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    affected_entity_id VARCHAR(128) NOT NULL,
    affected_entity_type VARCHAR(64) NOT NULL,
    trigger_event_id VARCHAR(64) REFERENCES operational_events(id) ON DELETE SET NULL,
    source_state_version BIGINT NOT NULL,
    evidence_json TEXT NOT NULL DEFAULT '{}',
    suppression_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_alerts_ws_status ON risk_alerts(workspace_id, status);
CREATE INDEX IF NOT EXISTS idx_alerts_suppression ON risk_alerts(suppression_key, status);

-- 4. Impact Reassessments Table
CREATE TABLE IF NOT EXISTS impact_reassessments (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    trigger_event_id VARCHAR(64) REFERENCES operational_events(id) ON DELETE SET NULL,
    previous_version BIGINT NOT NULL,
    new_version BIGINT NOT NULL,
    affected_node_ids TEXT NOT NULL DEFAULT '[]',
    affected_lane_ids TEXT NOT NULL DEFAULT '[]',
    affected_material_ids TEXT NOT NULL DEFAULT '[]',
    preserved_scope_json TEXT NOT NULL DEFAULT '[]',
    invalidated_scope_json TEXT NOT NULL DEFAULT '[]',
    affected_assessment_ids TEXT NOT NULL DEFAULT '[]',
    affected_plan_ids TEXT NOT NULL DEFAULT '[]',
    reassessed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reassess_ws_time ON impact_reassessments(workspace_id, reassessed_at DESC);

-- 5. Replanning Runs Table
CREATE TABLE IF NOT EXISTS replanning_runs (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    trigger_alert_id VARCHAR(64) REFERENCES risk_alerts(id) ON DELETE SET NULL,
    superseded_plan_id VARCHAR(64) REFERENCES recovery_plans(id) ON DELETE SET NULL,
    revised_plan_id VARCHAR(64) REFERENCES recovery_plans(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL,
    affected_agent_ids TEXT NOT NULL DEFAULT '[]',
    preserved_action_ids TEXT NOT NULL DEFAULT '[]',
    reconsidered_action_ids TEXT NOT NULL DEFAULT '[]',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    reason TEXT NOT NULL,
    audit_trail_json TEXT NOT NULL DEFAULT '[]',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_replanning_ws_time ON replanning_runs(workspace_id, started_at DESC);

