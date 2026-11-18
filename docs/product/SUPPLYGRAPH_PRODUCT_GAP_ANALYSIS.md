# SupplyGraph — Product Gap Analysis and Prioritized Recommendations

Derived from `SUPPLYGRAPH_INTENDED_VS_ACTUAL.md`, `SUPPLYGRAPH_ACTUAL_ARCHITECTURE.md`, `GRAPH_ARCHITECTURE_ASSESSMENT.md`, `SUPPLYGRAPH_UX_PROBLEM_ANALYSIS.md`. Priorities: **P0** blocks an honest demonstration or breaks the vision's core loop; **P1** needed for a professional product; **P2** valuable, can follow.

## KEEP (satisfies the intended product; do not rewrite)
| Item | Why |
|---|---|
| Module structure, hexagonal ports/adapters, ArchUnit rules | Clean, enforced, testable; not the source of the problems. |
| Domain model: `SupplyChainGraph`, BOM, inventory ledger, capacity calendars, versioned `DigitalTwinState`, snapshots | Good foundations; need persistence and extension, not replacement. |
| Phase 5 monitoring/ingestion, Phase 9 outbox/inbox, webhooks, SSRF guard, reconciliation | Real, tested on PostgreSQL. |
| Phase 10 governed execution (state machines, SoD, evidence, unknown-outcome handling, restart recovery, closed-loop governor) | Strongest part of the product; verified end to end. |
| ConcurGuard detectors and benchmark | Real engine; needs better input, not a rewrite. |
| OR-Tools model and DES/Monte Carlo engines | Real solvers; need real inputs. |
| Audit signing, outbox, RBAC services where applied | Sound building blocks. |
| Visual design language of the graph canvas and Lumo styling | Looks professional. |

## IMPROVE (works, needs refinement)
| Item | Gap | Pri |
|---|---|---|
| Impact analysis | Add BOM explosion to products/customers/revenue, inventory time-to-survive, multi-tier material view; show path on the graph. | P0 |
| ConcurGuard live path | Populate resource claims, capacity claims, dependencies and preconditions from plan actions so detectors can fire. | P1 |
| OR-Tools workspace | Build candidates from the twin (suppliers/sites/lanes/capacity), not literals. | P1 |
| Supplier/material intelligence | Replace constant OTIF/lead-time with values from delivery data or label "no data"; replace the single-source heuristic with graph analysis. | P0 (fabricated KPIs) |
| Decision Centre / explainability | Explanations generated from the actual evaluation (impact path, simulation result, optimizer output, ConcurGuard findings). | P1 |
| Phase 11 planning | Pull demand, inventory, BOM, capacity from the twin; keep the algorithms. | P1 |
| Phase 12 sustainability | Link carbon/ESG/traceability to twin materials, lots and suppliers; keep calculators. | P1 |
| Phase 13 collaboration | Feed capacity confirmations, quotation results and quality events into planning, risk scoring and execution tasks. | P1 |
| Phase 10 → twin | Make verified execution evidence change twin state (inventory/capacity/ETA) and make monitoring consume `RECOVERY_ACTION_COMPLETED`. | P0 for "learn" claim |
| Security | Apply `WorkspaceAccessGuard` + permissions to Phase 4–8 and 11–15 services; real authentication (OIDC) replacing the configured operator. | P0 |
| UI defects | Graph title layout, off-screen action columns (already fixed in the demo copy), dev-mode banner, raw enum labels. | P1 |

## REDESIGN (current architecture/UX unsuitable)
| Item | Why | Pri |
|---|---|---|
| **Digital-twin persistence & loading** | JVM-memory twin with a version row cannot be the source of truth; V2 tables unused. Persist graph/BOM/inventory/capacity, load at startup, version with optimistic locking, provide import and edit paths. | **P0** |
| **Graph model + visualization** | Visualization is fictional; model lacks material-supply edges and SPOF analysis. See `GRAPH_ARCHITECTURE_ASSESSMENT.md`. | **P0** |
| **Recommendation generation pipeline** | Three canned templates are the "AI + optimization + simulation" output. Design a pipeline: impact → candidate generation (agents + optimizer) → simulation → ConcurGuard → ranking → explanation. | **P0** |
| **Navigation / information architecture** | Module-per-menu-entry (≈50) → task-oriented areas and an incident-centred guided workspace. | **P0** |
| **Workspace/tenant identity** | `WS-AUTO-01` vs `"default-workspace"` literals in ≈25 places → one workspace context resolved from the authenticated principal. | **P0** |
| **Dashboards/role workspaces** | Replace literal KPIs with computed values or "no data"; derive persona pages from the same read models. | P1 |

## REBUILD (cannot reasonably be made to satisfy the vision by targeted improvement)
| Item | Why | Pri |
|---|---|---|
| **The eight "agents"** | They are constant-returning stubs; the orchestrator is unused. Rebuild the agent layer behind the existing bounded-tool boundary: each agent reads real twin/graph/impact context via tools and produces a proposal whose content depends on it (LLM-backed behind the adapter, with the deterministic fake kept for tests; every proposal still validated by ConcurGuard and approved by a human). Keep identities, tool allow-lists, audit. | **P0** (it is a headline capability) |
| **Graph visualization component** (the SVG literals) | Replace with a view bound to the new graph read model; reuse styling. | P0 (covered above) |

## REMOVE (redundant, misleading, unused)
| Item | Reason |
|---|---|
| Hard-coded KPIs (`87.4 / 100`, `$4.2M`), constant supplier OTIF/lead-time, "LIVE/real-time" labels on static pages | Misleading. |
| `DigitalTwinGraphExplorerView` sample tables (second copy of the topology) | Duplicate source of truth. |
| "Phase N" badges and the "Phase 4 Operational Capabilities" block on Home | Internal history, not product copy. |
| Hard-coded `AgentInvestigationHistoryView` rows | Fictional audit data; show real investigations or nothing. |
| V2 tables never used (`supply_chain_nodes`, …) — *unless* adopted by the redesigned persistence | Dead schema. |
| Duplicate entry points (Overview vs Command Centre; per-persona pages that repeat module pages) | Confusion; merge. |

## ADD (essential and missing)
| Item | Pri |
|---|---|
| Supply-chain **import and editing** (CSV/JSON/connector → twin nodes, lanes, BOM, inventory) with validation and versioning | P0 |
| **Incident-centred guided workspace** (map + impact + options + approval + execution + outcome) | P0 |
| One **coherent seeded scenario** across all phases (disruption → options → approval → execution → outcome; planning, ESG, supplier data consistent with the same network) and a "load demo supply chain" action with a visible SAMPLE marker | P0 |
| **Authentication** and workspace-scoped authorization | P0 |
| Real **agent reasoning** with traceable inputs/outputs and evaluation tests ("output changes when input changes") | P0 |
| Integration tests that assert **cross-module effects**: twin change → graph; disruption → impact path; execution outcome → twin; supplier confirmation → plan | P0 |
| Production deployment path (Vaadin production build, PostgreSQL profile, secrets, observability) | P1 |
| A LIVE execution adapter against a documented test endpoint | P2 |

## Suggested order (for the later implementation phase)
1. Persist and load the twin; one workspace context; import path.  2. Graph model + read model + visualization bound to it.  3. Impact analysis with BOM propagation and SPOF analysis; remove fabricated KPIs.  4. Recommendation pipeline (candidates from optimizer/agents → simulation → ConcurGuard with real claims → explanation).  5. Agent rebuild.  6. Execution outcome → twin and monitoring consumption.  7. Integrate Phases 11–13 with the twin.  8. Navigation/IA redesign and incident workspace.  9. Authentication/authorization sweep.  10. Seeded scenario, operator manual, acceptance tests.
