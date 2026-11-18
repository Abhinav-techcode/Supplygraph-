# Graph Architecture Assessment

Evidence date 2026-11-18. Code read: `domain.model.graph.*`, `DigitalTwinState`, `PostgresDigitalTwinRepositoryAdapter`, `ImpactAnalysisApplicationService`, `SupplierIntelligenceService`, `MaterialIntelligenceService`, `DigitalTwinNetworkGraphComponent`, `DigitalTwinGraphExplorerView`, `HomeView`, `DemoDataInitializer`. Browser: `/`, `/digital-twin`.

## 1. What exists

### 1.1 Domain graph (core-domain) — sound but narrow
* `SupplyChainGraph`: `Map<NodeId, SupplyChainNode>`, `Map<EdgeId, TransportationLane>`, outgoing/incoming adjacency, validated on construction (duplicate ids, dangling lane ends rejected). Operations: direct predecessors/successors, `getUpstreamReachableNodes`, `getDownstreamReachableNodes`, `findAllPaths` (584 lines across the package; `SupplyChainGraphTest`).
* **Node** = `SupplyChainNode` subtype: `Supplier`, `ManufacturingSite`, `Warehouse`, `LogisticsHub` (`NodeType` enum has exactly these four). A node is a *facility/organisation*.
* **Edge** = `TransportationLane` with a `TransportMode` and transit time/cost. An edge is a *physical logistics lane*.
* **BOM** is a separate structure (`BillOfMaterials`, `BomEntry`, `Material`, `Component`, `FinishedProduct`), not part of the graph. **Inventory** is a ledger (`InventoryLedger`), **capacity** a calendar per `Resource`. Nothing links `MaterialId` to a node in the graph model except identifiers passed by callers.

### 1.2 How it is created and persisted
* Created by `DemoDataInitializer` from ~17 literal nodes and lanes at every start. There is no screen, import or API that creates or edits a supply chain.
* **Not persisted.** `PostgresDigitalTwinRepositoryAdapter` keeps the state in a JVM `ConcurrentHashMap`; the database row is `(workspace_id, version, updated_at)`. After a restart the repository would return an empty graph. `supply_chain_nodes`, `transportation_lanes`, `bom_entries`, `inventory_*` tables exist in V2 but no entity maps them.
* Graph changes are persisted nowhere; the only change path (`DigitalTwinState.updateGraph`) has no caller.

### 1.3 Analysis that is real
* `ImpactAnalysisApplicationService.performImpactAssessment`: starting from the disruption's affected node ids, calls `graph.getDownstreamReachableNodes` and flags downstream facilities — genuine topological propagation using nominal transit times ("deterministic propagation based on active graph topology and nominal transit times"). Single-hop and multi-hop effects are distinguished (direct vs indirect nodes).
* ConcurGuard validates against a `DigitalTwinSnapshot` (version/staleness).

### 1.4 Analysis that is *not* graph intelligence
* "Single source" in `SupplierIntelligenceService` is a heuristic: `alternativesCount == 0 || downstreamConnections > 3`. There is no articulation-point / min-cut / dominator analysis, so true single points of failure are not computed.
* Supplier OTIF and lead-time variance are constants chosen by that boolean (88.5/96.2, 6.5/1.8).
* BOM and logistics are never combined: the product "affected by a supplier disruption" is not derived by walking BOM levels through supplier materials to finished goods; impact is node reachability only. Multi-tier *material* dependency (which component of which product depends on which supplier at which tier) is not modelled in the graph.
* No edge for "supplies material M to node N", no tier attribute on edges, no inventory/time-to-survive propagation on the graph (a separate `AdvancedResilience` service computes TTS/TTR from other inputs).

## 2. The visualization

* `DigitalTwinNetworkGraphComponent` (548 lines) renders an SVG (columns by tier, nodes as boxes, lanes as lines/arrows, filter buttons, inspector panel). It is visually strong (dark canvas, tier columns, red disrupted nodes, mode icons).
* **It does not read the twin.** The class has a no-argument constructor and builds `VisualNode`/`VisualLane` from literals; disrupted state, inventory ("4,200 units"), throughput ("20000 w/m") and "ConcurGuard safety state: CONTENDED" are constants. The explorer page duplicates the topology a second time as `sampleNodes/sampleLanes` tables.
* Consequences: a disruption created or resolved by the user never changes the graph; the Home page and explorer describe it as "LIVE TOPOLOGY"/"real-time health and disruption pings" — a misleading claim; any real twin (imported or modified) would show the wrong picture.
* Fixed layout (column index per tier), no zoom/pan/search/path highlighting, no upstream/downstream trace, no "why is this affected" explanation, no edge selection. The layout cannot scale beyond ~30 nodes.
* Layout bug: on a normal window the section title is squeezed into a narrow left column.

## 3. Verdict

| Question | Answer |
|---|---|
| Suitable domain model for the purpose? | **Partially.** The facility/lane graph is a good base for logistics topology, but the product requires multi-tier *material* dependency (BOM × supplier × site × lane) and single-point-of-failure analysis, neither of which the current model expresses. |
| Persistence suitable? | **No.** An in-memory twin with a version row cannot be the "common source of operational truth". |
| Visualization suitable for enterprise use? | **No** — attractive but fictional (static data). It must be bound to the twin before it can be shown as the product's centre. |

## 4. Recommended graph architecture (for the later implementation phase)

1. **Persist the twin graph**: map nodes, lanes, materials, BOM entries and inventory balances to the existing V2 tables (or a successor), load them at startup, and make `DigitalTwinState` the cache over that store with optimistic versioning. Remove the seed-in-code topology from the production path (keep it as an explicit "load demo supply chain" import).
2. **Extend the model** with a second edge kind: `SUPPLIES(material, qty/period, share)` between supplier/site nodes, alongside `TransportationLane` (physical flow) and BOM (`REQUIRES`) edges; add node attributes tier, country, criticality, capacity.
3. **Add real graph algorithms** in the domain: tiered propagation (BOM explosion down to finished goods and customers), articulation points/single-source detection per material, shortest/alternate path with capacity, time-to-survive using inventory.
4. **One graph read model** feeding every consumer: impact analysis, supplier/material intelligence, ConcurGuard snapshot, agents, optimization candidates, the visualization.
5. **Rebuild the visualization on that read model**: server-computed layout (layered DAG), pan/zoom, search, selection, upstream/downstream highlighting, disruption overlay driven by active disruptions, and a side panel answering "why is this affected" with the actual path and BOM line. Keep the existing SVG styling as the visual language.

Not required: a graph database. At the intended scale a PostgreSQL adjacency model plus in-memory traversal is adequate.
