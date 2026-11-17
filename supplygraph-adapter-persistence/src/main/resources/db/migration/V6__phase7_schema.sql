-- ==============================================================================
-- V6: Phase 7 Enterprise Data Integration, Quality Engine & Supply Chain Intelligence
-- ==============================================================================

-- 1. Enterprise Data Import Jobs
CREATE TABLE IF NOT EXISTS import_jobs (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    filename VARCHAR(255),
    file_checksum VARCHAR(128),
    schema_version VARCHAR(32),
    total_records INT NOT NULL,
    accepted_records INT NOT NULL,
    rejected_records INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    imported_by VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_workspace ON import_jobs(workspace_id, created_at DESC);

-- 2. Import Record Failures
CREATE TABLE IF NOT EXISTS import_job_failures (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_index INT NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    rejected_value TEXT,
    error_code VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    suggested_correction TEXT
);

CREATE INDEX IF NOT EXISTS idx_import_failures_job ON import_job_failures(job_id);

-- 3. Data Quality Assessment Reports
CREATE TABLE IF NOT EXISTS data_quality_reports (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_score NUMERIC(5, 2) NOT NULL,
    total_entities INT NOT NULL,
    clean_records INT NOT NULL,
    flawed_records INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quality_reports_workspace ON data_quality_reports(workspace_id, evaluated_at DESC);

-- 4. Data Quality Findings
CREATE TABLE IF NOT EXISTS data_quality_findings (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(64) NOT NULL REFERENCES data_quality_reports(id) ON DELETE CASCADE,
    rule_id VARCHAR(64) NOT NULL,
    dimension VARCHAR(32) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    actual_value TEXT,
    message TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    suggested_remediation TEXT
);

CREATE INDEX IF NOT EXISTS idx_quality_findings_report ON data_quality_findings(report_id);

-- 5. Supplier Risk Profiles
CREATE TABLE IF NOT EXISTS supplier_risk_profiles (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    supplier_id VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    tier INT NOT NULL,
    country VARCHAR(64),
    overall_risk_score NUMERIC(5, 2) NOT NULL,
    is_single_source BOOLEAN NOT NULL,
    downstream_sites_impacted INT NOT NULL,
    alternative_suppliers_count INT NOT NULL,
    qualification_status VARCHAR(64),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_supplier_profiles_ws ON supplier_risk_profiles(workspace_id);

-- 6. Material Intelligence Profiles
CREATE TABLE IF NOT EXISTS material_intelligence_profiles (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    criticality VARCHAR(32) NOT NULL,
    single_source_supplier_id VARCHAR(64),
    lead_time_days INT NOT NULL,
    on_hand_quantity NUMERIC(19, 4) NOT NULL,
    available_quantity NUMERIC(19, 4) NOT NULL,
    reserved_quantity NUMERIC(19, 4) NOT NULL,
    quarantined_quantity NUMERIC(19, 4) NOT NULL,
    in_transit_quantity NUMERIC(19, 4) NOT NULL,
    daily_consumption_rate NUMERIC(19, 4) NOT NULL,
    inventory_runway_days NUMERIC(10, 2) NOT NULL,
    expiry_risk_batch_count INT NOT NULL,
    downstream_product_count INT NOT NULL,
    industry_classification VARCHAR(64),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_material_profiles_ws ON material_intelligence_profiles(workspace_id);

-- 7. Network Resilience Assessments
CREATE TABLE IF NOT EXISTS network_resilience_assessments (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    assessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_resilience_index NUMERIC(5, 2) NOT NULL,
    spof_count INT NOT NULL,
    supplier_concentration_hhi NUMERIC(10, 2) NOT NULL,
    critical_path_depth INT NOT NULL,
    alt_coverage_ratio NUMERIC(5, 4) NOT NULL,
    bottlenecks_count INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_resilience_workspace ON network_resilience_assessments(workspace_id, assessed_at DESC);
