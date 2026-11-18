-- V9: Phase 9 connector execution history
CREATE TABLE IF NOT EXISTS connector_executions (
    id VARCHAR(64) PRIMARY KEY,
    connector_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    records_received INT NOT NULL DEFAULT 0,
    records_processed INT NOT NULL DEFAULT 0,
    records_rejected INT NOT NULL DEFAULT 0,
    error_category VARCHAR(32),
    error_message TEXT,
    attempt INT NOT NULL DEFAULT 1,
    retry_of VARCHAR(64),
    correlation_id VARCHAR(64) NOT NULL,
    simulated BOOLEAN NOT NULL,
    requested_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_conn_exec_connector ON connector_executions(connector_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_conn_exec_workspace ON connector_executions(workspace_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_conn_exec_retry ON connector_executions(retry_of);
