-- V11: Phase 10 governed execution (workflows, tasks, approvals, attempts, evidence, timeline, policies, outcomes, reassessments)
CREATE TABLE IF NOT EXISTS execution_workflows (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(64),
    plan_approved_by VARCHAR(128),
    source_twin_version VARCHAR(64),
    simulated BOOLEAN NOT NULL,
    replan_depth INT NOT NULL DEFAULT 0,
    status_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exec_wf_ws ON execution_workflows(workspace_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_exec_wf_plan ON execution_workflows(workspace_id, plan_id);
CREATE INDEX IF NOT EXISTS idx_exec_wf_status ON execution_workflows(status);

CREATE TABLE IF NOT EXISTS execution_tasks (
    id VARCHAR(80) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES execution_workflows(id) ON DELETE CASCADE,
    sequence_no INT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    description TEXT,
    parameters TEXT,
    status VARCHAR(32) NOT NULL,
    depends_on TEXT,
    condition_task_id VARCHAR(80),
    condition_outcome VARCHAR(32),
    attempts INT NOT NULL,
    max_attempts INT NOT NULL,
    expected_outcome TEXT,
    last_error TEXT,
    external_reference VARCHAR(255),
    simulated_evidence BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exec_task_wf ON execution_tasks(workflow_id, sequence_no);
CREATE INDEX IF NOT EXISTS idx_exec_task_status ON execution_tasks(status, updated_at);

CREATE TABLE IF NOT EXISTS execution_approvals (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES execution_workflows(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(64),
    scope VARCHAR(32) NOT NULL,
    approved BOOLEAN NOT NULL,
    rationale TEXT NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exec_appr_wf ON execution_approvals(workflow_id);

CREATE TABLE IF NOT EXISTS execution_attempts (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(80) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    attempt_no INT NOT NULL,
    adapter_id VARCHAR(96) NOT NULL,
    simulated BOOLEAN NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    external_reference VARCHAR(255),
    message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    requested_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_exec_attempt_task ON execution_attempts(task_id, attempt_no);
CREATE INDEX IF NOT EXISTS idx_exec_attempt_wf ON execution_attempts(workflow_id);

CREATE TABLE IF NOT EXISTS execution_evidence (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(80) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    simulated BOOLEAN NOT NULL,
    source VARCHAR(255),
    reference VARCHAR(255),
    expected TEXT,
    observed TEXT,
    note TEXT,
    recorded_by VARCHAR(64),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exec_evid_task ON execution_evidence(task_id, kind);
CREATE INDEX IF NOT EXISTS idx_exec_evid_wf ON execution_evidence(workflow_id);

CREATE TABLE IF NOT EXISTS execution_events (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(80),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64),
    detail TEXT,
    simulated BOOLEAN NOT NULL,
    correlation_id VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exec_events_wf ON execution_events(workflow_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_exec_events_ws ON execution_events(workspace_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS execution_policies (
    workspace_id VARCHAR(64) PRIMARY KEY,
    approver_differs_from_creator BOOLEAN NOT NULL,
    executor_differs_from_approver BOOLEAN NOT NULL,
    sensitive_second_approval BOOLEAN NOT NULL,
    allow_live_execution BOOLEAN NOT NULL,
    max_task_attempts INT NOT NULL,
    max_replan_depth INT NOT NULL,
    deviation_threshold_percent DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS plan_actuals (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    metric VARCHAR(48) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    source VARCHAR(255) NOT NULL,
    simulated BOOLEAN NOT NULL,
    recorded_by VARCHAR(64),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_plan_actuals_wf ON plan_actuals(workflow_id, recorded_at);

CREATE TABLE IF NOT EXISTS reassessment_requests (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    depth INT NOT NULL,
    deviation_percent DOUBLE PRECISION NOT NULL,
    reason TEXT,
    status VARCHAR(16) NOT NULL,
    proposed_by VARCHAR(64),
    decided_by VARCHAR(64),
    decision_rationale TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_reassess_wf ON reassessment_requests(workflow_id);
CREATE INDEX IF NOT EXISTS idx_reassess_ws ON reassessment_requests(workspace_id, created_at DESC);
