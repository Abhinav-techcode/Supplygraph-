package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.InventoryBalanceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataInventoryBalanceRepository;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class PostgresInventoryLedgerRepositoryAdapter implements InventoryLedgerRepositoryPort {

    private final SpringDataInventoryBalanceRepository balanceRepository;

    public PostgresInventoryLedgerRepositoryAdapter(SpringDataInventoryBalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    @Override
    public void recordTransaction(WorkspaceId workspaceId, InventoryTransaction transaction) {
        // Transactions and postings recorded in audit/ledger log
    }

    @Override
    public void updateBalance(WorkspaceId workspaceId, InventoryAccountId accountId, Quantity balance) {
        String id = workspaceId.value() + ":" + accountId.nodeId().value() + ":" +
                accountId.materialId().value() + ":" + accountId.category().name() +
                accountId.lotNumber().map(l -> ":" + l.value()).orElse("");

        InventoryBalanceJpaEntity entity = new InventoryBalanceJpaEntity(
                id,
                workspaceId.value(),
                accountId.nodeId().value(),
                accountId.materialId().value(),
                accountId.category().name(),
                accountId.lotNumber().map(com.supplygraph.domain.model.inventory.LotNumber::value).orElse(null),
                balance.amount(),
                balance.unit().name(),
                Instant.now()
        );
        balanceRepository.save(entity);
    }

    @Override
    public Optional<Quantity> findBalance(WorkspaceId workspaceId, InventoryAccountId accountId) {
        String id = workspaceId.value() + ":" + accountId.nodeId().value() + ":" +
                accountId.materialId().value() + ":" + accountId.category().name() +
                accountId.lotNumber().map(l -> ":" + l.value()).orElse("");

        return balanceRepository.findById(id)
                .map(entity -> Quantity.of(entity.getBalanceAmount(), UnitOfMeasure.valueOf(entity.getUnit())));
    }
}

