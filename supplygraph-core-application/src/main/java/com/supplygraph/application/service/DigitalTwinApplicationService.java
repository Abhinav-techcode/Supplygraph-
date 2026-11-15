package com.supplygraph.application.service;

import com.supplygraph.application.port.in.DigitalTwinUseCase;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.capacity.CapacityReservation;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.InventoryPosting;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.DomainEvent;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Application service orchestrating digital twin commands and transactional persistence boundaries.
 */
@Service
public class DigitalTwinApplicationService implements DigitalTwinUseCase {

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final InventoryLedgerRepositoryPort inventoryRepository;

    public DigitalTwinApplicationService(
            DigitalTwinRepositoryPort digitalTwinRepository,
            InventoryLedgerRepositoryPort inventoryRepository
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepository must not be null");
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "InventoryRepository must not be null");
    }

    @Override
    @Transactional
    public DigitalTwinState createDigitalTwin(WorkspaceId workspaceId, SupplyChainGraph graph, BillOfMaterials bom) {
        if (digitalTwinRepository.existsByWorkspaceId(workspaceId)) {
            throw new IllegalStateException("Digital twin already exists for workspace: " + workspaceId);
        }
        DigitalTwinState state = new DigitalTwinState(workspaceId, StateVersion.initial(), graph, bom);
        digitalTwinRepository.save(state);
        return state;
    }

    @Override
    @Transactional
    public boolean recordInventoryTransaction(WorkspaceId workspaceId, InventoryTransaction transaction, DomainEvent triggerEvent) {
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found for workspace: " + workspaceId));

        boolean applied = state.postInventoryTransaction(transaction, triggerEvent);
        if (applied) {
            digitalTwinRepository.save(state);
            inventoryRepository.recordTransaction(workspaceId, transaction);
            for (InventoryPosting p : transaction.postings()) {
                var bal = state.getInventoryLedger().getBalance(p.accountId(), p.quantity().unit());
                inventoryRepository.updateBalance(workspaceId, p.accountId(), bal);
            }
        }
        return applied;
    }

    @Override
    @Transactional
    public boolean reserveCapacity(WorkspaceId workspaceId, CapacityReservation reservation, DomainEvent triggerEvent, boolean allowOverbooking) {
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found for workspace: " + workspaceId));

        boolean reserved = state.bookCapacityReservation(reservation, triggerEvent, allowOverbooking);
        if (reserved) {
            digitalTwinRepository.save(state);
        }
        return reserved;
    }

    @Override
    @Transactional(readOnly = true)
    public DigitalTwinSnapshot getSnapshot(WorkspaceId workspaceId) {
        DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found for workspace: " + workspaceId));
        return state.createSnapshot();
    }
}

