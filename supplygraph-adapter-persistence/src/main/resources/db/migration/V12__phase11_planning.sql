-- Flyway Migration V12: Advanced Supply Chain Planning & Multi-Echelon Optimization (Phase 11)

CREATE TABLE sg_demand_forecasts (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    destination_node_id VARCHAR(64) NOT NULL,
    method VARCHAR(64) NOT NULL,
    forecasted_quantities_json TEXT NOT NULL,
    historical_baseline_json TEXT NOT NULL,
    period_overrides_json TEXT,
    mad DOUBLE PRECISION NOT NULL,
    mape DOUBLE PRECISION NOT NULL,
    rmse DOUBLE PRECISION NOT NULL,
    tracking_signal DOUBLE PRECISION NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_demand_forecasts_ws ON sg_demand_forecasts (workspace_id);
CREATE INDEX idx_demand_forecasts_mat ON sg_demand_forecasts (workspace_id, material_id);

CREATE TABLE sg_inventory_plans (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    target_service_level VARCHAR(32) NOT NULL,
    positions_json TEXT NOT NULL,
    total_carrying_cost DOUBLE PRECISION NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_inventory_plans_ws ON sg_inventory_plans (workspace_id);

CREATE TABLE sg_procurement_plans (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    net_requirements_json TEXT NOT NULL,
    supplier_allocations_json TEXT NOT NULL,
    total_projected_cost NUMERIC(19, 4) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_procurement_plans_ws ON sg_procurement_plans (workspace_id);

CREATE TABLE sg_production_schedules (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    plant_id VARCHAR(64) NOT NULL,
    schedule_name VARCHAR(255) NOT NULL,
    scheduled_jobs_json TEXT NOT NULL,
    average_capacity_utilization DOUBLE PRECISION NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_production_schedules_ws ON sg_production_schedules (workspace_id);

CREATE TABLE sg_planning_scenarios (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    scenario_name VARCHAR(255) NOT NULL,
    scenario_type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    total_projected_cost NUMERIC(19, 4) NOT NULL,
    customer_service_fill_rate DOUBLE PRECISION NOT NULL,
    average_lead_time_days DOUBLE PRECISION NOT NULL,
    carbon_footprint_co2_tons DOUBLE PRECISION NOT NULL,
    evaluated_by VARCHAR(64) NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_planning_scenarios_ws ON sg_planning_scenarios (workspace_id);

