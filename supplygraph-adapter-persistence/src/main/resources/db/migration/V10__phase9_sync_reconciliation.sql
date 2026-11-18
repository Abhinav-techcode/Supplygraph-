-- V10: Phase 9 synchronization checkpoints, integration field mirror, reconciliation items, field authority overrides
CREATE TABLE IF NOT EXISTS sync_checkpoints (
    connector_id VARCHAR(64) PRIMARY KEY,
    cursor_value VARCHAR(255),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    sync_count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS field_mirrors (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    connector_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    internal_value TEXT,
    last_synced_value TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_field_mirror UNIQUE (workspace_id, connector_id, entity_type, entity_key, field_name)
);
CREATE INDEX IF NOT EXISTS idx_field_mirrors_ws ON field_mirrors(workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS reconciliation_items (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    connector_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    external_value TEXT,
    internal_value TEXT,
    last_synced_value TEXT,
    status VARCHAR(24) NOT NULL,
    resolved_by VARCHAR(64),
    resolution_rationale TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    correlation_id VARCHAR(64),
    simulated BOOLEAN NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recon_ws_status ON reconciliation_items(workspace_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS field_authority_policies (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    authority VARCHAR(16) NOT NULL,
    CONSTRAINT uq_field_policy UNIQUE (workspace_id, entity_type, field_name)
);
