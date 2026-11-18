package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record PoCollaborationRecord(
        PoCollaborationId id,
        WorkspaceId workspaceId,
        String poNumber,
        PartnerOrganizationId supplierId,
        MaterialId materialId,
        double orderQuantity,
        BigDecimal totalAmountUsd,
        LocalDate promisedDeliveryDate,
        PoStatus status,
        Instant lastUpdated
) implements Serializable {
    public PoCollaborationRecord {
        Objects.requireNonNull(id, "PO collaboration ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(poNumber, "PO number cannot be null");
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        Objects.requireNonNull(totalAmountUsd, "Total amount cannot be null");
        Objects.requireNonNull(promisedDeliveryDate, "Promised delivery date cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");
    }

    public PoCollaborationRecord withStatus(PoStatus newStatus, LocalDate newPromisedDate) {
        return new PoCollaborationRecord(id, workspaceId, poNumber, supplierId, materialId, orderQuantity, totalAmountUsd, newPromisedDate, newStatus, Instant.now());
    }
}

