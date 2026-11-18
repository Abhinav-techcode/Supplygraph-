-- V8: Phase 9 Enterprise Integration Runtime (connector registry, transactional outbox, inbox dedup)
CREATE TABLE IF NOT EXISTS connectors (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    adapter_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    base_uri VARCHAR(512),
    credential_ref VARCHAR(128),
    allowed_hosts TEXT NOT NULL DEFAULT '',
    timeout_millis INT,
    max_response_bytes BIGINT,
    max_retries INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_connectors_workspace ON connectors(workspace_id, created_at DESC);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    aggregate_id VARCHAR(128),
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    replay_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_outbox_idempotency UNIQUE (workspace_id, idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_outbox_due ON outbox_events(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbox_workspace ON outbox_events(workspace_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS inbox_messages (
    workspace_id VARCHAR(64) NOT NULL,
    consumer VARCHAR(96) NOT NULL,
    message_id VARCHAR(160) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (workspace_id, consumer, message_id)
);
