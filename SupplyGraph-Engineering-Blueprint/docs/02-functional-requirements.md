# 02 — Functional Requirements

Requirement IDs are stable references. Prioritize by release; do not attempt every requirement in one implementation pass.

## A. Workspace and industry configuration

- FR-IND-001: A user can create a workspace and select an enabled industry pack.
- FR-IND-002: An administrator can configure facilities, suppliers, products, materials and network relationships.
- FR-IND-003: The system loads industry-specific terminology, scenario definitions and constraints through an extension contract.
- FR-IND-004: Adding an industry must not require modifying shared orchestration logic.
- FR-IND-005: Imported data must be validated and associated with its source.

## B. Supply network digital twin

- FR-DT-001: Store suppliers, sites, factories, warehouses, customers, materials, components, products and orders.
- FR-DT-002: Represent directed, typed relationships and their validity periods.
- FR-DT-003: Represent BOMs and production-stage dependencies.
- FR-DT-004: Track inventory by item, location, lot/batch where applicable, quantity and availability time.
- FR-DT-005: Track supplier, facility, route and production capacity over time.
- FR-DT-006: Query upstream and downstream dependencies.
- FR-DT-007: Maintain versioned snapshots or equivalent reproducible state for planning.
- FR-DT-008: Display data provenance and freshness.

## C. Disruption management

- FR-DIS-001: Create, update, acknowledge, investigate, mitigate and resolve disruption incidents.
- FR-DIS-002: Support disruption type, severity, start time, expected duration, affected entity and evidence.
- FR-DIS-003: Calculate affected downstream entities and explain the dependency path.
- FR-DIS-004: Support simultaneous disruptions.
- FR-DIS-005: Distinguish observed, imported and synthetic events.
- FR-DIS-006: Maintain an incident audit trail.

## D. Agentic investigation

- FR-AI-001: Coordinator delegates bounded tasks to specialized agents.
- FR-AI-002: Agents return schema-validated structured findings and action proposals.
- FR-AI-003: Each finding includes evidence references, assumptions and uncertainty.
- FR-AI-004: Agents invoke only allow-listed Java tools.
- FR-AI-005: Agent output cannot directly mutate authoritative supply-chain state.
- FR-AI-006: Failed, timed-out or malformed agent responses are handled safely.
- FR-AI-007: Record agent runs, inputs, outputs, model/provider and timestamps, subject to privacy controls.

## E. ConcurGuard

- FR-CG-001: Register each proposed action as an intent with actor, action type, resources, quantity, time interval, location, expected state version and dependencies.
- FR-CG-002: Identify semantic overlap where different action names consume or modify the same underlying resource.
- FR-CG-003: Detect quantity, capacity, temporal, dependency and stale-state conflicts.
- FR-CG-004: Distinguish hard conflicts from potentially schedulable conflicts.
- FR-CG-005: Produce an explanation and evidence for every conflict.
- FR-CG-006: Support approve, reject, delay, modify and request-replan outcomes.
- FR-CG-007: Revalidate plans against a consistent state version before approval.
- FR-CG-008: Support selective replanning while preserving unaffected valid actions.
- FR-CG-009: Maintain immutable decision history.
- FR-CG-010: Do not treat database locking alone as the complete semantic conflict solution.

## F. Optimization and simulation

- FR-OPT-001: Evaluate alternative suppliers and sourcing allocations.
- FR-OPT-002: Evaluate inventory allocation and production scheduling.
- FR-OPT-003: Support configurable objectives and hard constraints.
- FR-OPT-004: Return objective values, feasibility status and constraint explanations.
- FR-SIM-001: Run a baseline, disruption and recovery scenario against a versioned digital-twin state.
- FR-SIM-002: Support multiple simultaneous disruptions.
- FR-SIM-003: Compare recovery alternatives with consistent assumptions.
- FR-SIM-004: Record simulation configuration, inputs, outputs and engine version.
- FR-SIM-005: Clearly label estimates and synthetic assumptions.

## G. User experience and governance

- FR-UI-001: Provide dashboards for network health, incidents, agent runs, conflicts and recovery plans.
- FR-UI-002: Provide a human review and approval workflow.
- FR-UI-003: Show why a recommendation was made, affected entities, assumptions and trade-offs.
- FR-UI-004: Provide role-based access.
- FR-UI-005: Export reports and experiment results.
- FR-SEC-001: Enforce authentication and authorization on server-side operations.
- FR-AUD-001: Record security-relevant and decision-relevant actions.

## Requirement discipline

Before implementation, add acceptance criteria, priority (P0/P1/P2), dependencies and test references to each requirement. A requirement is not complete merely because a screen or endpoint exists.
