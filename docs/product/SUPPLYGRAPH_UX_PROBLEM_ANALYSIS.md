# UX Problem Analysis — first-time user walk-through

Method: opened the running app (port 8080, local profile, demo role switcher on) in the built-in browser as a user who knows supply-chain basics but not the architecture; read page text and screenshots; followed links. Pages visited: `/`, `/digital-twin`, `/command-center`, `/workspace/executive`, `/planning/demand`, `/sustainability/traceability/genealogy`, `/decision-centre`, plus (earlier, on my demo copy) the Phase 9/10 flows. Not every one of the 128 routes was opened.

## 1. Can a first-time user…

| # | Task | Result | Why |
|---|---|---|---|
| 1 | Understand what it does | **Partly** | Home headline and paragraph are clear. But the page also carries a "Phase 4 Operational Capabilities" bullet list (internal history), and the top bar says "Phases 1–15 Production Suite". |
| 2 | Understand what the graph represents | **No** | It is a tier-column picture labelled "LIVE". Nodes are facilities; edges are lanes (not stated); no legend; the page cannot say what a disruption affects. |
| 3 | Create or load a supply chain | **No** | No screen, import, or API creates or edits a network. The 17-node demo chain is baked in. The "Data Integration/Import" pages import data that never reaches the twin. |
| 4 | Identify a disruption | **Yes** | `Disruptions` and the Home table list two seeded incidents; new ones can be registered. |
| 5 | Understand its impact | **Partly** | `Impact` returns downstream facilities; the graph does not light up the affected path, and the executive page shows 0 active disruptions. |
| 6 | Run an analysis | **Yes, with confusion** | Simulation, Monte Carlo, Optimization, What-if, Pareto, Sensitivity, Advanced Resilience are separate pages, each with its own inputs and no guidance on order or on how they relate. |
| 7 | Understand AI recommendations | **No** | Three plans (Alpha/Beta/Gamma) with fixed numbers; "Agent History" shows four hard-coded rows. No agent output is traceable to the situation. |
| 8 | Compare recovery strategies | **Partly** | Cards for three plans, a Pareto page, an Explainable Recovery page — three different sets of options (Home text names yet another set). |
| 9 | Approve a decision | **Yes** | Decision Centre → Validate → Approve works. (Approver field was free text until my fix.) |
| 10 | Execute it | **Only with help** | Phase 9/10 pages exist but start empty; the user must know to go to "Governed Execution", convert a plan, switch role three times. My demo helper and "What to do next" column fix this only in the demo copy. |
| 11 | See what happened afterwards | **Partly** | Timeline, plan-vs-actual, closed-loop exist; nothing flows back to the graph/dashboards. |

## 2. Confusion points (evidence)
1. **Navigation = module list.** ≈50 top-level menu entries plus expandable groups ("SC Planning" 15 items, "Sustainability" 12, "Enterprise Platform"…). Labels such as "Advanced Resilience", "Experiment Manager", "Benchmark Studio", "Statistical Results", "Decision Intelligence", "Decision Provenance", "Explainable Recovery", "Data Provenance" are research/engineering concepts, not tasks. There is no guided flow Detect → Understand → Decide → Execute → Learn.
2. **Two "command centres"**: `/` Overview and `/command-center`; plus 9 "Role Workspaces" duplicating the main pages with different framing.
3. **Contradictory numbers on the same session**: Home — 2 active disruptions; Executive cockpit — 0 and "Revenue-at-Risk $4.2M", "Resilience 87.4/100" (literals); Global Search for "Munich" — empty although Munich is the headline disruption. Cause: two workspace ids (`WS-AUTO-01` vs `default-workspace`) hard-coded across views.
4. **Global search is not searching the product**: it covers disruptions/orders/RFQs/lots/incidents through services bound to one workspace; it does not search graph nodes (placeholder promises "network nodes", "CATL", "BAT") and returned nothing for a known entity. No empty-state message explains this.
5. **The graph is decorative**: clicking nodes shows static values ("4,200 units", "CONTENDED (Active Claim Conflicts)"); filters work; creating or resolving a disruption never changes it. Claims "real-time", "LIVE".
6. **Empty first screens**: execution, planning, sustainability, collaboration pages open with empty grids or require operator-typed sample input; nothing seeds a coherent scenario for Phases 11–15 (`DemoDataInitializer` has no planning/sustainability/collaboration data).
7. **Role switching and identity**: a "DEMO role switcher — not authentication" is the only identity mechanism; roles determine what succeeds, but the user is never told why an action was refused until a red toast appears.
8. **Layout defects** (seen): graph page title squeezed into a narrow column; wide tables hide their action buttons off-screen on a normal window (fixed only in the demo copy via frozen action columns); Vaadin "Development Mode" banner on every page.
9. **Terminology leaks**: phase numbers ("Phase 14 :: Unified Product Experience" badge), internal state names (`PENDING_APPROVAL`, `RECONCILIATION_REQUIRED`) shown raw without explanation, enum names as role labels.
10. **No orientation**: no onboarding, no "scenario" concept, no sample-data indicator on pages, no breadcrumbs, no cross-links from an alert to its impact to the plan to the workflow (links exist inside the Home buttons only).

## 3. Verdict
The UI is professional in visual style (consistent Lumo styling, good graph canvas) but **exposes backend modules as pages** rather than guiding a user through the disruption-recovery story. A professional demo today requires the presenter to know the route order, switch roles by hand, and avoid pages that contradict each other. It is not yet intuitive.

## 4. What would make it intuitive (inputs to the later phase)
* One **incident-centred workspace**: select an incident → map with affected path highlighted → impact summary (products, customers, revenue from real data) → options with cost/time/risk/CO₂ → approve → execute → outcome, in a single guided screen with a persistent stepper.
* Navigation reduced to ~8 task areas (Network, Incidents, Decisions, Execution, Suppliers, Planning, Sustainability, Administration); engineering/research pages (benchmarks, statistics, experiments) under an "Analytics lab" group.
* Remove literal KPIs; every tile shows "unknown/no data" when it has none.
* One workspace id, one seeded scenario covering all phases, a visible "SAMPLE DATA" marker.
* A first-run tour and a plain-language label for every status.
