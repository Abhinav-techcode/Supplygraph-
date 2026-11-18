# SupplyGraph — Java Collection Framework Usage & Architecture Standards

**Document Version:** 1.0.0  
**Author:** Principal Enterprise Software Architect & Staff Java Engineer  
**Repository:** `/Users/abhinavgupta/Desktop/Supplygraph`  
**Target Platform:** Java 21 LTS  

---

## 1. Core Principles

SupplyGraph enforces strict architectural rules regarding collection selection, mutability, concurrency, and performance:

1. **Defensive Immutability:** All domain aggregate roots and value objects expose unmodifiable views via `Collections.unmodifiableList()`, `Collections.unmodifiableMap()`, `Collections.unmodifiableSet()`, `List.copyOf()`, or Java 21 `Set.copyOf()`.
2. **Specialized Type-Safe Collections:** Whenever keys or elements are enums, `EnumMap` and `EnumSet` MUST be used instead of `HashMap` or `HashSet`.
3. **Deterministic Order Preservation:** In simulation, replanning, and serialization workflows where reproducible behavior is required, `LinkedHashMap` and `LinkedHashSet` are mandatory over non-deterministic hash structures.
4. **Natural Ordering & Range Operations:** For cryptographic hashing, sorted parameters, and chronological event timelines, `TreeMap` and `TreeSet` are employed.
5. **Lock-Free Concurrency:** In application services and caching layers shared across concurrent threads, `ConcurrentHashMap` with atomic compute methods (`computeIfAbsent`, `merge`) is mandated.
6. **Graph Traversal & Work Queues:** For BFS/DFS graph traversals and dependency resolution, `ArrayDeque` is chosen over `LinkedList` or `Stack` due to superior cache locality and zero node-allocation overhead.

---

## 2. Inventory of Specialized Collections Used in Codebase

### 2.1 `EnumMap<K, V>`
- **Justification:** Backed by compact primitive arrays indexed by `Enum.ordinal()`. Zero hash collisions, extremely high CPU cache line efficiency, and negligible memory footprint.
- **Key Codebase Usages:**
  - `PostgresDataQualityPersistenceAdapter`: Maps `QualityDimension` to quality scores (`Map<QualityDimension, Double> dimensionScores = new EnumMap<>(QualityDimension.class)`).
  - `DataQualityEngineService`: Dimension scoring and penalty weight accumulation.
  - `PostgresOutboxPersistenceAdapter`: Outbox event metrics mapped by `OutboxStatus`.
  - `CarbonAccountingService`: Scope 1, 2, and 3 emission totals by `EmissionScope`.
  - `SupplierSustainabilityService`: ESG scoring by `EsgPillar` (ENVIRONMENTAL, SOCIAL, GOVERNANCE).
  - `MultiEchelonInventoryService`: Safety stock allocations indexed by `EchelonTier`.

### 2.2 `EnumSet<E>`
- **Justification:** Backed by a bit-vector (`long` bitmask for enums with up to 64 elements). Set operations (union, intersection, contains) execute as single-cycle bitwise CPU instructions.
- **Key Codebase Usages:**
  - `UserRole`: Maps each role to its constituent permissions (`EnumSet.allOf(Permission.class)`, `EnumSet.of(...)`).
  - `SandboxExecutionAdapter`: Supported task types (`EnumSet.allOf(TaskType.class)`).
  - `ConcurGuard`: Conflict detection strategy masks and violation flags.
  - `FieldAuthorityPolicy`: Authorized mirror sources and sync scopes.
  - `ApiKeyRecord` & `ApiDeveloperCentreService`: Permitted security scopes (`EnumSet.of(ApiScope.READ_TWIN, ...)`).

### 2.3 `LinkedHashMap<K, V>` and `LinkedHashSet<E>`
- **Justification:** Maintains doubly-linked iteration ordering while retaining $O(1)$ amortized lookups. Guarantees deterministic iteration in simulation, graph traversals, and JSON/CSV ingestion.
- **Key Codebase Usages:**
  - `JsonDataParser` & `CsvDataParser`: Retains source column ordering for consistent schema mapping and data quality inspection.
  - `IntentDependencyGraph`: Preserves topological insertion ordering of action intents during selective replanning.
  - `ConcurGuardArbitrator`: Preserves priority-evaluated arbitration decisions.
  - `MultiObjectiveParetoService`: Maintains non-dominated frontier solutions in sorted rank order.
  - `GenealogyDAG`: Preserves parent-child lot lineage in topological order.

### 2.4 `TreeMap<K, V>` and `TreeSet<E>`
- **Justification:** Red-Black tree structure guaranteeing $O(\log n)$ performance and strict natural or comparator-based sorting.
- **Key Codebase Usages:**
  - `PostgresExecutionPersistenceAdapter`: Canonically sorts request parameters alphabetically before computing HMAC signatures for tamper prevention.
  - `SensitivityAnalysisService`: Sorts trial variations by deviation percentage.
  - `ChronologicalEventLedger`: Maintains time-ordered execution audit timeline.

### 2.5 `ConcurrentHashMap<K, V>`
- **Justification:** Thread-safe, highly concurrent bucket-level synchronization with lock-free volatile reads.
- **Key Codebase Usages:**
  - `PostgresDigitalTwinRepositoryAdapter`: Caches in-memory digital twin states per `WorkspaceId`.
  - `TokenBucketRateLimiter`: Per-IP/client token buckets for rate limiting.
  - `SandboxExecutionAdapter`: Atomic execution tracking counters and task status caches across concurrent worker threads.

### 2.6 `ArrayDeque<E>`
- **Justification:** Resizable array-backed double-ended queue. Unlike `LinkedList`, it has no pointer-chasing overhead and does not allocate a new `Node` wrapper object per element.
- **Key Codebase Usages:**
  - `IntentDependencyGraph`: BFS propagation queue for downstream intent invalidation.
  - `GenealogyDAG`: Bidirectional lot tracing queue (backward from finished goods to mines; forward from tainted batches to customers).
  - `TaskGraph`: Topological sorting and dependency resolution queue.

### 2.7 `PriorityQueue<E>`
- **Justification:** Binary heap providing $O(\log n)$ enqueue and $O(1)$ peek of minimum/maximum elements.
- **Key Codebase Usages:**
  - `DiscreteEventSimulationEngine`: Future event calendar sorted chronologically by simulated timestamp.
  - `ProductionPlanningService`: Earliest Due Date (EDD) dispatch queue for finite work center scheduling.

---

## 3. Immutability & Defensive Copying Patterns

Throughout `supplygraph-core-domain`, all records and domain entities enforce defensive copying on ingress and return unmodifiable collections on egress:

```java
// Pattern 1: Constructor defensive copy
public record BillOfMaterials(
        BomId id,
        PartNumber finishedGood,
        List<BomItem> items
) {
    public BillOfMaterials {
        Objects.requireNonNull(id, "BOM id cannot be null");
        Objects.requireNonNull(finishedGood, "Finished good cannot be null");
        // Defensive copy ensuring immutability of encapsulated state
        items = List.copyOf(items);
    }
}

// Pattern 2: EnumMap initialization with unmodifiable view
public Map<QualityDimension, Double> getDimensionScores() {
    return Collections.unmodifiableMap(this.dimensionScores);
}
```

---

## 4. Verification & Testing

Collection invariants are verified by automated tests:
1. Attempting to mutate an unmodifiable collection throws `UnsupportedOperationException`.
2. Iteration order tests verify that `LinkedHashMap` and `TreeMap` maintain required sequences across serializations.
3. Thread-safety tests verify that `ConcurrentHashMap` instances handle concurrent updates without race conditions.

