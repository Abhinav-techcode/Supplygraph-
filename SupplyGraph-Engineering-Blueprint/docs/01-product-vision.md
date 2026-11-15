# 01 — Product Vision and Scope

## 1. Product statement

SupplyGraph is an industry-configurable software platform that models supply networks, receives operational and disruption events, evaluates downstream impact, coordinates specialized AI agents, detects conflicts between proposed actions, validates recovery plans through deterministic optimization and simulation, and presents evidence-backed recommendations to human decision-makers.

## 2. Product goals

- Represent multi-tier supply networks and their operational dependencies.
- Model industry-specific entities, processes, constraints and disruption scenarios.
- Detect and explain direct and cascading disruption impacts.
- Use specialized agents to investigate and propose recovery actions.
- Prevent individually plausible but collectively infeasible agent plans from being combined.
- Compare recovery alternatives using deterministic calculations and simulation.
- Support scenario experimentation and repeatable evaluation.
- Provide an extensible architecture for additional industries.

## 3. Target users

- Supply-chain control tower analyst
- Procurement manager
- Production planner
- Logistics manager
- Quality/risk analyst
- Operations manager
- Platform administrator
- Researcher/experiment designer

## 4. Initial industry packs

1. EV battery manufacturing
2. Pharmaceutical supply chain
3. Semiconductor manufacturing
4. Automotive component manufacturing

Each pack supplies domain vocabulary, entity extensions, constraints, scenario definitions, KPI definitions and agent capabilities. Shared platform workflows remain industry-independent.

## 5. Product principles

- Decision support before automation.
- Evidence before recommendation.
- Deterministic validation before approval.
- Explicit uncertainty and data provenance.
- Explainable impact paths and trade-offs.
- Industry configuration over duplicated code.
- Human approval for consequential actions.
- Software-only; no physical IoT/hardware scope.

## 6. Explicit non-goals for initial release

- Direct control of industrial machinery or PLCs.
- Physical sensor, ESP32, MQTT or hardware integration.
- Autonomous purchase orders, shipment bookings or factory control.
- A replacement for ERP, MES, WMS, TMS or regulated quality systems.
- Guaranteed predictive accuracy without validated data.
- Production-grade integrations with every vendor.
- Microservices and distributed deployment before justified.
- Blockchain or unrelated technology additions.

## 7. Data strategy

Support three clearly distinguished data classes:

- **Synthetic/demo data:** generated for deterministic demonstrations; visibly labelled synthetic.
- **Imported data:** uploaded by a user or loaded from a documented public dataset; preserve source and import timestamp.
- **External integration data:** obtained from a configured external system; record source, timestamp, freshness and validation status.

Never present synthetic events as live industrial telemetry. Never fabricate supplier identities, prices, capacities or operational facts.

## 8. Success criteria

The platform should demonstrate, through reproducible tests and experiments:

- Correct dependency and downstream-impact traversal.
- Detection of resource and temporal conflicts across agent plans.
- Rejection or repair of infeasible combined plans.
- Preservation of unaffected valid actions during selective replanning.
- Measurable comparison against baseline coordination approaches.
- Traceable explanations for recommendations.
- Repeatable scenario execution and results.
