-- SupplyGraph Phase 4 Schema Migration: Disruptions, Impact Assessments,
-- Simulation Runs, Monte Carlo Runs, Optimization Runs, and Recovery Plans.
-- V3__phase4_schema.sql

CREATE TABLE IF NOT EXISTS disruptions (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    industry_code VARCHAR(64) NOT NULL,
    scenario_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    affected_nodes_json TEXT,
    affected_lanes_json TEXT,
    affected_materials_json TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    expected_end_time TIMESTAMP WITH TIME ZONE,
    impact_params_json TEXT,
    source_evidence TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_disruption_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_disruptions_workspace ON disruptions(workspace_id);
CREATE INDEX IF NOT EXISTS idx_disruptions_status ON disruptions(status);

CREATE TABLE IF NOT EXISTS impact_assessments (
    id VARCHAR(64) PRIMARY KEY,
    disruption_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    digital_twin_version BIGINT NOT NULL,
    assessment_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_severity VARCHAR(32) NOT NULL,
    node_impacts_json TEXT,
    material_shortages_json TEXT,
    capacity_impacts_json TEXT,
    logistics_impacts_json TEXT,
    total_estimated_cost NUMERIC(19, 4) NOT NULL DEFAULT 0.0,
    risk_indicators_json TEXT,
    assumptions_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assessment_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_assessments_workspace ON impact_assessments(workspace_id);
CREATE INDEX IF NOT EXISTS idx_assessments_disruption ON impact_assessments(disruption_id);

CREATE TABLE IF NOT EXISTS simulation_runs (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64),
    scenario_id VARCHAR(64) NOT NULL,
    source_twin_version BIGINT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_event_count INT NOT NULL DEFAULT 0,
    service_level_percentage DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    total_shortage_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0.0,
    max_delay_seconds BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    warnings_json TEXT,
    assumptions_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_simulation_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_simulation_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_simulations_workspace ON simulation_runs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_simulations_disruption ON simulation_runs(disruption_id);

CREATE TABLE IF NOT EXISTS monte_carlo_runs (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64),
    seed BIGINT NOT NULL,
    replication_count INT NOT NULL,
    mean_delay_days DOUBLE PRECISION NOT NULL,
    median_delay_days DOUBLE PRECISION NOT NULL,
    p90_delay_days DOUBLE PRECISION NOT NULL,
    p95_delay_days DOUBLE PRECISION NOT NULL,
    mean_shortage_quantity NUMERIC(19, 4) NOT NULL,
    max_shortage_quantity NUMERIC(19, 4) NOT NULL,
    sla_violation_probability DOUBLE PRECISION NOT NULL,
    recovery_within_deadline_probability DOUBLE PRECISION NOT NULL,
    estimated_mean_cost NUMERIC(19, 4) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_montecarlo_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_montecarlo_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_montecarlo_workspace ON monte_carlo_runs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_montecarlo_disruption ON monte_carlo_runs(disruption_id);

CREATE TABLE IF NOT EXISTS optimization_runs (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    disruption_id VARCHAR(64) NOT NULL,
    solver_status VARCHAR(32) NOT NULL,
    objective_value DOUBLE PRECISION NOT NULL,
    unmet_demand_quantity NUMERIC(19, 4) NOT NULL,
    expected_delay_days DOUBLE PRECISION NOT NULL,
    estimated_cost NUMERIC(19, 4) NOT NULL,
    solver_runtime_millis BIGINT NOT NULL,
    decisions_json TEXT,
    diagnostics_json TEXT,
    explanations_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_optimization_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_optimization_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_optimization_workspace ON optimization_runs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_optimization_disruption ON optimization_runs(disruption_id);

CREATE TABLE IF NOT EXISTS recovery_plans (
    id VARCHAR(64) PRIMARY KEY,
    disruption_id VARCHAR(64) NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    source_twin_version BIGINT NOT NULL,
    impact_assessment_id VARCHAR(64),
    simulation_run_id VARCHAR(64),
    monte_carlo_run_id VARCHAR(64),
    optimization_run_id VARCHAR(64),
    plan_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    actions_json TEXT,
    metrics_json TEXT,
    concurguard_validated BOOLEAN NOT NULL DEFAULT FALSE,
    concurguard_conflicts_json TEXT,
    approval_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_plan_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_plans_workspace ON recovery_plans(workspace_id);
CREATE INDEX IF NOT EXISTS idx_plans_disruption ON recovery_plans(disruption_id);
CREATE INDEX IF NOT EXISTS idx_plans_status ON recovery_plans(status);

