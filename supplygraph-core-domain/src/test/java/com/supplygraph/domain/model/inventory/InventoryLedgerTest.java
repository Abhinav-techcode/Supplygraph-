package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryLedgerTest {

    private InventoryLedger ledger;
    private NodeId warehouseA;
    private NodeId warehouseB;
    private MaterialId cathodePowder;
    private InventoryAccountId onHandA;
    private InventoryAccountId onHandB;
    private InventoryAccountId externalOffset;

    @BeforeEach
    void setUp() {
        ledger = new InventoryLedger();
        warehouseA = NodeId.of("WH-A");
        warehouseB = NodeId.of("WH-B");
        cathodePowder = MaterialId.of("MAT-NMC");

        onHandA = InventoryAccountId.of(warehouseA, cathodePowder, BalanceCategory.ON_HAND_AVAILABLE);
        onHandB = InventoryAccountId.of(warehouseB, cathodePowder, BalanceCategory.ON_HAND_AVAILABLE);
        externalOffset = InventoryAccountId.of(NodeId.of("SUPPLIER-CLEARING"), cathodePowder, BalanceCategory.EXTERNAL_OFFSET);
    }

    @Test
    @DisplayName("Transaction enforces strict conservation: Total debits must equal total credits")
    void shouldEnforceConservationOfQuantity() {
        // Attempt an unbalanced transaction: debit 100 kg, credit 50 kg
        InventoryPosting debit = InventoryPosting.debit(onHandA, Quantity.of(100, UnitOfMeasure.KILOGRAM));
        InventoryPosting credit = InventoryPosting.credit(externalOffset, Quantity.of(50, UnitOfMeasure.KILOGRAM));

        assertThatThrownBy(() -> new InventoryTransaction(
                TransactionId.of("TX-UNBALANCED"), Instant.now(), "PURCHASE", "PO-1", List.of(debit, credit)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conservation violation: Total debits");
    }

    @Test
    @DisplayName("Transfer preserves quantity and updates accounts atomically")
    void shouldTransferInventoryConservingQuantity() {
        Instant now = Instant.now();

        // 1. Initial Receipt: 500 KG into Warehouse A
        InventoryTransaction receipt = InventoryTransaction.transfer(
                TransactionId.of("TX-INIT"), now, "RECEIPT", "PO-101",
                externalOffset, onHandA, Quantity.of(500, UnitOfMeasure.KILOGRAM));
        ledger.postTransaction(receipt);

        assertThat(ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("500.0000");

        // 2. Transfer: 200 KG from Warehouse A to Warehouse B
        InventoryTransaction transfer = InventoryTransaction.transfer(
                TransactionId.of("TX-TRF-01"), now.plus(1, ChronoUnit.HOURS), "TRANSFER", "TRF-01",
                onHandA, onHandB, Quantity.of(200, UnitOfMeasure.KILOGRAM));
        ledger.postTransaction(transfer);

        // Verify balances
        assertThat(ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("300.0000");
        assertThat(ledger.getBalance(onHandB, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("200.0000");

        // Total inventory in system: 300 + 200 = 500 KG (Exact conservation!)
        Quantity totalInSystem = ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM)
                .add(ledger.getBalance(onHandB, UnitOfMeasure.KILOGRAM));
        assertThat(totalInSystem.amount()).isEqualByComparingTo("500.0000");
    }

    @Test
    @DisplayName("Posting prevents internal balances from going negative")
    void shouldPreventNegativeAvailableInventory() {
        // Initial inventory is 0. Attempting to transfer 100 KG should fail.
        InventoryTransaction transfer = InventoryTransaction.transfer(
                TransactionId.of("TX-OVERDRAW"), Instant.now(), "TRANSFER", "TRF-ERR",
                onHandA, onHandB, Quantity.of(100, UnitOfMeasure.KILOGRAM));

        assertThatThrownBy(() -> ledger.postTransaction(transfer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient inventory on account");
    }

    @Test
    @DisplayName("Expired lot cannot be posted to ON_HAND_AVAILABLE")
    void shouldRejectExpiredLotIntoAvailableInventory() {
        LotNumber expiredLotNum = LotNumber.of("LOT-EXPIRED-99");
        ExpirationDate expiredDate = ExpirationDate.of(LocalDate.now().minusDays(5));
        InventoryLot expiredLot = InventoryLot.of(
                expiredLotNum, cathodePowder, Instant.now().minus(30, ChronoUnit.DAYS), expiredDate, "VEND-1");
        ledger.registerLot(expiredLot);

        InventoryAccountId lotAccount = InventoryAccountId.of(
                warehouseA, cathodePowder, BalanceCategory.ON_HAND_AVAILABLE, expiredLotNum);

        InventoryTransaction receipt = InventoryTransaction.transfer(
                TransactionId.of("TX-EXP-RECEIPT"), Instant.now(), "RECEIPT", "PO-EXP",
                externalOffset, lotAccount, Quantity.of(50, UnitOfMeasure.KILOGRAM));

        assertThatThrownBy(() -> ledger.postTransaction(receipt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot debit/credit expired lot into ON_HAND_AVAILABLE");
    }

    @Test
    @DisplayName("Reservation lifecycle allocates, locks, and releases inventory atomically")
    void shouldManageReservationLifecycle() {
        Instant now = Instant.now();

        // Initial deposit: 100 KG into ON_HAND_AVAILABLE
        ledger.postTransaction(InventoryTransaction.transfer(
                TransactionId.of("TX-DEP"), now, "RECEIPT", "PO-1",
                externalOffset, onHandA, Quantity.of(100, UnitOfMeasure.KILOGRAM)));

        // Create Reservation: Reserve 40 KG for production
        InventoryAccountId reservedProd = InventoryAccountId.of(
                warehouseA, cathodePowder, BalanceCategory.RESERVED_PRODUCTION);
        ReservationId resId = ReservationId.of("RES-BATCH-001");
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        Reservation res = ledger.createReservation(
                resId, onHandA, reservedProd, Quantity.of(40, UnitOfMeasure.KILOGRAM), now, expiresAt);

        assertThat(res.getStatus()).isEqualTo(Reservation.Status.ACTIVE);
        assertThat(ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("60.0000");
        assertThat(ledger.getBalance(reservedProd, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("40.0000");

        // Release Reservation
        ledger.releaseReservation(resId, now.plus(1, ChronoUnit.DAYS));

        assertThat(res.getStatus()).isEqualTo(Reservation.Status.RELEASED);
        assertThat(ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("100.0000");
        assertThat(ledger.getBalance(reservedProd, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("Transaction posting is idempotent: Reposting same transaction ID causes no double-spend")
    void shouldBeIdempotentOnTransactionPosting() {
        Instant now = Instant.now();
        InventoryTransaction tx = InventoryTransaction.transfer(
                TransactionId.of("TX-IDEMPOTENT-1"), now, "RECEIPT", "PO-1",
                externalOffset, onHandA, Quantity.of(100, UnitOfMeasure.KILOGRAM));

        boolean first = ledger.postTransaction(tx);
        boolean second = ledger.postTransaction(tx);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(ledger.getBalance(onHandA, UnitOfMeasure.KILOGRAM).amount()).isEqualByComparingTo("100.0000");
    }
}

