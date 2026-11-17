-- ==============================================================================
-- SupplyGraph Phase 6 Schema: Enterprise Security, Governance, Provenance, and Research Benchmarks
-- ==============================================================================

-- 1. Enterprise User Profiles and RBAC
CREATE TABLE IF NOT EXISTS user_profiles (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    assigned_workspaces VARCHAR(1024) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_username ON user_profiles (username);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles (role);

-- 2. Immutable, Append-Only Governance Audit Logs with Cryptographic Signatures
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    previous_state_version VARCHAR(32),
    new_state_version VARCHAR(32),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    rationale TEXT,
    concurguard_conflict_ref VARCHAR(128),
    signature_hash VARCHAR(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_time ON audit_logs (workspace_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs (target_entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);

-- 3. Data Provenance and Asset Classification
CREATE TABLE IF NOT EXISTS provenance_records (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    provenance_type VARCHAR(64) NOT NULL,
    source_reference VARCHAR(256) NOT NULL,
    state_version VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    attributes_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_provenance_workspace_entity ON provenance_records (workspace_id, entity_id);
CREATE INDEX IF NOT EXISTS idx_provenance_type ON provenance_records (provenance_type);

-- 4. Reproducible Research Benchmark Trials
CREATE TABLE IF NOT EXISTS benchmark_experiments (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    scenario VARCHAR(64) NOT NULL,
    baseline VARCHAR(64) NOT NULL,
    dataset_reference VARCHAR(128) NOT NULL,
    twin_version VARCHAR(32) NOT NULL,
    random_seed BIGINT NOT NULL,
    trial_runs INT NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metrics_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    limitations_notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_benchmark_scenario_baseline ON benchmark_experiments (scenario, baseline);
