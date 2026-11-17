package com.supplygraph.domain.model.provenance;

import com.supplygraph.domain.model.audit.AuditLogId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.RecoveryPlanId;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Complete audit and provenance trace documenting the end-to-end lineage from original disruption
 * trigger to audited human approval of a recovery plan.
 */
public record ProvenanceTrace(
        RecoveryPlanId recoveryPlanId,
        DisruptionId disruptionId,
        List<ProvenanceRecord> inputEvidence,
        String digitalTwinVersion,
        ImpactAssessmentId impactAssessmentId,
        List<String> agentProposalIds,
        String concurGuardValidationRef,
        String simulationRunId,
        String optimizationRunId,
        AuditLogId approvalAuditId
) implements Serializable {

    public ProvenanceTrace {
        Objects.requireNonNull(recoveryPlanId, "Recovery plan ID cannot be null");
        Objects.requireNonNull(disruptionId, "Disruption ID cannot be null");
        Objects.requireNonNull(digitalTwinVersion, "Digital twin version cannot be null");
        inputEvidence = inputEvidence != null ? List.copyOf(inputEvidence) : Collections.emptyList();
        agentProposalIds = agentProposalIds != null ? List.copyOf(agentProposalIds) : Collections.emptyList();
    }
}

