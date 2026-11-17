# SupplyGraph Phase 4 — Mathematical Optimization Engine (Google OR-Tools)

## 1. Architectural Overview

The **Mathematical Optimization Engine** in `supplygraph-simulation-engine` applies deterministic mixed-integer programming (MIP) and linear programming (LP) to solve multi-echelon supply-chain recovery problems. It formulates mathematically optimal trade-offs across financial cost, customer SLA tardiness, and emissions/carbon impact.

```
+-------------------------------------------------------------------------------+
|                    Google OR-Tools MIP & Relaxation Engine                    |
|                                                                               |
|   +-----------------------+      +------------------+      +---------------+  |
|   | RecoveryOptProblem    | ---> | MPSolver (GLOP/  | ---> | Optimization- |  |
|   | Demand, Limits, Opts  |      | SCIP) or Fallback|      | Result        |  |
|   +-----------------------+      +------------------+      +---------------+  |
|               |                           |                        |          |
|               v                           v                        v          |
|   +-----------------------+      +------------------+      +---------------+  |
|   | Multi-Objective       |      | Native / Simplex |      | Allocations,  |  |
|   | Weights (Cost/SLA/CO2)|      | Constraint Model |      | Costs, Status |  |
|   +-----------------------+      +------------------+      +---------------+  |
+-------------------------------------------------------------------------------+
```

## 2. Solver Strategy & Fail-Safe Architecture

1. **Primary Solver — Google OR-Tools Java 9.11 (`MPSolver`)**:
   - Integrates with GLOP (Google Linear Optimization Package) and SCIP/CBC for Integer Programming.
   - Evaluates boolean decision variables $x_i \in \{0, 1\}$ (activation of recovery options) and continuous variables $y_i \ge 0$ (allocated material volumes).

2. **Pure-Java Linear Relaxation Fallback**:
   - In environments where native C++ libraries for Google OR-Tools are unavailable or unsupported on specific OS architectures, the engine cleanly catches `UnsatisfiedLinkError` or `NoClassDefFoundError` and delegates to `solveSimplexRelaxation()`.
   - The greedy knapsack and simplex relaxation guarantees deterministic, high-quality, bounded feasibility without throwing unhandled runtime failures to user sessions.

## 3. Mathematical Formulation

### Objective Function
Minimize the weighted scalar objective:
$$\min \left( w_{\text{cost}} \cdot \frac{C_{\text{total}}}{C_{\text{ref}}} + w_{\text{sla}} \cdot \frac{T_{\text{max}}}{T_{\text{ref}}} + w_{\text{co2}} \cdot \frac{E_{\text{total}}}{E_{\text{ref}}} \right)$$

subject to:
$$w_{\text{cost}} + w_{\text{sla}} + w_{\text{co2}} = 1.0, \quad w_k \ge 0$$

### Constraints
1. **Demand Satisfaction**:
   $$\sum_{i \in \text{Alternatives}} y_i \ge D_{\text{required}}$$
2. **Supplier/Lane Capacity Limits**:
   $$0 \le y_i \le C_i^{\max} \cdot x_i, \quad \forall i$$
3. **Budget Upper Bound**:
   $$\sum_{i} \left( c_i^{\text{fixed}} \cdot x_i + c_i^{\text{unit}} \cdot y_i \right) \le B_{\text{max}}$$
4. **Single-Source / Multi-Source Policy Constraints**:
   $$\sum_{i \in \text{Exclusive}} x_i \le 1$$

## 4. Output Contract (`OptimizationResult`)

- `optimal`: Boolean flag indicating mathematical optimality or feasible approximation.
- `objectiveValue`: Minimized scalar multi-objective score.
- `totalCost`: Cumulative financial cost of selected options.
- `estimatedLeadTimeDays`: Critical path latency of the selected recovery combination.
- `co2EmissionsKg`: Projected carbon expenditure.
- `allocations`: Map of alternative IDs to recommended quantities.
- `status`: Solver exit state (`OPTIMAL`, `FEASIBLE`, `INFEASIBLE`, `FEASIBLE_RELAXATION_FALLBACK`).

