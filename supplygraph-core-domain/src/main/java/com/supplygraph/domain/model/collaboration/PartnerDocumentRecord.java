package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record PartnerDocumentRecord(
        PartnerDocumentId id,
        WorkspaceId workspaceId,
        PartnerOrganizationId partnerId,
        String fileName,
        DocumentCategory category,
        String cryptographicHash,
        boolean verified,
        Instant uploadedAt
) implements Serializable {
    public PartnerDocumentRecord {
        Objects.requireNonNull(id, "Document ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(partnerId, "Partner ID cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(cryptographicHash, "Cryptographic hash cannot be null");
        Objects.requireNonNull(uploadedAt, "Uploaded timestamp cannot be null");
    }

    public PartnerDocumentRecord withVerified(boolean isVerified) {
        return new PartnerDocumentRecord(id, workspaceId, partnerId, fileName, category, cryptographicHash, isVerified, uploadedAt);
    }
}

