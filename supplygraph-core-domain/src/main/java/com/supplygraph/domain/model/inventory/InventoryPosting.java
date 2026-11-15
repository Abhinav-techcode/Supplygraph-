package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;
import java.util.UUID;

/**
 * Individual double-entry ledger posting line.
 */
public record InventoryPosting(
        String postingId,
        InventoryAccountId accountId,
        PostingType postingType,
        Quantity quantity
) {
    public InventoryPosting {
        if (postingId == null || postingId.isBlank()) {
            postingId = UUID.randomUUID().toString();
        }
        Objects.requireNonNull(accountId, "AccountId must not be null");
        Objects.requireNonNull(postingType, "PostingType must not be null");
        Objects.requireNonNull(quantity, "Quantity must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Posting quantity must be strictly positive: " + quantity);
        }
    }

    public static InventoryPosting debit(InventoryAccountId account, Quantity quantity) {
        return new InventoryPosting(UUID.randomUUID().toString(), account, PostingType.DEBIT, quantity);
    }

    public static InventoryPosting credit(InventoryAccountId account, Quantity quantity) {
        return new InventoryPosting(UUID.randomUUID().toString(), account, PostingType.CREDIT, quantity);
    }
}

