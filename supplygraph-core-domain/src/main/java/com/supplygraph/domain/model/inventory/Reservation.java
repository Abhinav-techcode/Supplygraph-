package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;

import java.time.Instant;
import java.util.Objects;

/**
 * Explicit reservation allocating on-hand inventory to a specific production order or outbound shipment.
 */
public class Reservation {
    public enum Status {
        ACTIVE,
        FULFILLED,
        RELEASED,
        EXPIRED
    }

    private final ReservationId id;
    private final InventoryAccountId sourceAccount;
    private final InventoryAccountId reservedAccount;
    private final Quantity reservedQuantity;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Status status;

    public Reservation(ReservationId id, InventoryAccountId sourceAccount, InventoryAccountId reservedAccount,
                       Quantity reservedQuantity, Instant createdAt, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "ReservationId must not be null");
        this.sourceAccount = Objects.requireNonNull(sourceAccount, "Source account must not be null");
        this.reservedAccount = Objects.requireNonNull(reservedAccount, "Reserved account must not be null");
        if (sourceAccount.category() != BalanceCategory.ON_HAND_AVAILABLE) {
            throw new IllegalArgumentException("Reservations must originate from ON_HAND_AVAILABLE");
        }
        if (reservedAccount.category() != BalanceCategory.RESERVED_PRODUCTION &&
                reservedAccount.category() != BalanceCategory.RESERVED_SHIPMENT) {
            throw new IllegalArgumentException("Reserved account must be RESERVED_PRODUCTION or RESERVED_SHIPMENT");
        }
        this.reservedQuantity = Objects.requireNonNull(reservedQuantity, "Quantity must not be null");
        if (!reservedQuantity.isPositive()) {
            throw new IllegalArgumentException("Reservation quantity must be strictly positive");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt must not be null");
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration must not be before creation time");
        }
        this.status = Status.ACTIVE;
    }

    public ReservationId getId() {
        return id;
    }

    public InventoryAccountId getSourceAccount() {
        return sourceAccount;
    }

    public InventoryAccountId getReservedAccount() {
        return reservedAccount;
    }

    public Quantity getReservedQuantity() {
        return reservedQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isActive(Instant asOf) {
        if (status != Status.ACTIVE) {
            return false;
        }
        return asOf.isBefore(expiresAt);
    }

    public void markFulfilled() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Cannot fulfill non-active reservation: " + status);
        }
        this.status = Status.FULFILLED;
    }

    public void markReleased() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Cannot release non-active reservation: " + status);
        }
        this.status = Status.RELEASED;
    }

    public void markExpired() {
        if (status == Status.ACTIVE) {
            this.status = Status.EXPIRED;
        }
    }
}

