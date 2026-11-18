-- Flyway Migration V13: Sustainability, Carbon Intelligence & Traceability (Phase 12)

CREATE TABLE sg_carbon_emission_records (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    facility_or_lane_id VARCHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    activity_category VARCHAR(255) NOT NULL,
    activity_amount DOUBLE PRECISION NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL,
    emission_factor_kg_co2e DOUBLE PRECISION NOT NULL,
    total_emissions_kg_co2e DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_carbon_emissions_ws ON sg_carbon_emission_records (workspace_id);
CREATE INDEX idx_carbon_emissions_scope ON sg_carbon_emission_records (workspace_id, scope);

CREATE TABLE sg_genealogy_dags (
    workspace_id VARCHAR(64) PRIMARY KEY,
    nodes_json TEXT NOT NULL,
    edges_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sg_traceability_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    batch_number VARCHAR(128) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    location_id VARCHAR(64) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL,
    custody_holder VARCHAR(255) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_traceability_nodes_ws ON sg_traceability_nodes (workspace_id);
CREATE INDEX idx_traceability_nodes_batch ON sg_traceability_nodes (batch_number);

CREATE TABLE sg_supplier_esg_scorecards (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    supplier_id VARCHAR(64) NOT NULL,
    environmental_score DOUBLE PRECISION NOT NULL,
    social_score DOUBLE PRECISION NOT NULL,
    governance_score DOUBLE PRECISION NOT NULL,
    composite_esg_score DOUBLE PRECISION NOT NULL,
    cbam_compliant BOOLEAN NOT NULL,
    conflict_minerals_certified BOOLEAN NOT NULL,
    eudr_compliant BOOLEAN NOT NULL,
    audited_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_supplier_esg_ws ON sg_supplier_esg_scorecards (workspace_id);
CREATE INDEX idx_supplier_esg_supp ON sg_supplier_esg_scorecards (workspace_id, supplier_id);

CREATE TABLE sg_circular_ledger_entries (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    flow_type VARCHAR(64) NOT NULL,
    intake_quantity DOUBLE PRECISION NOT NULL,
    recovery_yield_percentage DOUBLE PRECISION NOT NULL,
    recovered_quantity DOUBLE PRECISION NOT NULL,
    virgin_material_avoided_kg DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_circular_ledger_ws ON sg_circular_ledger_entries (workspace_id);

CREATE TABLE sg_environmental_scenarios (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    scenario_name VARCHAR(255) NOT NULL,
    baseline_co2_tons DOUBLE PRECISION NOT NULL,
    projected_co2_tons DOUBLE PRECISION NOT NULL,
    reduction_percentage DOUBLE PRECISION NOT NULL,
    total_abatement_cost_usd NUMERIC(19, 4) NOT NULL,
    cost_per_ton_co2_avoided_usd NUMERIC(19, 4) NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_environmental_scenarios_ws ON sg_environmental_scenarios (workspace_id);

