# SupplyGraph — Database Setup & Flyway Migration Guide

**Document ID:** `SG-DOC-004`  
**Phase:** Phase 1 (Foundation Baseline)  
**Status:** Official Database Specification  

---

## 1. Database Architecture & Principles

- **PostgreSQL 16+** is the sole transactional source of truth for SupplyGraph.
- **Flyway** manages all relational database schema migrations. Manual database mutations (`CREATE TABLE`, `ALTER TABLE`) are strictly prohibited in all environments.
- **Optimistic Locking:** Entity models employ `@Version` or monotonic version columns (`state_version`) to prevent lost updates without heavy pessimistic database locks.
- **Clean Adapter Port:** Database persistence is encapsulated strictly within `supplygraph-adapter-persistence`. Domain code interacts exclusively through repository interfaces.

---

## 2. Baseline Schema: `V1__init_schema.sql`

Located in `supplygraph-adapter-persistence/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS workspaces (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    industry VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workspaces_industry ON workspaces(industry);

CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(128) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_workspace ON audit_events(workspace_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_events(created_at);
```

---

## 3. Local PostgreSQL Setup (Docker Compose)

The project includes a root `docker-compose.yml` configured for local PostgreSQL 16:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: supplygraph-postgres
    environment:
      POSTGRES_DB: supplygraph
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d supplygraph"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### Starting the Database:
```bash
docker compose up -d
```

### Checking Database Health:
```bash
docker compose ps
# Expected state: Up (healthy)
```

---

## 4. Integration Testing with Testcontainers

Integration tests in `supplygraph-adapter-persistence` (e.g. `FlywayMigrationIntegrationTest`) spin up disposable PostgreSQL 16 containers via Testcontainers. Tests execute real migrations against an empty database and verify table creation without manual database provisioning.



---

## 5. Phase 2 Schema: `V2__digital_twin_schema.sql`

Located in `supplygraph-adapter-persistence/src/main/resources/db/migration/V2__digital_twin_schema.sql`:

Establishes the relational schema for the digital twin graph, materials catalog, BOM DAG, double-entry inventory ledger, and capacity bookings:

- `digital_twin_states`: Stores root aggregate versions and timestamps.
- `supply_chain_nodes`: Stores supplier, manufacturing site, warehouse, and logistics hub topologies.
- `transportation_lanes`: Directed transportation edges with transit durations and freight costs.
- `materials`: Material catalog with SKUs and unit definitions.
- `bom_entries`: BOM relationships linking parents to components with scrap rates.
- `inventory_balances`: Account balances by node, material, category, and lot.
- `inventory_transactions`: Atomic double-entry transaction journal.
- `inventory_postings`: Individual balanced credit/debit legs.
