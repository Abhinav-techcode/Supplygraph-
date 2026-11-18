package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RfqDocument(
        RfqId id,
        WorkspaceId workspaceId,
        String title,
        MaterialId targetMaterialId,
        double targetQuantity,
        LocalDate submissionDeadline,
        RfqStatus status,
        List<RfqQuotation> quotations,
        Instant createdAt
) implements Serializable {
    public RfqDocument {
        Objects.requireNonNull(id, "RFQ ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(targetMaterialId, "Target material ID cannot be null");
        Objects.requireNonNull(submissionDeadline, "Deadline cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        quotations = quotations != null ? List.copyOf(quotations) : Collections.emptyList();
    }

    public RfqDocument withQuotation(RfqQuotation quotation) {
        List<RfqQuotation> updated = new ArrayList<>(quotations);
        updated.removeIf(q -> q.id().equals(quotation.id()));
        updated.add(quotation);
        return new RfqDocument(id, workspaceId, title, targetMaterialId, targetQuantity, submissionDeadline, status, updated, createdAt);
    }

    public RfqDocument withStatus(RfqStatus newStatus) {
        return new RfqDocument(id, workspaceId, title, targetMaterialId, targetQuantity, submissionDeadline, newStatus, quotations, createdAt);
    }

    public Optional<RfqQuotation> findQuotation(RfqQuotationId quoteId) {
        return quotations.stream().filter(q -> q.id().equals(quoteId)).findFirst();
    }
}

