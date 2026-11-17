package com.supplygraph.domain.model.plan;

import com.supplygraph.domain.model.twin.StateVersion;

import java.time.Instant;
import java.util.Objects;

/**
 * Explicit audited record of a human decision-maker's approval or rejection of a recovery plan.
 */
public record PlanApproval(
        String approvedBy,
        Instant approvalTimestamp,
        String decision,
        String rationale,
        StateVersion revalidatedTwinVersion
) {
    public PlanApproval {
        Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        Objects.requireNonNull(approvalTimestamp, "approvalTimestamp must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(revalidatedTwinVersion, "revalidatedTwinVersion must not be null");
        rationale = rationale == null ? "" : rationale;
    }
}

