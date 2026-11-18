package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Supplier ESG audit scorecard measuring decarbonization, social compliance, and regulatory adherence.
 */
public record SupplierEsgScorecard(
        EsgScorecardId id,
        WorkspaceId workspaceId,
        NodeId supplierId,
        double environmentalScore,
        double socialScore,
        double governanceScore,
        double compositeEsgScore,
        boolean cbamCompliant,
        boolean conflictMineralsCertified,
        boolean eudrCompliant,
        Instant auditedAt
) implements Serializable {
    public SupplierEsgScorecard {
        Objects.requireNonNull(id, "ESG scorecard ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(auditedAt, "Audit timestamp cannot be null");
        if (environmentalScore < 0 || environmentalScore > 100 ||
            socialScore < 0 || socialScore > 100 ||
            governanceScore < 0 || governanceScore > 100) {
            throw new IllegalArgumentException("ESG scores must be within [0, 100]");
        }
    }

    public boolean isFullyCompliant() {
        return cbamCompliant && conflictMineralsCertified && eudrCompliant && compositeEsgScore >= 70.0;
    }
}

