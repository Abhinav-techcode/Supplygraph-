# Handoff prompt for the next coding agent (Antigravity)

Paste everything below the line into the agent. Context: repo `/Users/abhinavgupta/Desktop/Supplygraph`, branch `product-realignment` (changes are uncommitted in the working tree). Build with JDK 21: `JAVA_HOME=<jdk21> ./mvnw clean verify` (Docker needed). Run the UI: `./run-demo.sh` (H2, port 8080; demo role switcher is in the left menu). Read first: `docs/product/SUPPLYGRAPH_PRODUCT_VISION_VALIDATION.md`, `docs/product/SUPPLYGRAPH_PRODUCT_GAP_ANALYSIS.md`, `docs/architecture/GRAPH_ARCHITECTURE_ASSESSMENT.md`.

---

You are continuing the realignment of SupplyGraph from "many modules plus a seeded demo" to one integrated, honest product. Rules: never show invented numbers (use "No data"), never label simulated things as real, no mock data in views, keep hexagonal boundaries (ArchUnit), add tests for every change, run the full build before reporting, and report exactly what passed and what did not.

## Already done in the working tree (verified: 453 tests, 0 failures)
* Digital twin persisted and reloaded (`TwinGraphStore`, Flyway V16, `TwinPersistenceIntegrationTest`).
* One workspace id (`WS-AUTO-01`) everywhere; duplicate seeding removed.
* Graph read model (`NetworkGraphViewService`) and a graph UI rendered only from it; the hard-coded graph is gone; global search covers facilities.
* Recovery options derived from the twin (`RecoveryOptionDeriver`: buffer stock, alternative supplier, resequencing; avoids disrupted lanes/nodes) instead of three canned plans. Cost is the sum of lane costs per shipment (item prices are not modelled).
* About 25 fabricated KPI tiles replaced by computed values or "No data"; fake supplier OTIF/lead-time relabelled as structural estimates; fake agent history and the false "maturity checklist" removed; "Phase N" badges removed.
* Demo helper, working role switcher, action-first grids.

## Still to do, in this order
1. **Seed data realism**: `DemoDataInitializer` still seeds hub/port nodes as `Warehouse`, and lane `costPerKg` values are really per-shipment costs (the derived plans show very large costs because of that). Make the sample network consistent. Add an explicit "Load sample supply chain" action and a **supply-chain import** (CSV/JSON of nodes, lanes, materials, BOM, inventory) that writes to the persisted twin with validation and versioning. Do not seed on startup in production profiles.
2. **Impact analysis** (`ImpactAnalysisApplicationService`): propagate through the BOM to finished products and customers, use inventory time-to-survive, show the affected path on the graph, and replace the hard-coded `$85` unit price with a configurable, visibly labelled assumption.
3. **Recommendation pipeline**: run `RecoveryOptionDeriver` outputs through simulation and ConcurGuard with real resource/capacity claims (today `detectConflicts` builds intents with no claims), rank them, and generate explanations from the actual numbers. `OptimizationWorkspaceView` must build candidates from the twin, not literals.
4. **Agents**: rebuild the eight agents (`supplygraph-adapter-ai`) so each reads twin/graph/impact context through the bounded tool boundary and returns proposals that depend on it (LLM behind the adapter, deterministic fake for tests). Invoke them from incident handling, record real investigations and show them in Agent History. Proposals still go through ConcurGuard and human approval.
5. **Outcome -> twin**: make verified (non-simulated) execution evidence update twin inventory/capacity, and make monitoring consume `RECOVERY_ACTION_COMPLETED`.
6. **Connect Phases 11-13 to the twin** (planning, sustainability/traceability, supplier collaboration): read demand/inventory/BOM/capacity from the twin, link genealogy to twin materials/lots, and let supplier capacity confirmations and quotations feed planning and execution tasks.
7. **Security**: real authentication (OIDC) replacing `UiPrincipalFilter`; apply `WorkspaceAccessGuard` and permissions to the Phase 4-8 and 11-15 services (only ~10 classes enforce it today).
8. **Navigation/IA**: replace the ~50-entry module menu with task areas (Network, Incidents, Decisions, Execution, Suppliers, Planning, Sustainability, Administration) and one incident-centred guided workspace (map with affected path, impact, options with cost/time/risk/CO2, approve, execute, outcome). Keep research pages under "Analytics lab".
9. **Tests that prove integration**: twin change changes the graph; disruption yields an impact path; execution outcome changes the twin; supplier confirmation changes the plan.
10. Finish with an operator manual, one coherent seeded scenario across all phases, and a local acceptance report with exact commands and results.
