# 08 — Simulation and Optimization

## Purpose

The optimizer searches for candidate decisions. The simulator evaluates their consequences over time. Neither should rely on LLM arithmetic.

## Optimization responsibilities

Potential decision variables:
- Supplier selection and procurement quantities.
- Inventory allocation by facility and order.
- Production quantities and time slots.
- Shipment route and departure choices.
- Recovery action scheduling.

Potential objectives:
- Minimize total recovery cost.
- Minimize late orders and delay.
- Minimize production loss.
- Maximize service level.
- Minimize disruption exposure.

Use configurable weighted objectives or Pareto comparisons. Do not hide trade-offs inside one unexplained score.

## Hard constraints

Examples:
- Material availability.
- Supplier capacity.
- Factory/line capacity.
- Inventory conservation.
- BOM requirements.
- Route and shipment capacity.
- Time windows and precedence.
- Qualified supplier/material rules.
- Quality holds and expiry restrictions.
- Industry-specific constraints.

A solution is feasible only if all hard constraints pass. Soft objectives rank feasible solutions.

## Digital twin simulation

- Define a deterministic simulation clock and event ordering.
- Support baseline, disrupted and recovery-plan runs.
- Model inventory arrivals/consumption, production, shipments and demand.
- Support simultaneous disruption events.
- Persist input state version, parameters, engine version and results.
- Make runs reproducible with recorded random seeds when stochastic models are used.
- Clearly distinguish predicted/estimated outcomes from observed facts.

## Experiment comparisons

At minimum compare:
1. No recovery / baseline policy.
2. Single-agent or independent-agent proposals without conflict coordination.
3. Basic resource reservation/locking baseline.
4. ConcurGuard conflict-aware coordination.
5. ConcurGuard plus selective replanning.

## Metrics

- Feasible combined-plan rate.
- Number of detected conflicts.
- False-positive and false-negative conflict rates, where ground truth is available.
- Recovery time.
- Total recovery cost.
- Late-order count / service level.
- Production loss.
- Number of replanned actions.
- Percentage of unaffected actions preserved.
- Computation time and LLM cost/latency.

Do not publish performance claims until experiments are reproducible and baseline implementations are fair.
