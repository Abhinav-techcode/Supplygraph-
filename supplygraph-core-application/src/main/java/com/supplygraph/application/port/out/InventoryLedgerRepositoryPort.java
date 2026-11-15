package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryTransaction;

import java.util.Optional;

/**
 * Outbound repository port for persisting inventory transactions and balances.
 */
public interface InventoryLedgerRepositoryPort {
    void recordTransaction(WorkspaceId workspaceId, InventoryTransaction transaction);
    void updateBalance(WorkspaceId workspaceId, InventoryAccountId accountId, Quantity balance);
    Optional<Quantity> findBalance(WorkspaceId workspaceId, InventoryAccountId accountId);
}

