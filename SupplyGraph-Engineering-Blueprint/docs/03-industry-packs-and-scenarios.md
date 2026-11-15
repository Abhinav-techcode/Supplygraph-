# 03 — Industry Packs and Initial Scenario Catalogue

## Extension contract

Each industry pack must define:
- Industry ID, name and version.
- Domain entity extensions and terminology.
- Scenario catalogue.
- Validation rules and hard constraints.
- Relevant KPI definitions.
- Agent capabilities and permitted tools.
- Data import mappings and provenance requirements.
- Scenario-specific acceptance tests.

Scenario definitions must be configuration/domain objects, not scattered `if (industry == ...)` branches. Shared workflows should call industry contracts.

The initial catalogue contains eight scenarios per industry. This is a starting library, not an exhaustive taxonomy.

## 1. EV battery manufacturing

1. **Lithium supply shortage** — assess qualified alternatives, available inventory, lead times and allocation.
2. **Nickel/cobalt price surge** — compare sourcing and product-mix alternatives under cost constraints.
3. **Battery material shipment delay** — calculate affected production orders and alternative transport/supply options.
4. **Storage temperature excursion** — flag affected inventory for quality assessment; do not automatically declare material safe or unsafe without approved rules and evidence.
5. **Cell manufacturing equipment failure** — recalculate capacity and reschedule production.
6. **Battery quality rejection spike** — trace affected lots, estimate replacement requirements and assess order impact.
7. **EV demand surge** — allocate materials, production capacity and finished inventory.
8. **Recycling/recovery capacity disruption** — assess secondary-material availability and alternative qualified recovery capacity.

## 2. Pharmaceutical supply chain

1. **API supplier shortage** — identify qualified alternative sources and assess approval constraints.
2. **Cold-chain excursion** — flag affected shipments/batches for qualified disposition; do not infer product usability.
3. **Expiry risk** — prioritize allocation using approved shelf-life and customer rules.
4. **Manufacturing batch failure** — assess replacement production, batch traceability and order impact.
5. **Packaging material shortage** — evaluate approved alternatives and production schedule effects.
6. **Regulatory or quality hold** — block affected batches from allocation and trace downstream exposure.
7. **Medicine demand surge** — allocate scarce inventory under configured priority and policy constraints.
8. **Warehouse/distribution disruption** — identify qualified alternate storage and distribution paths.

## 3. Semiconductor manufacturing

1. **Wafer supply shortage** — assess qualified supply and wafer allocation.
2. **Specialty chemical shortage** — evaluate alternative qualified sources and affected process stages.
3. **Fabrication equipment breakdown** — recalculate fab capacity and reschedule work-in-progress.
4. **Cleanroom environmental anomaly** — flag affected operations for review under configured process rules.
5. **Yield deterioration** — trace lots, quantify yield impact and estimate capacity loss.
6. **Packaging/testing bottleneck** — evaluate alternate qualified capacity and order prioritization.
7. **Chip demand spike** — optimize capacity allocation and customer commitments.
8. **Export/geopolitical restriction** — identify affected suppliers, regions and feasible sourcing alternatives.

## 4. Automotive component manufacturing

1. **Critical component shortage** — find alternatives and allocate available parts.
2. **Tier-2/Tier-3 supplier failure** — discover hidden upstream dependencies and downstream exposure.
3. **Just-in-time delivery failure** — estimate line-stop risk and evaluate inventory/logistics responses.
4. **Assembly-line equipment failure** — recalculate production capacity and delivery commitments.
5. **Component quality defect** — trace affected lots and assemblies; support quarantine/rework decisions.
6. **Transport route disruption** — evaluate alternate routes, costs and delivery windows.
7. **Vehicle demand surge** — balance component inventory, production capacity and customer commitments.
8. **Supplier capacity reduction** — redistribute sourcing across qualified suppliers.

## Scenario schema

Every scenario should contain:
- Stable scenario ID and industry ID.
- Name, description and severity.
- Preconditions and affected entity types.
- Trigger mechanism (manual, imported event or synthetic experiment).
- Required input data and validation.
- State transition/event produced.
- Impact-analysis rules.
- Industry constraints.
- Candidate recovery actions.
- Expected outputs and KPIs.
- Safety/approval requirements.
- Deterministic acceptance tests.

## Data and compliance caution

These scenario definitions are software planning abstractions. Real pharmaceutical, semiconductor and battery handling constraints must be supplied or reviewed by qualified domain experts and authoritative sources before any operational claim is made.
