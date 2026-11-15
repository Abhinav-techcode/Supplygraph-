package com.supplygraph.domain.model.twin;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.capacity.CapacityReservation;
import com.supplygraph.domain.model.capacity.Resource;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.InventoryLedger;
import com.supplygraph.domain.model.inventory.InventoryTransaction;

import java.time.Instant;
import java.util.*;

/**
 * Authoritative Aggregate Root representing the complete Digital Twin state.
 * Enforces monotonic version progression, event deduplication, and deterministic state transitions.
 */
public class DigitalTwinState {
    private final WorkspaceId workspaceId;
    private StateVersion currentVersion;
    private SupplyChainGraph graph;
    private BillOfMaterials bom;
    private final InventoryLedger inventoryLedger;
    private final Map<ResourceId, Resource> resources;
    private final Set<String> processedEventIds;
    private final List<StateTransition> transitionHistory;

    public DigitalTwinState(WorkspaceId workspaceId, StateVersion initialVersion,
                            SupplyChainGraph initialGraph, BillOfMaterials initialBom) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        this.currentVersion = initialVersion == null ? StateVersion.initial() : initialVersion;
        this.graph = initialGraph == null ? SupplyChainGraph.empty() : initialGraph;
        this.bom = initialBom == null ? BillOfMaterials.empty() : initialBom;
        this.inventoryLedger = new InventoryLedger();
        this.resources = new LinkedHashMap<>();
        this.processedEventIds = new HashSet<>();
        this.transitionHistory = new ArrayList<>();
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public synchronized StateVersion getCurrentVersion() {
        return currentVersion;
    }

    public synchronized SupplyChainGraph getGraph() {
        return graph;
    }

    public synchronized BillOfMaterials getBom() {
        return bom;
    }

    public InventoryLedger getInventoryLedger() {
        return inventoryLedger;
    }

    public synchronized Collection<Resource> getResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    public synchronized Optional<Resource> findResource(ResourceId id) {
        return Optional.ofNullable(resources.get(id));
    }

    public synchronized void registerResource(Resource resource) {
        Objects.requireNonNull(resource, "Resource must not be null");
        resources.put(resource.getId(), resource);
    }

    public synchronized boolean isEventProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    public synchronized List<StateTransition> getTransitionHistory() {
        return List.copyOf(transitionHistory);
    }

    /**
     * Atomically updates the network graph with monotonic version increment and event deduplication.
     */
    public synchronized boolean updateGraph(SupplyChainGraph newGraph, DomainEvent triggerEvent) {
        Objects.requireNonNull(newGraph, "New graph must not be null");
        Objects.requireNonNull(triggerEvent, "Trigger event must not be null");

        if (processedEventIds.contains(triggerEvent.eventId())) {
            return false; // Event deduplication
        }

        StateVersion fromVersion = this.currentVersion;
        StateVersion toVersion = fromVersion.next();

        this.graph = newGraph;
        this.currentVersion = toVersion;
        this.processedEventIds.add(triggerEvent.eventId());
        this.transitionHistory.add(new StateTransition(fromVersion, toVersion, triggerEvent, Instant.now()));
        return true;
    }

    /**
     * Atomically updates the Bill of Materials with monotonic version increment and event deduplication.
     */
    public synchronized boolean updateBom(BillOfMaterials newBom, DomainEvent triggerEvent) {
        Objects.requireNonNull(newBom, "New BOM must not be null");
        Objects.requireNonNull(triggerEvent, "Trigger event must not be null");

        if (processedEventIds.contains(triggerEvent.eventId())) {
            return false; // Event deduplication
        }

        StateVersion fromVersion = this.currentVersion;
        StateVersion toVersion = fromVersion.next();

        this.bom = newBom;
        this.currentVersion = toVersion;
        this.processedEventIds.add(triggerEvent.eventId());
        this.transitionHistory.add(new StateTransition(fromVersion, toVersion, triggerEvent, Instant.now()));
        return true;
    }

    /**
     * Posts an inventory transaction to the ledger and increments state version.
     */
    public synchronized boolean postInventoryTransaction(InventoryTransaction tx, DomainEvent triggerEvent) {
        Objects.requireNonNull(tx, "Transaction must not be null");
        Objects.requireNonNull(triggerEvent, "Trigger event must not be null");

        if (processedEventIds.contains(triggerEvent.eventId())) {
            return false;
        }

        boolean posted = this.inventoryLedger.postTransaction(tx);
        if (!posted) {
            return false;
        }

        StateVersion fromVersion = this.currentVersion;
        StateVersion toVersion = fromVersion.next();

        this.currentVersion = toVersion;
        this.processedEventIds.add(triggerEvent.eventId());
        this.transitionHistory.add(new StateTransition(fromVersion, toVersion, triggerEvent, Instant.now()));
        return true;
    }

    /**
     * Books a capacity reservation on a resource and increments state version.
     */
    public synchronized boolean bookCapacityReservation(CapacityReservation reservation, DomainEvent triggerEvent, boolean allowOverbooking) {
        Objects.requireNonNull(reservation, "Reservation must not be null");
        Objects.requireNonNull(triggerEvent, "Trigger event must not be null");

        if (processedEventIds.contains(triggerEvent.eventId())) {
            return false;
        }

        Resource res = resources.get(reservation.resourceId());
        if (res == null) {
            throw new NoSuchElementException("Resource not found: " + reservation.resourceId());
        }

        res.getCalendar().bookReservation(reservation, allowOverbooking);

        StateVersion fromVersion = this.currentVersion;
        StateVersion toVersion = fromVersion.next();

        this.currentVersion = toVersion;
        this.processedEventIds.add(triggerEvent.eventId());
        this.transitionHistory.add(new StateTransition(fromVersion, toVersion, triggerEvent, Instant.now()));
        return true;
    }

    /**
     * Produces an immutable point-in-time snapshot.
     */
    public synchronized DigitalTwinSnapshot createSnapshot() {
        return new DigitalTwinSnapshot(
                this.workspaceId,
                this.currentVersion,
                Instant.now(),
                this.graph,
                this.bom,
                this.inventoryLedger.getAllBalances()
        );
    }
}

