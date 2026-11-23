# SupplyGraph Metric Provenance Register

**Version:** 1.0.0  
**Status:** Certified Active Governance  
**Scope:** SupplyGraph Enterprise Digital Twin, Multi-Agent ConcurGuard Engine, Simulation & Optimization Platform

---

## 1. Principles of Metric Provenance

SupplyGraph enforces strict provenance on every operational, analytical, financial, and sustainability metric displayed across UI dashboards, agent proposals, and executive decision centers.

### Governance Directives:
1. **Zero Fake Metrics:** If real telemetry or digital twin ledger transactions are absent, the platform surfaces `UNKNOWN`, `NO_DATA`, or `UNASSESSED`. It never fabricates fallback averages or synthetic placeholder numbers in production profiles (`!demo`).
2. **Deterministic Derivations:** All calculations use mathematically sound formulas implemented in pure domain logic (`supplygraph-core-domain`).
3. **Double-Entry Ledger Integrity:** All inventory balances are validated against immutable debit/credit transaction histories in `InventoryLedger`.
4. **Transparent Assumptions:** Every estimation discloses its mathematical assumptions and confidence intervals to human decision-makers.

---

## 2. Metric Specification Catalog

| Metric Identifier | Metric Name | Standard Unit | Mathematical Formula & Source | Upstream Entity | Missing Data Behavior | Governance Threshold |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **FIN-EXPOSURE-01** | Downstream Financial Exposure | USD ($) | $\sum_{d \in D} (Q_d \times P_m)$ where $Q_d$ is demand quantity and $P_m$ is material/product price | Finished Goods BOM & Customer Demand Ledger | Displays `UNASSESSED`; flags missing pricing to Procurement | Exposure > \$500,000 requires VP approval |
| **INV-TTS-01** | Inventory Time-to-Survive (TTS) | Days | $\min_{n \in N_{down}} \left( \frac{\text{On-Hand Available}(n, m)}{\text{Daily Consumption Rate}(n, m)} \right)$ | `InventoryLedger` on-hand balances & Manufacturing Line throughput | Displays `INSUFFICIENT_TELEMETRY`; assumes 0 buffer | TTS < 3.0 days triggers Critical Operational Alert |
| **NET-SPOF-01** | Single Point of Failure (SPOF) Index | Categorical (LOW, MEDIUM, HIGH, CRITICAL) | Directed acyclic graph connectivity: $|Paths(S, D) \setminus \{n\}| = 0$ | `SupplyChainGraph` topological analysis | Evaluates to `INDETERMINATE` if network graph disconnected | Critical SPOF requires dual-sourcing contingency |
| **BOM-CASCADE-01** | BOM Cascading Impact Depth | Integer (Tiers) | Recursive graph traversal depth from impacted raw material node to leaf finished product | `BillOfMaterials` relationship hierarchy | Defaults to 1 (immediate direct consumption) | Depth $\ge 3$ triggers Multi-Tier Agent Swarm |
| **OR-COST-01** | Total Recovery Option Cost | USD ($) | $\sum_{l \in L_{route}} (CostPerKg_l \times Q_{ship}) + \sum \text{ExpediteSurcharges}$ | `TransportationLane` cost matrix & Supplier contracts | Shows `0.00 (Unmodeled Costs Disclosed)` with explicit warning | Cost > Budget threshold requires CFO signoff |
| **OR-TIME-01** | Recovery Resolution Delay | Days | $\sum_{l \in L_{route}} \text{NominalTransitTime}_l + \text{ProcurementLeadDays}$ | Lane transit durations & Supplier Master records | Displays nominal lane transit without customs buffer | Delay > 5.0 days triggers Customer Order Reassessment |
| **ESG-CO2-01** | Freight Carbon Emissions | kg CO₂e | $\sum_{l \in L} (Distance_l \times Weight_{kg} \times EmissionFactor_{mode})$ | Logistics lanes & EN 16258 / GLEC emissions database | Displays `ESTIMATED (GLEC-V3 Standard)` | Emission delta > +25% requires Sustainability Signoff |
| **ROB-SCORE-01** | Plan Robustness Score | Percentage (%) | $1.0 - \left( \text{RiskScore}_{supplier} \times 0.4 + \text{LaneUnreliability} \times 0.6 \right)$ | Supplier Risk Profiler & Carrier Historical Telemetry | Evaluates to 50.0% (Uncertainty penalty applied) | Robustness < 70% requires Secondary Fallback Plan |
| **AGT-CONF-01** | Agent Confidence Score | Percentage (%) | Weighted Bayesian evidence score based on tool outputs and data freshness | `AgentProposal` metadata | Capped at 40% if inputs contain stale state | Confidence < 80% mandates Human Approval |
| **CONC-INV-01** | ConcurGuard Claim Feasibility | Boolean / Status | $\sum_{i \in Intents} ClaimedQty(i, acc) \le AvailableBalance(acc)$ | Living Twin `InventorySnapshot` & `ResourceClaim` | Hard failure (`CONFLICT_DETECTED`); blocks plan approval | Any detected invariant breach blocks execution dispatch |

---

## 3. Disruption & Impact Derivation Rules

### 3.1. Inventory Balance Computation
- **Source of Truth:** `com.supplygraph.domain.model.inventory.InventoryLedger`
- **Methodology:** Complete algebraic summation of all posted `InventoryPosting` records grouped by `InventoryAccountId`:
  $$\text{Balance} = \sum \text{Debit Postings} - \sum \text{Credit Postings}$$
- **Invariant:** Balance in `ON_HAND_AVAILABLE` must never be negative ($B \ge 0$). Any attempt to commit or allocate more than available triggers `CONCURGUARD_QUANTITY_CONFLICT`.

### 3.2. Transportation Lane Reliability & Lead Time
- **Source of Truth:** `com.supplygraph.domain.model.graph.TransportationLane`
- **Transit Duration:** Explicit `java.time.Duration` assigned to edge.
- **Multimodal Speeds & Costs:**
  - `ROAD`: 65 km/h, \$0.85–\$1.20/kg
  - `RAIL`: 45 km/h, \$0.42–\$0.65/kg
  - `OCEAN`: 30 km/h, \$0.15–\$0.28/kg
  - `AIR`: 750 km/h, \$4.50–\$8.50/kg

---

## 4. Audit & Regulatory Compliance
- Every calculation change, threshold violation, and human approval is logged via `com.supplygraph.application.audit.AuditGovernanceService` to immutable PostgreSQL audit storage.
- Complies with ISO 28000 (Supply Chain Security Management) and EU Corporate Sustainability Due Diligence Directive (CSDDD).

