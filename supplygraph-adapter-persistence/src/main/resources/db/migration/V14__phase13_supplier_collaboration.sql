-- =============================================================================
-- SupplyGraph Phase 13: Supplier Collaboration & Partner Ecosystem
-- =============================================================================

CREATE TABLE IF NOT EXISTS sg_partner_organizations (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    partner_type VARCHAR(64) NOT NULL,
    partner_tier VARCHAR(64) NOT NULL,
    duns_number VARCHAR(64),
    country_code VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    onboarded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_partner_ws ON sg_partner_organizations(workspace_id);

CREATE TABLE IF NOT EXISTS sg_rfq_documents (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    target_material_id VARCHAR(64) NOT NULL,
    target_quantity NUMERIC(18, 4) NOT NULL,
    submission_deadline DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    quotations_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_rfq_ws ON sg_rfq_documents(workspace_id);

CREATE TABLE IF NOT EXISTS sg_rfq_quotations (
    id VARCHAR(64) PRIMARY KEY,
    rfq_id VARCHAR(64) NOT NULL,
    supplier_id VARCHAR(64) NOT NULL,
    unit_price_usd NUMERIC(18, 4) NOT NULL,
    lead_time_days INT NOT NULL,
    min_order_quantity NUMERIC(18, 4) NOT NULL,
    validity_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_quote_rfq ON sg_rfq_quotations(rfq_id);

CREATE TABLE IF NOT EXISTS sg_po_collaboration_records (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    po_number VARCHAR(64) NOT NULL,
    supplier_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    order_quantity NUMERIC(18, 4) NOT NULL,
    total_amount_usd NUMERIC(18, 4) NOT NULL,
    promised_delivery_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_po_ws ON sg_po_collaboration_records(workspace_id);

CREATE TABLE IF NOT EXISTS sg_advance_shipping_notices (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    po_id VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(128) NOT NULL,
    carrier_name VARCHAR(128) NOT NULL,
    shipped_quantity NUMERIC(18, 4) NOT NULL,
    estimated_arrival_date DATE NOT NULL,
    dispatched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_asn_ws ON sg_advance_shipping_notices(workspace_id);
CREATE INDEX IF NOT EXISTS idx_sg_asn_po ON sg_advance_shipping_notices(po_id);

CREATE TABLE IF NOT EXISTS sg_quality_incidents_8d (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    supplier_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    defect_title VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    current_step VARCHAR(64) NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_quality_8d_ws ON sg_quality_incidents_8d(workspace_id);

CREATE TABLE IF NOT EXISTS sg_partner_documents (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    partner_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    cryptographic_hash VARCHAR(128) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sg_partner_docs_ws ON sg_partner_documents(workspace_id);
CREATE INDEX IF NOT EXISTS idx_sg_partner_docs_partner ON sg_partner_documents(partner_id);

