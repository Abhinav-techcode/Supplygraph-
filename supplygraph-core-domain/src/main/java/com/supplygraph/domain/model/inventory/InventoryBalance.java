package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;

/**
 * Immutable balance snapshot for a specific inventory account.
 */
public record InventoryBalance(
        InventoryAccountId accountId,
        Quantity balance
) {
    public InventoryBalance {
        Objects.requireNonNull(accountId, "AccountId must not be null");
        Objects.requireNonNull(balance, "Balance must not be null");
        if (accountId.category() != BalanceCategory.EXTERNAL_OFFSET && balance.isNegative()) {
            throw new IllegalArgumentException(
                    "Internal inventory balance cannot be negative for account: " + accountId + ", amount: " + balance);
        }
    }

    public static InventoryBalance zero(InventoryAccountId accountId, com.supplygraph.domain.model.common.UnitOfMeasure unit) {
        return new InventoryBalance(accountId, Quantity.zero(unit));
    }
}

