# SupplyGraph Phase 4 — Discrete-Event Simulation Engine

## 1. Architectural Overview

The **Discrete-Event Simulation (DES) Engine** is an isolated, deterministic, chronological event simulator residing in `supplygraph-simulation-engine`. It operates strictly on immutable snapshots of the digital twin state (`DigitalTwinSnapshot`) and simulates operational cascades, disruptions, and recovery strategies across multi-tier supply networks without ever mutating authoritative production ledgers.

```
+-------------------------------------------------------------------------------+
|                        Discrete-Event Simulation Engine                       |
|                                                                               |
|   +-----------------------+      +------------------+      +---------------+  |
|   | SimulationEventQueue  | ---> | Event Dispatcher | ---> | State Traj.   |  |
|   | (PrioQueue: Time/Prio)|      | (Event Handlers) |      | & Metrics     |  |
|   +-----------------------+      +------------------+      +---------------+  |
|               ^                           |                                   |
|               |                           v                                   |
|   +-----------------------+      +-------------------------+                  |
|   | SimulationConfig      |      | SimulatedDigitalTwin-   |                  |
|   | Horizon & Step Size   |      | State (In-Memory Clone) |                  |
|   +-----------------------+      +-------------------------+                  |
+-------------------------------------------------------------------------------+
```

## 2. Event Dispatching & Deterministic Ordering

All simulation events implement `Comparable<SimulationEvent>` and are ordered via the strict contract:
1. **Timestamp (`Instant`)**: Chronological simulated time progression.
2. **Priority (`int`)**: Deterministic priority tier (lower integer = higher execution precedence for simultaneous events).
3. **Event Identifier (`String`)**: Lexicographical tie-breaker guaranteeing identical replay.

### Supported Simulation Events (`SimulationEventType`):
- `DISRUPTION_START`: Initial shock injection into target nodes or transportation lanes.
- `INVENTORY_SHORTAGE_DETECTED`: Triggered when demand exceeds on-hand physical stock.
- `SHIPMENT_DEPARTED`: Transit dispatched along edges with lead-time latency.
- `SHIPMENT_ARRIVED`: Inbound material received and deposited into available inventory.
- `PRODUCTION_STARTED`: Assembly sequence scheduled against active line capacity.
- `PRODUCTION_COMPLETED`: Output batch manufactured and credited to warehouse accounts.
- `CAPACITY_DEGRADED`: Site throughput diminished due to incident outage or maintenance.
- `CAPACITY_RESTORED`: Restoration of operational capacity post-disruption.
- `RECOVERY_ACTION_TRIGGERED`: Execution of approved candidate recovery actions.
- `DISRUPTION_RESOLVED`: Incident closure and resumption of steady-state parameters.
- `CHECKPOINT`: Periodic state observation for trajectory tracking.

## 3. State Isolation & Safeguards

- **Authoritative Isolation**: The simulator clones domain structures into `SimulatedDigitalTwinState`. No JPA entities or persistent tables are altered during simulation.
- **Clock Boundaries**: The clock advances monotonically; events with timestamps prior to the current clock time are rejected with `IllegalArgumentException`.
- **Termination Safeguards**: Bounded by `maxEvents` (default 50,000) and explicit horizon duration, preventing infinite scheduling loops.
- **Trajectories**: Generates time-series data points (`TrajectoryPoint`) tracking inventory levels, capacity utilization, and stockout occurrences.

## 4. Output Contract

The simulation produces an immutable `SimulationResult`:
- `processedEventCount`: Total event transitions executed.
- `eventTimeline`: Audited log of all dispatched events.
- `inventoryTrajectories`, `productionTrajectories`, `shortageTrajectories`.
- `serviceLevelPercentage`: Customer fulfillment ratio achieved.
- `totalShortageQuantity`: Cumulative unfulfilled unit volume.
- `maxDelaySeconds`: Maximum delivery lateness observed.
- `status`: Execution state (`COMPLETED`, `TERMINATED_MAX_EVENTS`, `FAILED`).
