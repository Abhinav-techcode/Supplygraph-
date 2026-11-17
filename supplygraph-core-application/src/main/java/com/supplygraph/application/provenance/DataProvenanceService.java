package com.supplygraph.application.provenance;

import com.supplygraph.application.port.out.AuditLogRepositoryPort;
import com.supplygraph.application.port.out.ProvenanceRepositoryPort;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.provenance.ProvenanceId;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;
import com.supplygraph.domain.model.provenance.ProvenanceTrace;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing data provenance, asset classification, and end-to-end recovery traceability.
 */
@Service
public class DataProvenanceService {

    private static final Logger log = LoggerFactory.getLogger(DataProvenanceService.class);

    private final ProvenanceRepositoryPort provenanceRepository;
    private final AuditLogRepositoryPort auditLogRepository;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    public DataProvenanceService(
            ProvenanceRepositoryPort provenanceRepository,
            AuditLogRepositoryPort auditLogRepository,
            WorkspaceAccessGuard workspaceAccessGuard
    ) {
        this.provenanceRepository = provenanceRepository;
        this.auditLogRepository = auditLogRepository;
        this.workspaceAccessGuard = workspaceAccessGuard;
    }

    /**
     * Registers a provenance record distinguishing real imported, synthetic, or AI-generated data.
     */
    public ProvenanceRecord registerProvenance(
            WorkspaceId workspaceId,
            String entityId,
            String entityType,
            ProvenanceType type,
            String sourceReference,
            String stateVersion,
            Map<String, String> attributes
    ) {
        ProvenanceRecord record = ProvenanceRecord.of(
                workspaceId,
                entityId,
                entityType,
                type,
                sourceReference,
                stateVersion,
                attributes
        );
        log.info("Provenance recorded for [{}: {}] type [{}] from source [{}]",
                entityType, entityId, type, sourceReference);
        return provenanceRepository.save(record);
    }

    public List<ProvenanceRecord> getRecordsByWorkspace(WorkspaceId workspaceId) {
        return provenanceRepository.findByWorkspaceId(workspaceId);
    }

    public List<ProvenanceRecord> getRecordsByEntity(String entityId) {
        return provenanceRepository.findByEntityId(entityId);
    }

    public List<ProvenanceRecord> getAllRecords() {
        return provenanceRepository.findAll();
    }

    /**
     * Assembles the full traceability lineage for an approved or proposed recovery plan.
     */
    public ProvenanceTrace buildTrace(
            RecoveryPlanId planId,
            DisruptionId disruptionId,
            String digitalTwinVersion,
            ImpactAssessmentId impactAssessmentId,
            List<String> agentProposalIds,
            String concurGuardValidationRef,
            String simulationRunId,
            String optimizationRunId
    ) {
        List<ProvenanceRecord> evidence = provenanceRepository.findByEntityId(disruptionId.value().toString());

        // Find associated approval audit entry if one exists
        AuditLogEntry approvalAudit = auditLogRepository.findByTargetEntityId(planId.value().toString())
                .stream()
                .filter(a -> "PLAN_APPROVED".equals(a.action()))
                .findFirst()
                .orElse(null);

        return new ProvenanceTrace(
                planId,
                disruptionId,
                evidence,
                digitalTwinVersion,
                impactAssessmentId,
                agentProposalIds,
                concurGuardValidationRef,
                simulationRunId,
                optimizationRunId,
                approvalAudit != null ? approvalAudit.id() : null
        );
    }
}

