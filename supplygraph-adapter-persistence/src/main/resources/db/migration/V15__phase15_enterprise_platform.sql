-- =============================================================================
-- SupplyGraph Phase 15: Enterprise Platform Maturity & Administration
-- =============================================================================

CREATE TABLE IF NOT EXISTS sg_industry_policies (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    industry_code VARCHAR(64) NOT NULL,
    version_number INT NOT NULL,
    policy_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_policy_ws ON sg_industry_policies(workspace_id);
CREATE INDEX IF NOT EXISTS idx_sg_policy_ws_ind ON sg_industry_policies(workspace_id, industry_code);

CREATE TABLE IF NOT EXISTS sg_workspace_quotas (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL UNIQUE,
    max_nodes INT NOT NULL,
    max_active_disruptions INT NOT NULL,
    max_concurrent_simulations INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_quota_ws ON sg_workspace_quotas(workspace_id);

CREATE TABLE IF NOT EXISTS sg_api_keys (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    key_name VARCHAR(128) NOT NULL,
    hashed_token VARCHAR(128) NOT NULL UNIQUE,
    scopes_csv VARCHAR(256) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_apikey_ws ON sg_api_keys(workspace_id);
CREATE INDEX IF NOT EXISTS idx_sg_apikey_token ON sg_api_keys(hashed_token);

CREATE TABLE IF NOT EXISTS sg_backup_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    snapshot_name VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    storage_checksum VARCHAR(128) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_snapshot_ws ON sg_backup_snapshots(workspace_id);

