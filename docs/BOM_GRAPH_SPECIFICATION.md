# SupplyGraph — Multi-Tier Bill of Materials (BOM) Specification

**Document ID:** `SG-SPEC-003`  
**Phase:** Phase 2 (Digital Twin Core & Graph Domain Model)  
**Status:** Approved Engineering Specification  

---

## 1. Mathematical Representation

A multi-tier Bill of Materials is formalized as a finite, directed acyclic graph (DAG):
$$G_{\text{BOM}} = (M, E)$$
Where:
- $M$ is the finite set of materials (Raw Materials, Intermediates, Components, Finished Products).
- $E \subset M \times M$ is the set of directed consumption edges $(P, C)$, where parent material $P$ requires component material $C$.
- Each edge carries:
  - $q(P, C) \in \mathbb{Q}^+$: Nominal quantity of component $C$ required to assemble one unit of parent $P$.
  - $s(P, C) \in [0.0, 1.0)$: Scrap/yield loss factor for this assembly step.

---

## 2. Invariants & Guarantees

### 2.1 Acyclicity (No Circular Assemblies)
A product cannot depend on itself directly or transitively:
$$\nexists \, (m_1, m_2, \dots, m_k, m_1) \quad \text{such that} \quad (m_i, m_{i+1}) \in E$$
- Enforced on construction using 3-color Depth First Search (white = unvisited, gray = visiting, black = visited).
- Encountering a gray node detects a back-edge and throws `IllegalStateException`.

### 2.2 Deterministic Topological Ordering
Using Kahn's algorithm, materials are sorted in deterministic bottom-up topological order:
$$\forall (P, C) \in E, \quad \text{index}(C) < \text{index}(P)$$
Leaf raw materials appear first, followed by subassemblies, culminating in finished products.

### 2.3 Multi-Level Scrap-Adjusted Explosion
When calculating total component requirements for $Q_{\text{root}}$ units of finished product $P_{\text{root}}$:
$$\text{GrossRequirement}(C) = \text{NetQuantity}(C) \times (1 + s(P, C))$$
For deep multi-tier hierarchies, scrap factors compound deterministically through the DAG:
$$\text{Total Gross}(C_{\text{leaf}}) = Q_{\text{root}} \prod_{i=1}^{k} q(P_i, C_i) \cdot (1 + s(P_i, C_i))$$

---

## 3. High-Precision Arithmetic

All BOM quantity calculations are executed with `BigDecimal`:
- Standard scale: 4 decimal digits.
- Rounding mode: `RoundingMode.HALF_UP`.
- Floating-point `double` and `float` types are strictly prohibited in the domain model.

