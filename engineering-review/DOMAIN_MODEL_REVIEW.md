# SupplyGraph — Domain Model & Graph Representation Review

**Document ID:** `SG-ENG-REV-005`  
**Author:** Principal Software Architect & Domain-Driven Design Specialist  
**Date:** November 2026  
**Status:** Domain Model Review Baseline  

---

## 1. Domain Modeling Principles & Invariants

SupplyGraph models complex industrial supply chains using **Domain-Driven Design (DDD)**. The domain layer is pure Java, free from persistence and web framework annotations.

### 1.1 Fundamental Conventions
1. **Strong Typing & Value Objects:** Generic `String` and `Map<String, Object>` are banned in core models. Entities use strongly typed IDs (`SupplierId`, `FacilityId`, `MaterialId`, `OrderId`).
2. **Numeric Precision:** Money, prices, and quantities use `java.math.BigDecimal` with explicit scale. Floating-point types (`float`, `double`) are strictly prohibited in domain accounting.
3. **Units of Measure (UoM):** Every quantity is a composite Value Object `Quantity(BigDecimal amount, UnitOfMeasure unit)`. Arithmetic between incompatible units (e.g., metric tons vs. kilograms) requires explicit domain unit conversion.
4. **Temporal Consistency:** Time intervals are modeled as half-open intervals `[start, end)` using `java.time.Instant`. This prevents boundary overlap ambiguities when booking capacity windows or shift schedules.
5. **State Versioning:** Every entity snapshot carries a monotonically increasing `stateVersion` (e.g., `Long`). Recovery proposals reference `expectedStateVersion`.
6. **Workspace Isolation:** Every root aggregate belongs to an immutable `WorkspaceId`. Cross-workspace data leakage is impossible at the domain level.
7. **Explicit Data Provenance:** Every operational record carries a `DataProvenance` value object (`SourceType` [SYNTHETIC, IMPORTED, LIVE_INTEGRATION], `sourceIdentifier`, `importedAt`, `confidenceScore`).

---

## 2. Supply Network Graph Representation

The digital twin graph is modeled mathematically as a **Typed Attributed Directed Graph** $G = (V, E)$:
- Vertices $V$ represent physical and organizational entities.
- Edges $E$ represent relationships, transport lanes, and bill-of-materials dependencies.

```
[Supplier] ─── (SupplyEdge / LeadTime) ───► [Facility: Factory / Warehouse]
    │                                                   │
    ▼                                                   ▼
[Material Lot] ── (BOM / Stage Dependency) ──► [Component / Finished Product]
                                                        │
                                                        ▼ (ShipmentRoute)
                                                [Customer Distribution]
```

### 2.1 Graph Node Entities ($V$)
1. **`Organization` & `Supplier`:** Legal and operational entities. Attributes: tier level (Tier-1, Tier-2, Tier-3), qualification status, lead times, risk score, geography.
2. **`Facility`:** Physical sites with subtypes:
   - `Factory`: Contains production lines, cleanrooms, takt time constraints.
   - `Warehouse / DistributionCenter`: Storage positions, temperature zones, inbound/outbound dock capacity.
3. **`Item` (Material, Component, Product):** Hierarchical items. Attributes: SKU, standard cost, shelf-life duration, hazard class, compliance requirements.
4. **`InventoryPosition` & `InventoryLot`:** Physical stock at a facility. Tracks: `quantityOnHand`, `quantityReserved`, `quantityAvailable`, `lotNumber`, `manufacturingDate`, `expiryDate`, `qualityStatus` (`RELEASED`, `QUARANTINED`, `REJECTED`).
5. **`CapacityResource`:** Finite resources. E.g., Fab Stepper machine hours, dry-room square meters, packaging line shifts, loading dock throughput.

### 2.2 Graph Edge Entities ($E$)
1. **`SupplyEdge`:** Directed relationship from Supplier to Facility for a specific Item. Stores negotiated minimum order quantities (MOQ), standard lead time, contracted capacity, and unit cost.
2. **`BillOfMaterialsEdge` (BOM):** Directed dependency from precursor Material/Component to parent Assembly. Stores consumption multiplier, scrap rate, and assembly stage sequence.
3. **`ProductionStageEdge`:** Precedence graph between manufacturing phases (e.g., Wafer Fab -> Dicing -> Wire Bonding -> Packaging -> Final Test).
4. **`TransportRouteEdge`:** Directed shipping lane between facilities. Stores transit mode (Air, Sea, Road, Rail), transit duration distribution, carrier ID, cost per kg, and emissions factor.

---

## 3. Disruption & Incident Aggregate

```java
public record DisruptionIncident(
    IncidentId id,
    WorkspaceId workspaceId,
    String title,
    DisruptionType type,
    SeverityLevel severity,
    EntityReference affectedEntity, // e.g., Supplier, Route, Facility
    Instant detectedAt,
    Instant estimatedStartTime,
    Duration estimatedDuration,
    DisruptionStatus status,
    DataProvenance provenance,
    List<EvidenceAttachment> evidence
) {
    // Domain methods: acknowledge(), startInvestigation(), attachImpact(), resolve()
}
```

### 3.1 Downstream Cascading Impact Traversal
When a disruption is attached to node $V_{disrupted}$:
1. **Upstream/Downstream Breadth-First / Depth-First Traversal:**
   $$\text{Impacted}(V) = \{ v \in V \mid \text{path}(V_{disrupted}, v) \text{ exists in } G \}$$
2. **Runway Calculation ($R_{runway}$):**
   $$R_{runway} = \frac{\text{Available Inventory at Facility}}{\text{Daily Consumption Rate from Active Orders}}$$
3. **Time-to-Impact ($T_{impact}$):** If $T_{impact} < R_{runway}$, immediate mitigation is required before assembly line starvation occurs.

---

## 4. Recovery & Planning Aggregates

### 4.1 `ActionIntent` & `ResourceClaim`
The fundamental unit of agent proposals and ConcurGuard arbitration:

```java
public record ActionIntent(
    IntentId id,
    ProposalId proposalId,
    AgentIdentifier proposingAgent,
    IncidentId incidentId,
    ActionType actionType, // EXPEDITE_SHIPMENT, PROCURE_ALTERNATIVE, RESCHEDULE_PRODUCTION, SPLIT_BATCH
    EntityReference targetEntity,
    List<ResourceClaim> resourceClaims,
    TimeWindow plannedTimeWindow,
    Long expectedStateVersion,
    List<Precondition> preconditions,
    List<IntentId> dependencies,
    BigDecimal estimatedCost,
    boolean requiresHumanApproval
) {}

public record ResourceClaim(
    ResourceId resourceId,
    ResourceType type, // INVENTORY, MACHINE_CAPACITY, DOCK_BAY, TRANSPORT_LANE
    Quantity claimedQuantity,
    TimeWindow claimWindow,
    ClaimPriority priority
) {}
```

### 4.2 `ConflictRecord` & `ArbitrationDecision`
- **`ConflictRecord`:** Details the exact violation (e.g., "Quantity claim of 500 kg Lithium Carbonate exceeds available unreserved stock of 200 kg at Facility-A in window $[T_1, T_2)$").
- **`ArbitrationDecision`:** Outcome (`ALLOW`, `DELAY`, `RESCHEDULE`, `MODIFY`, `REJECT`, `REQUEST_REPLANNING`), applied modifications, and cryptographic audit hash.

---

## 5. Domain Invariants Checklist

1. **Non-Negative Inventory:** $\text{QuantityOnHand} - \text{QuantityReserved} \ge 0$ at all times.
2. **Unit Conversion Coherence:** Arithmetic between different `UnitOfMeasure` instances is illegal without an explicit conversion factor in the registry.
3. **BOM Acyclicity:** The Bill-of-Materials dependency graph must be a strict Directed Acyclic Graph (DAG). Cycles trigger a domain `CircularDependencyException`.
4. **State Staleness Protection:** An intent whose `expectedStateVersion` is strictly less than current `stateVersion` must fail validation with `StaleStateVersionException`.
5. **Quarantine Invariant:** Any inventory lot in `QUARANTINED` or `EXPIRED` status cannot be claimed for production or shipment.

