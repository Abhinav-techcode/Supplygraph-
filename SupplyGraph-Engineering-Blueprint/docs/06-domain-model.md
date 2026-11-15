# 06 — Core Domain Model

## Design principles

- Model business concepts explicitly; avoid generic maps and untyped JSON as the core domain.
- Use `BigDecimal` for money and precise quantities where appropriate; define units.
- Use `Instant` for event timestamps and explicit time zones for local business schedules.
- Represent intervals consistently (recommend half-open `[start, end)`).
- Distinguish physical quantity, available quantity, reserved quantity and projected quantity.
- Every important record should carry workspace/tenant scope where applicable.
- Record provenance for imported and synthetic data.
- Avoid industry-specific fields in universal entities when an extension/value object is more appropriate.

## Core concepts

- Workspace / IndustryProfile
- Organization / Supplier / Facility
- Factory / Warehouse / DistributionCenter / Customer
- Material / Component / Product
- BillOfMaterials / ProductionStage
- SupplierRelationship / SupplyEdge
- InventoryPosition / InventoryLot / Batch
- CapacityResource / CapacityWindow
- Route / Shipment / ShipmentEvent
- CustomerOrder / ProductionOrder
- DisruptionIncident / ImpactAssessment
- AgentRun / AgentFinding / ActionIntent
- ResourceClaim / Conflict / ArbitrationDecision
- RecoveryPlan / RecoveryAction
- SimulationRun / SimulationResult
- Approval / AuditEvent / DataProvenance

## Important invariants

1. A resource cannot be allocated beyond its available capacity over overlapping time windows.
2. A plan must identify the digital-twin state version against which it was generated.
3. An intent based on a stale state version must be revalidated before acceptance.
4. An agent proposal is not an executed action.
5. A simulated or imported record must retain its provenance label.
6. A quality hold or quarantine blocks allocation until a qualified disposition is recorded.
7. A recovery plan must satisfy all configured hard constraints before it can be marked feasible.
8. Conflicts and arbitration decisions are auditable.
9. Quantities must have compatible units before arithmetic or comparison.
10. Workspace boundaries must be enforced in every repository query and application use case.

## Industry extensions

Industry packs may add entities and policies such as:
- EV: cell chemistry, battery cell/pack, material grade, recycling stream.
- Pharma: API, formulation, batch, expiry, cold-chain excursion, quality disposition.
- Semiconductor: wafer lot, process stage, fab tool, yield, specialty chemical.
- Automotive: part number, tier supplier, assembly line, vehicle programme.

Do not assume the illustrative list is complete. Validate each pack with domain-specific acceptance criteria.
