-- ==============================================================================
-- V7: Phase 8 Advanced Decision Intelligence, Explainable AI & Provenance Graph
-- ==============================================================================

-- 1. Explainable Recovery Recommendations
CREATE TABLE IF NOT EXISTS explainable_recommendations (
    id VARCHAR(64) PRIMARY KEY,
    disruption_id VARCHAR(64) NOT NULL,
    state_version VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    primary_actions TEXT NOT NULL,
    expected_cost_usd NUMERIC(19, 4) NOT NULL,
    expected_recovery_time_hours DOUBLE PRECISION NOT NULL,
    service_restoration_pct DOUBLE PRECISION NOT NULL,
    evidence_json TEXT,
    constraints_json TEXT,
    tradeoffs_json TEXT,
    uncertainty_json TEXT,
    requires_human_approval BOOLEAN NOT NULL DEFAULT TRUE,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(64),
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_recommendations_disruption ON explainable_recommendations(disruption_id);

-- 2. Decision Provenance Graph Nodes
CREATE TABLE IF NOT EXISTS decision_provenance_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    label VARCHAR(255) NOT NULL,
    entity_reference_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_dpn_workspace_disr ON decision_provenance_nodes(workspace_id, disruption_id);

-- 3. Decision Provenance Graph Edges
CREATE TABLE IF NOT EXISTS decision_provenance_edges (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(64) NOT NULL,
    source_node_id VARCHAR(64) NOT NULL REFERENCES decision_provenance_nodes(id) ON DELETE CASCADE,
    target_node_id VARCHAR(64) NOT NULL REFERENCES decision_provenance_nodes(id) ON DELETE CASCADE,
    edge_type VARCHAR(32) NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dpe_workspace_disr ON decision_provenance_edges(workspace_id, disruption_id);

-- 4. What-If Scenario Evaluations
CREATE TABLE IF NOT EXISTS what_if_experiments (
    id VARCHAR(64) PRIMARY KEY,
    scenario_id VARCHAR(64) NOT NULL,
    scenario_name VARCHAR(255) NOT NULL,
    baseline_cost_usd NUMERIC(19, 4) NOT NULL,
    scenario_cost_usd NUMERIC(19, 4) NOT NULL,
    baseline_ttr_days DOUBLE PRECISION NOT NULL,
    scenario_ttr_days DOUBLE PRECISION NOT NULL,
    baseline_service_pct DOUBLE PRECISION NOT NULL,
    scenario_service_pct DOUBLE PRECISION NOT NULL,
    baseline_resilience_idx DOUBLE PRECISION NOT NULL,
    scenario_resilience_idx DOUBLE PRECISION NOT NULL,
    cost_variance_pct DOUBLE PRECISION NOT NULL,
    recovery_time_delta_days DOUBLE PRECISION NOT NULL,
    variance_summary TEXT NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_what_if_scenario ON what_if_experiments(scenario_id, evaluated_at DESC);

-- 5. Multi-Objective Pareto Evaluations
CREATE TABLE IF NOT EXISTS pareto_evaluations (
    id VARCHAR(64) PRIMARY KEY,
    disruption_id VARCHAR(64) NOT NULL,
    all_solutions_json TEXT NOT NULL,
    frontier_solutions_json TEXT NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pareto_disruption ON pareto_evaluations(disruption_id, evaluated_at DESC);

-- 6. Sensitivity Analysis Reports
CREATE TABLE IF NOT EXISTS sensitivity_analysis_reports (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    alternative_id VARCHAR(64) NOT NULL,
    baseline_cost_usd NUMERIC(19, 4) NOT NULL,
    baseline_recovery_days DOUBLE PRECISION NOT NULL,
    baseline_service_pct DOUBLE PRECISION NOT NULL,
    trials_json TEXT NOT NULL,
    overall_robustness_score DOUBLE PRECISION NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sensitivity_workspace ON sensitivity_analysis_reports(workspace_id, generated_at DESC);

-- 7. Advanced Resilience Metrics
CREATE TABLE IF NOT EXISTS advanced_resilience_metrics (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    time_to_survive_days DOUBLE PRECISION NOT NULL,
    time_to_recover_days DOUBLE PRECISION NOT NULL,
    supplier_exposure_idx DOUBLE PRECISION NOT NULL,
    recovery_flexibility_idx DOUBLE PRECISION NOT NULL,
    bottleneck_node_count INT NOT NULL,
    composite_resilience_score DOUBLE PRECISION NOT NULL,
    breakdown_json TEXT,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_adv_resilience_workspace ON advanced_resilience_metrics(workspace_id, calculated_at DESC);
