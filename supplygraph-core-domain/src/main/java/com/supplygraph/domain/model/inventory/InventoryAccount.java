package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;

/**
 * Double-entry ledger account.
 */
public class InventoryAccount {
    private final InventoryAccountId id;
    private final String description;

    public InventoryAccount(InventoryAccountId id, String description) {
        this.id = Objects.requireNonNull(id, "AccountId must not be null");
        this.description = description;
    }

    public InventoryAccountId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryAccount that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InventoryAccount{" +
                "id=" + id +
                ", desc='" + description + '\'' +
                '}';
    }
}

