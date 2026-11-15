# 07 — Agentic AI and ConcurGuard Design

## Agent responsibilities

- Coordinator: manages bounded workflow, deadlines and escalation.
- Risk Intelligence: assesses disruption evidence and risk exposure.
- Inventory: reports inventory position, reservations and shortages.
- Procurement: proposes qualified sourcing alternatives.
- Logistics: proposes route and shipment alternatives.
- Production: proposes capacity and schedule adjustments.
- Quality: interprets recorded quality events under configured rules.
- Recovery Planner: assembles candidate plans and explains trade-offs.

Agents are capabilities, not autonomous database owners. They use allow-listed Java tools and return typed proposals.

## Structured proposal contract

An action proposal should include:
- `proposalId`
- `agentId`
- `incidentId`
- `actionType`
- `targetEntity`
- `resourceClaims`
- `quantity` and unit, if applicable
- `timeWindow`
- `location`
- `preconditions`
- `dependencies`
- `expectedStateVersion`
- `evidenceReferences`
- `assumptions`
- `confidence` with defined semantics
- `reversibility`
- `proposedEffects`
- `requiresHumanApproval`

Validate every field and reject malformed or unsupported proposals. Never execute natural-language text as a tool instruction.

## ConcurGuard intent registry

An intent is a normalized representation of a proposed action. It should express what resource is affected, how much, where, when, under which preconditions and against which state version.

Resource claims may include:
- Inventory quantity at a location and time.
- Supplier capacity during a period.
- Production line capacity.
- Warehouse capacity.
- Route/shipping capacity.
- Qualified batch/lot availability.
- Shared equipment or process capacity.

## Conflict classes

- Quantity conflict: combined claims exceed available quantity.
- Capacity conflict: combined use exceeds capacity.
- Temporal conflict: incompatible actions overlap in time.
- Dependency conflict: an action's prerequisite is absent or invalid.
- Semantic conflict: differently named actions consume or modify the same underlying resource.
- State conflict: proposal was generated from stale state.
- Policy conflict: proposal violates an industry rule or approval policy.

## Conflict resolution

Possible outcomes:
- Allow: no conflict.
- Delay/reschedule: conflict can be resolved in another time window.
- Modify: adjust quantity, route, facility or schedule.
- Reject: infeasible or prohibited.
- Replan: ask affected agents to produce alternatives.

Do not overuse pessimistic locking. Database transactions protect persistence integrity; ConcurGuard addresses plan-level semantic and temporal compatibility. Both are required for different failure classes.

## Selective replanning

1. Receive a new event or changed state.
2. Identify affected resources and dependency subgraph.
3. Mark only impacted intents/actions as requiring revalidation.
4. Preserve unaffected actions if their preconditions remain true.
5. Replan the minimal affected subgraph.
6. Re-run conflict detection and simulation.
7. Record what was preserved, changed, rejected and why.

## LLM safety and reliability

- Treat retrieved documents, user uploads and model outputs as untrusted data.
- Do not let prompt content override system policy or tool permissions.
- Use typed tool arguments and server-side authorization.
- Apply timeouts, retries with limits, rate limits and token budgets.
- Keep deterministic calculations outside the LLM.
- Log enough for debugging without storing secrets or unnecessary personal data.
- Provide graceful behavior when the LLM provider is unavailable.
- Do not claim a model is reasoning correctly merely because its response is fluent.

## Human approval

Initial release must require explicit human approval before consequential external actions. The application may simulate the proposed effect before approval, but must not place real orders, book shipments or alter external industrial systems.
