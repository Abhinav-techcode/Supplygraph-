package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.time.Instant;
import java.util.*;

/**
 * Authoritative double-entry inventory ledger.
 * Guarantees quantity conservation, non-negative balances, lot traceability,
 * explicit reservation lifecycles, and idempotent transaction processing.
 */
public class InventoryLedger {
    private final Map<InventoryAccountId, Quantity> balances = new LinkedHashMap<>();
    private final Map<TransactionId, InventoryTransaction> transactionHistory = new LinkedHashMap<>();
    private final Map<LotNumber, InventoryLot> lotRegistry = new LinkedHashMap<>();
    private final Map<ReservationId, Reservation> reservations = new LinkedHashMap<>();

    public InventoryLedger() {
    }

    public synchronized void registerLot(InventoryLot lot) {
        Objects.requireNonNull(lot, "Lot must not be null");
        lotRegistry.put(lot.lotNumber(), lot);
    }

    public Optional<InventoryLot> findLot(LotNumber lotNumber) {
        return Optional.ofNullable(lotRegistry.get(lotNumber));
    }

    public synchronized Quantity getBalance(InventoryAccountId accountId, UnitOfMeasure fallbackUnit) {
        Quantity current = balances.get(accountId);
        if (current == null) {
            return Quantity.zero(fallbackUnit);
        }
        return current;
    }

    public synchronized Map<InventoryAccountId, Quantity> getAllBalances() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(balances));
    }

    public synchronized List<InventoryTransaction> getTransactionHistory() {
        return List.copyOf(transactionHistory.values());
    }

    public synchronized Optional<InventoryTransaction> findTransaction(TransactionId id) {
        return Optional.ofNullable(transactionHistory.get(id));
    }

    /**
     * Atomically posts a balanced transaction to the ledger.
     * Idempotency guarantee: If transactionId was previously posted, operation returns silently.
     */
    public synchronized boolean postTransaction(InventoryTransaction transaction) {
        Objects.requireNonNull(transaction, "Transaction must not be null");

        // Idempotency check:
        if (transactionHistory.containsKey(transaction.id())) {
            return false; // Already processed
        }

        // Pre-validation pass: Verify balances and lot conditions before mutating any state
        Map<InventoryAccountId, Quantity> tentativeBalances = new HashMap<>(balances);

        for (InventoryPosting posting : transaction.postings()) {
            InventoryAccountId acc = posting.accountId();
            Quantity current = tentativeBalances.getOrDefault(acc, Quantity.zero(posting.quantity().unit()));

            // Verify lot expiration constraints
            if (acc.lotNumber().isPresent() && acc.category() == BalanceCategory.ON_HAND_AVAILABLE) {
                InventoryLot lot = lotRegistry.get(acc.lotNumber().get());
                if (lot != null && lot.isExpired(transaction.timestamp())) {
                    throw new IllegalStateException("Cannot debit/credit expired lot into ON_HAND_AVAILABLE: " + lot.lotNumber());
                }
            }

            if (posting.postingType() == PostingType.CREDIT) {
                // Internal accounts must not go negative
                if (acc.category() != BalanceCategory.EXTERNAL_OFFSET) {
                    if (current.compareTo(posting.quantity()) < 0) {
                        throw new IllegalStateException("Insufficient inventory on account " + acc +
                                ". Available: " + current + ", required credit: " + posting.quantity());
                    }
                }
                tentativeBalances.put(acc, current.subtract(posting.quantity()));
            } else {
                tentativeBalances.put(acc, current.add(posting.quantity()));
            }
        }

        // Apply state mutation atomically
        balances.putAll(tentativeBalances);
        transactionHistory.put(transaction.id(), transaction);
        return true;
    }

    /**
     * Creates an atomic reservation moving inventory from ON_HAND_AVAILABLE to RESERVED_*.
     */
    public synchronized Reservation createReservation(
            ReservationId reservationId,
            InventoryAccountId sourceAccount,
            InventoryAccountId reservedAccount,
            Quantity quantity,
            Instant now,
            Instant expiresAt
    ) {
        Reservation reservation = new Reservation(reservationId, sourceAccount, reservedAccount, quantity, now, expiresAt);

        TransactionId txId = TransactionId.of("TX-RES-" + reservationId.value());
        InventoryTransaction tx = InventoryTransaction.transfer(
                txId,
                now,
                "RESERVATION_ALLOCATION",
                reservationId.value(),
                sourceAccount,
                reservedAccount,
                quantity
        );

        postTransaction(tx);
        reservations.put(reservationId, reservation);
        return reservation;
    }

    /**
     * Releases an active reservation, moving inventory back to ON_HAND_AVAILABLE.
     */
    public synchronized void releaseReservation(ReservationId reservationId, Instant now) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new NoSuchElementException("Reservation not found: " + reservationId);
        }
        reservation.markReleased();

        TransactionId txId = TransactionId.of("TX-REL-" + reservationId.value());
        InventoryTransaction tx = InventoryTransaction.transfer(
                txId,
                now,
                "RESERVATION_RELEASE",
                reservationId.value(),
                reservation.getReservedAccount(),
                reservation.getSourceAccount(),
                reservation.getReservedQuantity()
        );

        postTransaction(tx);
    }

    /**
     * Fulfills an active reservation upon shipment or production consumption.
     */
    public synchronized void fulfillReservation(ReservationId reservationId, InventoryAccountId consumptionOffsetAccount, Instant now) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new NoSuchElementException("Reservation not found: " + reservationId);
        }
        reservation.markFulfilled();

        TransactionId txId = TransactionId.of("TX-FUL-" + reservationId.value());
        InventoryTransaction tx = InventoryTransaction.transfer(
                txId,
                now,
                "RESERVATION_FULFILLMENT",
                reservationId.value(),
                reservation.getReservedAccount(),
                consumptionOffsetAccount,
                reservation.getReservedQuantity()
        );

        postTransaction(tx);
    }

    public synchronized Optional<Reservation> findReservation(ReservationId id) {
        return Optional.ofNullable(reservations.get(id));
    }
}

