-- SupplyGraph Digital Twin & Graph Domain Schema
-- V2__digital_twin_schema.sql

CREATE TABLE IF NOT EXISTS digital_twin_states (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_twin_workspace FOREIGN KEY (id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS supply_chain_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    location VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_node_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nodes_workspace ON supply_chain_nodes(workspace_id);

CREATE TABLE IF NOT EXISTS transportation_lanes (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    source_node_id VARCHAR(64) NOT NULL,
    target_node_id VARCHAR(64) NOT NULL,
    transport_mode VARCHAR(32) NOT NULL,
    transit_duration_seconds BIGINT NOT NULL,
    cost_per_kg NUMERIC(15, 4) NOT NULL,
    is_operational BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_lane_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_lane_source FOREIGN KEY (source_node_id) REFERENCES supply_chain_nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_lane_target FOREIGN KEY (target_node_id) REFERENCES supply_chain_nodes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lanes_workspace ON transportation_lanes(workspace_id);
CREATE INDEX IF NOT EXISTS idx_lanes_source ON transportation_lanes(source_node_id);
CREATE INDEX IF NOT EXISTS idx_lanes_target ON transportation_lanes(target_node_id);

CREATE TABLE IF NOT EXISTS materials (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    material_type VARCHAR(64) NOT NULL,
    base_unit VARCHAR(32) NOT NULL,
    CONSTRAINT fk_material_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_materials_workspace ON materials(workspace_id);

CREATE TABLE IF NOT EXISTS bom_entries (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    parent_material_id VARCHAR(64) NOT NULL,
    child_material_id VARCHAR(64) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    scrap_rate NUMERIC(7, 4) NOT NULL DEFAULT 0.0,
    position INT NOT NULL,
    CONSTRAINT fk_bom_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_bom_parent FOREIGN KEY (parent_material_id) REFERENCES materials(id) ON DELETE CASCADE,
    CONSTRAINT fk_bom_child FOREIGN KEY (child_material_id) REFERENCES materials(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bom_workspace ON bom_entries(workspace_id);
CREATE INDEX IF NOT EXISTS idx_bom_parent ON bom_entries(parent_material_id);

CREATE TABLE IF NOT EXISTS inventory_balances (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    material_id VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    lot_number VARCHAR(128),
    balance_amount NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_balance_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_balance_workspace ON inventory_balances(workspace_id);
CREATE INDEX IF NOT EXISTS idx_balance_node_mat ON inventory_balances(node_id, material_id);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    reference_type VARCHAR(128) NOT NULL,
    reference_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tx_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tx_workspace ON inventory_transactions(workspace_id);

CREATE TABLE IF NOT EXISTS inventory_postings (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    posting_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    CONSTRAINT fk_posting_tx FOREIGN KEY (transaction_id) REFERENCES inventory_transactions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_posting_tx ON inventory_postings(transaction_id);

