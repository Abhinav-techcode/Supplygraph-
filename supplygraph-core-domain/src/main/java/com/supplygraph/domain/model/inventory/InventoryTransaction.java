package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.common.Quantity;

import java.time.Instant;
import java.util.*;

/**
 * Atomic double-entry inventory transaction.
 * Strict conservation guarantee: Total debits must equal total credits.
 */
public record InventoryTransaction(
        TransactionId id,
        Instant timestamp,
        String referenceType,
        String referenceId,
        List<InventoryPosting> postings
) {
    public InventoryTransaction {
        Objects.requireNonNull(id, "TransactionId must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        Objects.requireNonNull(referenceType, "ReferenceType must not be null");
        Objects.requireNonNull(postings, "Postings must not be null");
        if (postings.size() < 2) {
            throw new IllegalArgumentException("A double-entry transaction must contain at least 2 postings (debit and credit)");
        }
        postings = List.copyOf(postings);

        // Conservation of quantity verification:
        validateConservation(postings);
    }

    private static void validateConservation(List<InventoryPosting> postings) {
        Map<com.supplygraph.domain.model.common.UnitOfMeasure.Dimension, Quantity> debitSums = new HashMap<>();
        Map<com.supplygraph.domain.model.common.UnitOfMeasure.Dimension, Quantity> creditSums = new HashMap<>();

        for (InventoryPosting p : postings) {
            var dim = p.quantity().unit().getDimension();
            if (p.postingType() == PostingType.DEBIT) {
                debitSums.merge(dim, p.quantity(), Quantity::add);
            } else {
                creditSums.merge(dim, p.quantity(), Quantity::add);
            }
        }

        if (!debitSums.keySet().equals(creditSums.keySet())) {
            throw new IllegalArgumentException(
                    "Conservation violation: Mismatched unit dimensions between debits and credits: " +
                            debitSums.keySet() + " vs " + creditSums.keySet());
        }

        for (var dim : debitSums.keySet()) {
            Quantity debits = debitSums.get(dim);
            Quantity credits = creditSums.get(dim);
            if (debits.compareTo(credits) != 0) {
                throw new IllegalArgumentException(
                        "Conservation violation: Total debits (" + debits + ") != Total credits (" + credits + ") for dimension " + dim);
            }
        }
    }

    /**
     * Factory helper to create a balanced two-legged transfer transaction.
     */
    public static InventoryTransaction transfer(
            TransactionId id,
            Instant timestamp,
            String referenceType,
            String referenceId,
            InventoryAccountId sourceAccount,
            InventoryAccountId targetAccount,
            Quantity quantity
    ) {
        InventoryPosting credit = InventoryPosting.credit(sourceAccount, quantity);
        InventoryPosting debit = InventoryPosting.debit(targetAccount, quantity);
        return new InventoryTransaction(id, timestamp, referenceType, referenceId, List.of(credit, debit));
    }
}

