package com.supplygraph.domain.model.platform;

import com.supplygraph.domain.model.WorkspaceId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record IndustryPolicyRecord(
        IndustryPolicyId id,
        WorkspaceId workspaceId,
        String industryCode,
        int versionNumber,
        String policyJson,
        PolicyStatus status,
        Instant createdAt
) implements Serializable {
    public IndustryPolicyRecord {
        Objects.requireNonNull(id, "IndustryPolicyId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(industryCode, "IndustryCode cannot be null");
        Objects.requireNonNull(policyJson, "PolicyJson cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("Version number must be >= 1");
        }
    }

    public IndustryPolicyRecord withStatus(PolicyStatus newStatus) {
        return new IndustryPolicyRecord(id, workspaceId, industryCode, versionNumber, policyJson, newStatus, createdAt);
    }
}

