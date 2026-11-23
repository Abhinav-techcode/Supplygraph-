-- V17: persisted agent investigations (auditable evidence per agent per incident) and recorded outcomes used to calibrate agent confidence
CREATE TABLE IF NOT EXISTS agent_investigations (
    investigation_id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(128) NOT NULL,
    agent_name VARCHAR(128) NOT NULL,
    role VARCHAR(64) NOT NULL,
    target_entity VARCHAR(256),
    tools_invoked INT NOT NULL,
    intents_generated INT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    outcome VARCHAR(64) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agent_investigations_ws ON agent_investigations (workspace_id, created_at);

CREATE TABLE IF NOT EXISTS agent_outcomes (
    outcome_id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    success BOOLEAN NOT NULL,
    note VARCHAR(512),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agent_outcomes_ws_role ON agent_outcomes (workspace_id, role);
