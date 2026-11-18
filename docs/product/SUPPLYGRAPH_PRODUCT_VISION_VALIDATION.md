# SupplyGraph — Product Vision Validation (Phase Zero)

Scope: understand the intended product, then test whether it was built. **No code, configuration or behaviour was changed**; only the six analysis documents were written. Evidence date 2026-11-18. Inspected: repository working tree (HEAD `9dfc00c` plus ~230 uncommitted files, which include the Phase 11–15 implementation), source reading, `grep`-based cross-reference checks, and the running application on port 8080 in a browser. Companion documents: `SUPPLYGRAPH_INTENDED_VS_ACTUAL.md`, `SUPPLYGRAPH_PRODUCT_GAP_ANALYSIS.md`, `SUPPLYGRAPH_UX_PROBLEM_ANALYSIS.md`, `../architecture/SUPPLYGRAPH_ACTUAL_ARCHITECTURE.md`, `../architecture/GRAPH_ARCHITECTURE_ASSESSMENT.md`.

**Limits of this review (what I did not verify):** I did not run the Monte Carlo/DES engines on twin data, did not read every one of the 128 views or all Phase 11–15 algorithms, did not run the full test suite on the current (concurrently edited, uncommitted) tree, and did not inspect `docs/testing/*` produced by another session. Existing documents in `docs/` (e.g. `SUPPLYGRAPH_PHASE_1_15_MASTER_AUDIT.md`, which rates Phases 1–10 "COMPLETE AND VERIFIED" and calls Phases 11–15 "not implemented") are inconsistent with the current tree and with each other and were treated as claims, not evidence.

## 1. The intended product, reconstructed
An industry-configurable, agentic supply-chain intelligence platform whose **centre is a living digital twin** (persisted network, BOM, inventory, capacity) from which everything else is derived: disruption detection and propagation, agent investigation, deterministic conflict arbitration (ConcurGuard), simulation, optimization, explainable recommendations, human-governed execution, outcome reconciliation that feeds the twin, plus supplier collaboration, sustainability/traceability, integration, and role-based experiences. Its test is whether a user can follow **one incident from signal to executed, verified recovery** on their own data.

## 2. Headline finding
The platform is **a collection of well-engineered modules, around a seeded demonstration, not yet one integrated product.** The strongest, genuinely verified parts are the governance and reliability layers (Phase 5 monitoring, Phase 9 outbox/connectors, Phase 10 execution). The parts that define the *intelligence* of the product — the twin as source of truth, the graph, the agents, the recommendation pipeline, the dashboards — are largely **presentation or fixtures**:

1. The twin graph is **not persisted**; it lives in JVM memory and is rebuilt from literals at every start.
2. The graph on screen is **hard-coded** and never reads the twin or disruptions.
3. The eight agents return **constant proposals** and are **never invoked** from the UI.
4. "Alternatives" are **three canned templates**; ConcurGuard receives intents with **no resource claims**.
5. Executive KPIs (**87.4/100**, **$4.2M**) and supplier OTIF/lead-time are **invented constants**.
6. Phases 11–15 (planning, sustainability, collaboration, platform) are **islands** with no reference to the twin, events or execution, and only 10 service classes enforce permissions at all.
7. Two workspace ids are hard-coded across the views, so the same session shows contradictory numbers.

The test suite (≈441 `@Test` methods) is green because it validates modules in isolation; no test asserts a cross-module effect.

## 3. Scores (1–10, evidence-based)

| Dimension | Score | Basis |
|---|---|---|
| Product vision alignment | **5** | All 17 capabilities have code and pages; 0 are fully aligned; the intelligence core is fixture-driven. |
| Architecture coherence | **5** | Excellent layering and boundaries; no coherent data flow: twin not durable, three topology copies, four sets of recovery options, two workspace ids. |
| Digital twin quality | **4** | Strong in-memory aggregate (versioning, ledger, snapshots); not persisted, no import/edit, not fed by integrations or outcomes. |
| Graph intelligence | **3** | Real reachability/path traversal; no BOM/material-supply edges, no SPOF/min-cut, heuristic single-source, constant supplier metrics. |
| AI usefulness | **2** | Agents are constants; the framework (bounded tools, identity, audit) is real but unused; no model client wired. |
| Optimization correctness | **5** | Genuine LP/MIP formulation; inputs hard-coded in the view; simple model. |
| Decision explainability | **4** | Provenance, templated explanations, audit trail exist; explanations do not derive from real evaluation of the options. |
| Governed execution | **7** | Verified: state machines, SoD, two-person rule, evidence, unknown outcomes, restart recovery, bounded reassessment. Simulated adapter only; outcomes do not change the twin. |
| Data integrity | **4** | Execution/outbox data is transactional and tested; the twin is volatile; duplicate sources of truth; constants displayed as data. |
| Security | **4** | Strong in Phase 9/10 services; **no authentication**; no permission/workspace enforcement in Phase 4–8 and 11–15 services; dev-mode UI. |
| UI/UX quality | **4** | Polished look, graph canvas attractive; navigation mirrors modules (~50 entries), contradictory numbers, empty first screens, layout defects. |
| Ease of operation | **4** | Requires route knowledge and manual role switching; one-command demo exists only on a separate branch copy. |
| Demo readiness | **5** | A guided Phase 9/10 demo works (on the demo copy); the main story (graph → impact → agents → plan) cannot stand up to questions ("change the data and watch the graph"). |
| Deployment readiness | **3** | H2/PostgreSQL profiles and Docker files exist; no production Vaadin bundle, no auth, volatile twin, secrets via env only. |

## 4. How the implemented percentage was computed
Each of the 17 intended capabilities (list in `INTENDED_VS_ACTUAL`) is scored by status: FA = 1.0, IN = 0.6, PI = 0.35, PO = 0.1–0.15, NI = 0; adjusted within a status where evidence differs (graph 0.3, agents 0.15, dashboards 0.2, simulation 0.5 because input provenance was not verified, optimization 0.5, integration 0.45).
Scores: twin 0.35, dependency intelligence 0.6, graph 0.3, disruption/propagation 0.6, agents 0.15, ConcurGuard 0.6, simulation 0.5, optimization 0.5, planning 0.35, explainability 0.35, governed execution 0.6, reconciliation 0.35, collaboration 0.35, integration 0.45, sustainability 0.35, dashboards 0.2, industry configuration 0.3 → **6.9 / 17 ≈ 41 %**.
This is a judgement-weighted estimate with equal capability weights, not a measurement; a reasonable range is **35–45 %**. Counting only capabilities that are *fully aligned and verified* gives **0 of 17**. By code volume the figure would be far higher (the modules exist), which is exactly the trap the brief warns about.

## 5. Required decisions

1. **Have we built the SupplyGraph we intended?** Not yet. We have built most of its parts and the strongest governance layer, but the core promise — a living twin driving explainable, agent-informed recovery decisions on the user's own supply chain — is not delivered.
2. **Percentage genuinely implemented and verified:** ≈ **41 %** (range 35–45 %); 0 of 17 capabilities fully aligned and verified (method in §4).
3. **Is the graph architecture suitable?** The domain graph is a sound base for logistics topology but **must change**: persist it, add material-supply and BOM-aware edges, add SPOF/propagation algorithms, and publish one read model for all consumers.
4. **Is the graph visualization good enough for enterprise use?** No. It is visually good but fictional (hard-coded data labelled LIVE). It must be rebound to the twin.
5. **Is the UI good enough for a professional demonstration?** Not as it stands: contradictory figures, invented KPIs, empty first screens, and a module-list navigation will be exposed by any probing question. The visual quality is a good base.
6. **Are the 15 phases genuinely integrated?** No. Phases 1–10 are connected in a chain (disruption → impact → plan → approval → execution) but with canned recommendations and a one-way, event-only link back; Phases 11–13 and 15 are independent islands; Phase 14 presents them together without shared data.
7. **Five biggest weaknesses:** (1) volatile, unpersisted digital twin with no load/edit path; (2) fictional graph and no real graph intelligence (BOM/material/SPOF); (3) agents and recommendations are fixtures, not the product's intelligence; (4) Phases 11–15 disconnected from the twin and execution, and execution outcomes never update the twin or monitoring; (5) invented KPIs, duplicate sources of truth (two workspace ids, three topologies) and missing authentication/authorization across most services.
8. **What must change before the final demonstration:** items marked P0 in the gap analysis — persisted twin with a supply-chain import, graph bound to the twin with real impact paths, a recommendation pipeline whose output depends on the incident (agents/optimizer/simulation/ConcurGuard with real claims), removal of fabricated KPIs, one workspace context, outcome → twin feedback, a task-oriented navigation with an incident-centred guided flow, authentication, and one coherent seeded scenario across all phases.
9. **What can safely remain unchanged:** module/hexagonal structure and ArchUnit rules, the domain primitives, Phase 5 monitoring, Phase 9 connector/outbox/webhook runtime, Phase 10 governed execution engine, ConcurGuard detectors, the OR-Tools and simulation engines, audit signing, the visual design language.
10. **Is the technology stack appropriate?** Yes. Java 21, Spring Boot, Vaadin, PostgreSQL, Flyway, OR-Tools and the Spring AI adapter boundary are adequate; a graph database is not needed at the intended scale. Needs: JDK 21 pinned, a production Vaadin build, and an authentication provider (OIDC).

## 6. Recommendation

**REARCHITECT SPECIFIC SUBSYSTEMS** — not a rewrite and not merely targeted polish:

* Re-architect: **digital-twin persistence and graph model/read model**, the **recommendation pipeline**, the **agent layer**, and the **navigation/information architecture** (including one workspace context).
* Targeted improvements: impact analysis, ConcurGuard inputs, optimization inputs, Phases 11–13 integration with the twin, outcome → twin feedback, security sweep, dashboards.
* Keep: everything listed under KEEP in the gap analysis.

Following the brief, work stops here. The implementation phase should follow the order in `SUPPLYGRAPH_PRODUCT_GAP_ANALYSIS.md` and should begin by agreeing the target scenario and graph model.
