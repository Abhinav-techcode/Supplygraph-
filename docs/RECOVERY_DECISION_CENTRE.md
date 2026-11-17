# SupplyGraph Phase 4 — Vaadin Flow Recovery Decision Centre

## 1. Architectural Overview

The **Recovery Decision Centre** in `supplygraph-web` delivers a human-in-the-loop decision-support interface built on **Vaadin Flow 24.6** and Spring Boot 3.4. It translates complex simulation trajectories, stochastic risk surfaces, and MIP solver outputs into actionable, interactive dashboards.

Strict architectural guardrails are maintained:
- **No Direct Entity Access**: Vaadin views interact exclusively via application service ports (`DisruptionApplicationService`, `ImpactAnalysisApplicationService`, `SimulationApplicationService`, `MonteCarloApplicationService`, `OptimizationApplicationService`, `RecoveryPlanningApplicationService`).
- **Zero Authoritative State Mutation by Agents or Simulators**: Only human decision-makers with explicit identity and rationale can approve recovery plans.
- **Optimistic Locking & ConcurGuard Protection**: All plan approvals verify that the underlying `DigitalTwinState` version has not drifted between proposal generation and human sign-off.

```
+-------------------------------------------------------------------------------+
|                       Vaadin Flow Navigation Architecture                     |
|                                                                               |
|   +-----------------------------------------------------------------------+   |
|   |                           MainLayout                                  |   |
|   |  Overview | Disruptions | Impact | Simulation | Monte Carlo | Opt | RDC   |   |
|   +-----------------------------------------------------------------------+   |
|                                       |                                       |
|       +-------------------------------+-------------------------------+       |
|       |                               |                               |       |
|       v                               v                               v       |
|   [DisruptionsView]          [SimulationView]            [RecoveryDecision-   |
|   Incident Triage            Chronological Event Traj.    CentreView]         |
|   & Timeline                 & Bottlenecks               Alternative Plans,   |
|                                                           Diffs & Approvals   |
+-------------------------------------------------------------------------------+
```

## 2. Implemented Decision Centre Views

### 1. `HomeView` (`/`)
- Platform KPI summary cards: Active Incidents, At-Risk Products, Pending Recovery Plans, Simulation Engines.
- Interactive shortcuts directly linking to operational workspaces.
- Platform architecture overview explaining the flow from Digital Twin to Human Decisions.

### 2. `DisruptionsView` (`/disruptions`)
- Interactive filterable grid displaying all disruptions across workspaces.
- Status badge indicators (`DRAFT`, `VALIDATED`, `ACTIVE`, `RESOLVED`, `CANCELLED`).
- Severity indicators (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `NEGLIGIBLE`).
- Detail dialog with direct triggers to launch Cascading Impact Analysis.

### 3. `ImpactAssessmentView` (`/impact`)
- Cascading blast radius breakdown generated via topological graph traversal.
- Direct & indirect affected node hierarchy.
- Bill of Materials explosion highlighting component stockouts and assembly line stops.
- Transportation lane bottleneck latency tracking.

### 4. `SimulationWorkspaceView` (`/simulation`)
- Discrete-event simulation parameter configuration (Horizon, Step Size, Seed).
- Interactive simulated event queue timeline (`SimulationEventQueue`).
- Key performance indicators: Processed Event Count, Overall Service Level %, Total Shortage Volume, Max Delay.
- Time-series trajectory visualizer for inventory buffers and line utilization.

### 5. `MonteCarloWorkspaceView` (`/monte-carlo`)
- Stochastic risk analysis configuration (Replication Count, Seed, Distribution Selection).
- Quantile percentile metrics: Mean, Median, P90, and P95 delivery delays.
- Probability of Stockout and SLA Compliance Rate dials.
- Value-at-Risk financial loss distribution.

### 6. `OptimizationWorkspaceView` (`/optimization`)
- Google OR-Tools multi-objective weight sliders ($w_{\text{cost}}, w_{\text{sla}}, w_{\text{co2}}$).
- Mixed-Integer Programming solver dispatch with automatic linear relaxation fallback.
- Optimal alternative supplier allocations, cost breakdown, and lead-time reductions.

### 7. `RecoveryDecisionCentreView` (`/decision-centre`)
- Side-by-side comparative scorecard of candidate recovery alternatives:
  - **Plan Alpha (Fast-Track Expedited Logistics)**: Maximum SLA protection, higher transportation cost.
  - **Plan Beta (Alternate Supplier Reallocation)**: Balanced cost and lead-time with dual-sourcing.
  - **Plan Gamma (Production Rescheduling & Deferral)**: Minimized cash burn, delayed non-critical orders.
- ConcurGuard concurrency validation indicator with conflict detection.
- **Audited Human Approval Flow**:
  - Modal confirmation dialog capturing Decision-Maker Name, Reason, and Approval Timestamp.
  - Optimistic locking validation verifying snapshot freshness before committing state.

