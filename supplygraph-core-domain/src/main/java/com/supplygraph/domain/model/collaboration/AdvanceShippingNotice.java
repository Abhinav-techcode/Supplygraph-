package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record AdvanceShippingNotice(
        AdvanceShippingNoticeId id,
        WorkspaceId workspaceId,
        PoCollaborationId poId,
        String trackingNumber,
        String carrierName,
        double shippedQuantity,
        LocalDate estimatedArrivalDate,
        Instant dispatchedAt
) implements Serializable {
    public AdvanceShippingNotice {
        Objects.requireNonNull(id, "ASN ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(poId, "PO ID cannot be null");
        Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");
        Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        Objects.requireNonNull(estimatedArrivalDate, "Estimated arrival date cannot be null");
        Objects.requireNonNull(dispatchedAt, "Dispatched timestamp cannot be null");
        if (shippedQuantity <= 0) {
            throw new IllegalArgumentException("Shipped quantity must be positive");
        }
    }
}

