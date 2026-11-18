# SupplyGraph — Local Interactive Demonstration Guide
## Comprehensive Operator & Evaluator Manual for Local Exploration

**Application Local URL:** [http://localhost:8080/](http://localhost:8080/)  
**Database URL:** `jdbc:postgresql://localhost:5433/supplygraph`  
**Default Demo Workspace:** `WS-AUTO-01` ("Automotive Powertrain & Electronics Global Network")  
**Target Platform:** Java 21 LTS, Spring Boot 3.4.1, Vaadin Flow 24.5.4, PostgreSQL 16+  

---

## 1. Quickstart & Service Management

### 1.1 Start Application
```bash
# 1. Ensure PostgreSQL container is running on port 5433
docker compose up -d postgres

# 2. Launch Spring Boot composition root with Demo Role Switcher enabled
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export SUPPLYGRAPH_UI_DEMO_ROLE_SWITCHER=true
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/supplygraph

./mvnw spring-boot:run -pl supplygraph-web
```
The application will launch on `http://localhost:8080/`.

### 1.2 Stop Application
- If running in terminal foreground: press `Ctrl + C`.
- If running as background process:
```bash
pkill -f "supplygraph-web"
```

### 1.3 View Live Logs
```bash
tail -f supplygraph.log
# Or inspect container logs:
docker logs -f supplygraph-postgres
```

---

## 2. Demo User Personas & Role Switcher

SupplyGraph enforces strict server-side Role-Based Access Control (RBAC). For local evaluation and separation-of-duties demonstration, the **Demo Role Switcher** is enabled in the top navigation bar. Selecting any persona immediately updates the session context and reloads with that persona's permissions:

| Persona Role | Display Name | Core Permissions & Landing Cockpit |
|---|---|---|
| `SYSTEM_ADMIN` | System Administrator | Full platform authority, quotas, policies, API keys (`/admin/enterprise`) |
| `WORKSPACE_ADMIN` | Workspace Administrator | Workspace configuration, users, policies (`/workspace/admin`) |
| `SUPPLY_CHAIN_MANAGER` | Lead Supply Chain Manager | Digital twin management, disruption handling, replanning (`/workspace/planner`) |
| `PROCUREMENT_MANAGER` | Procurement Manager | Supplier allocation, purchase orders, RFQ bidding (`/workspace/procurement`) |
| `PRODUCTION_MANAGER` | Production Manager | Plant work center schedules, capacity commitments (`/workspace/production`) |
| `LOGISTICS_MANAGER` | Logistics Manager | Freight routes, transport lanes, delivery confirmations (`/workspace/logistics`) |
| `QUALITY_MANAGER` | Quality Manager | AS9100 / IATF inspections, 8D quality incident resolution (`/workspace/quality`) |
| `ANALYST` | Operations Research Analyst | What-if simulations, Monte Carlo, Pareto frontiers (`/workspace/analyst`) |
| `VIEWER` | Read-Only Auditor | View-only inspection of dashboards and immutable audit trails |

---

## 3. Recommended Interactive Evaluation Tours

### Tour A: The 9 Persona Cockpits & Command Centre
1. Navigate to [http://localhost:8080/command-center](http://localhost:8080/command-center).
2. Type `MCU` into the global search bar to instantly query parts, disruption incidents, and POs.
3. Use the top persona switcher or the sidebar "Role Workspaces" menu to visit:
   - **Executive Cockpit:** [http://localhost:8080/workspace/executive](http://localhost:8080/workspace/executive)
   - **Planner Cockpit:** [http://localhost:8080/workspace/planner](http://localhost:8080/workspace/planner)
   - **Procurement Cockpit:** [http://localhost:8080/workspace/procurement](http://localhost:8080/workspace/procurement)
   - **Production Cockpit:** [http://localhost:8080/workspace/production](http://localhost:8080/workspace/production)
   - **Logistics Cockpit:** [http://localhost:8080/workspace/logistics](http://localhost:8080/workspace/logistics)
   - **Quality Cockpit:** [http://localhost:8080/workspace/quality](http://localhost:8080/workspace/quality)
   - **Analyst Cockpit:** [http://localhost:8080/workspace/analyst](http://localhost:8080/workspace/analyst)
   - **Supplier Cockpit:** [http://localhost:8080/workspace/supplier](http://localhost:8080/workspace/supplier)
   - **Admin Cockpit:** [http://localhost:8080/workspace/admin](http://localhost:8080/workspace/admin)

### Tour B: Disruption Ingestion, ConcurGuard & Decision Centre
1. Open **Disruptions Console:** [http://localhost:8080/disruptions](http://localhost:8080/disruptions). Inspect active incident `Munich Fab Flooding`.
2. Open **Impact Assessment:** [http://localhost:8080/impact](http://localhost:8080/impact). View cascading blast radius across Tier 1, Tier 2, and assembly plants.
3. Open **Simulation Workspace:** [http://localhost:8080/simulation](http://localhost:8080/simulation) & **Monte Carlo:** [http://localhost:8080/monte-carlo](http://localhost:8080/monte-carlo). Review P10, P50, and P90 risk profiles.
4. Open **Optimization Workspace:** [http://localhost:8080/optimization](http://localhost:8080/optimization). Trigger Google OR-Tools MIP solver.
5. Open **Recovery Decision Centre:** [http://localhost:8080/decision-centre](http://localhost:8080/decision-centre). Inspect alternative plans and approve via human-in-the-loop workflow.

### Tour C: Multi-Echelon Planning (MEIO)
1. Open **Planning Command Centre:** [http://localhost:8080/planning](http://localhost:8080/planning).
2. Inspect **Safety Stock Planner:** [http://localhost:8080/planning/inventory/safety-stock](http://localhost:8080/planning/inventory/safety-stock).
3. Inspect **Supplier Allocation:** [http://localhost:8080/planning/procurement/allocation](http://localhost:8080/planning/procurement/allocation).
4. Inspect **Scenario Comparison:** [http://localhost:8080/planning/scenarios/results](http://localhost:8080/planning/scenarios/results).

### Tour D: Sustainability, Carbon & Traceability
1. Open **Sustainability Command Centre:** [http://localhost:8080/sustainability](http://localhost:8080/sustainability).
2. Inspect **Carbon Footprint:** [http://localhost:8080/sustainability/carbon](http://localhost:8080/sustainability/carbon) (Scope 1, 2, 3 breakdown).
3. Open **Product Genealogy:** [http://localhost:8080/sustainability/traceability/genealogy](http://localhost:8080/sustainability/traceability/genealogy).
4. Run **Recall Impact Analysis:** [http://localhost:8080/sustainability/traceability/recall](http://localhost:8080/sustainability/traceability/recall) (traverse forward DAG from tainted silicon to finished customer lots).

### Tour E: Supplier Collaboration & 8D Quality
1. Open **SRM Hub:** [http://localhost:8080/collaboration/suppliers](http://localhost:8080/collaboration/suppliers).
2. Open **RFQ Management:** [http://localhost:8080/collaboration/rfq](http://localhost:8080/collaboration/rfq).
3. Open **Supplier Portal:** [http://localhost:8080/portal](http://localhost:8080/portal) (PO acknowledgments, ASNs).
4. Open **8D Quality Investigations:** [http://localhost:8080/collaboration/quality](http://localhost:8080/collaboration/quality).

### Tour F: Enterprise Administration, Policies & Disaster Recovery
1. Open **Admin Centre:** [http://localhost:8080/admin/enterprise](http://localhost:8080/admin/enterprise).
2. Open **Workspace Quotas:** [http://localhost:8080/admin/workspaces](http://localhost:8080/admin/workspaces).
3. Open **Industry Policies:** [http://localhost:8080/admin/industry-policies](http://localhost:8080/admin/industry-policies) (Draft, Validate, Activate, Diff, Rollback).
4. Open **API Developer Centre:** [http://localhost:8080/admin/api](http://localhost:8080/admin/api) (Generate scoped keys, view curl docs).
5. Open **Disaster Recovery Vault:** [http://localhost:8080/admin/backup](http://localhost:8080/admin/backup) (Capture snapshots, verify SHA-256 integrity).
6. Open **System Health & Performance:** [http://localhost:8080/admin/health](http://localhost:8080/admin/health) & [http://localhost:8080/admin/performance](http://localhost:8080/admin/performance).

---

## 4. Resetting the Demo Workspace

To reset only the `WS-AUTO-01` demo workspace without affecting other databases or schemas:
```bash
docker exec supplygraph-postgres psql -U postgres -d supplygraph -c "DELETE FROM workspaces WHERE id = 'WS-AUTO-01';"
```
On the next application launch, `DemoDataInitializer` will automatically reseed fresh, pristine demonstration data.

