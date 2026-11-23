package com.supplygraph.application.explainability;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ExplainableRecommendationRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.explainability.*;
import com.supplygraph.domain.model.recovery.RecoveryPlanAlternative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Generates transparent, evidence-backed recovery recommendations and enforces mandatory human approval.
 */
@Service
public class ExplainableRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ExplainableRecommendationService.class);

    private final ExplainableRecommendationRepositoryPort recommendationRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final AuditGovernanceService auditGovernanceService;

    public ExplainableRecommendationService(
            ExplainableRecommendationRepositoryPort recommendationRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            AuditGovernanceService auditGovernanceService
    ) {
        this.recommendationRepository = recommendationRepository;
        this.recoveryPlanRepository = recoveryPlanRepository;
        this.auditGovernanceService = auditGovernanceService;
    }

    /**
     * Synthesizes an explainable recommendation for a recovery plan alternative with evidence,
     * constraint compliance, trade-offs, and uncertainty factors.
     */
    public ExplainableRecommendation generateRecommendation(
            String disruptionId,
            String stateVersion,
            RecoveryPlanAlternative alternative
    ) {
        Objects.requireNonNull(disruptionId, "disruptionId cannot be null");
        Objects.requireNonNull(alternative, "alternative cannot be null");

        RecommendationId id = RecommendationId.generate();

        // Only facts that the recovery option itself states are recorded. A confidence of -1 means "not measured": no score is invented.
        List<DecisionEvidence> evidence = List.of(DecisionEvidence.of(
                "EVID-OPTION", "Figures stated by the recovery option", "Recovery option derived from the digital twin", DecisionEvidence.NOT_MEASURED, Instant.now(),
                "Cost " + alternative.expectedCost() + " USD, recovery in " + String.format("%.1f", alternative.timeToRecoverHours()) + " hours, service level "
                        + String.format("%.1f", alternative.serviceLevelRestoration()) + "%. Twin version " + (stateVersion != null ? stateVersion : "unknown") + "."));

        // No budget ceilings, supplier limits or certifications are assumed: the twin does not hold them, so no constraint is claimed as satisfied.
        List<DecisionConstraint> constraints = List.of();
        List<DecisionTradeOff> tradeOffs = List.of();
        Map<String, String> uncertainty = Map.of("Not assessed", "No risk analysis is attached to this recommendation. Run the simulation and Monte Carlo analysis for risk figures.");

        List<String> primaryActions = alternative.actions();

        ExplainableRecommendation recommendation = ExplainableRecommendation.createPending(
                id,
                disruptionId,
                stateVersion != null ? stateVersion : "V1",
                "Recommended Recovery: " + alternative.name(),
                alternative.description() == null ? "" : alternative.description(),
                primaryActions,
                alternative.expectedCost(),
                alternative.timeToRecoverHours(),
                alternative.serviceLevelRestoration(),
                evidence,
                constraints,
                tradeOffs,
                uncertainty
        );

        recommendationRepository.save(recommendation);
        log.info("Generated explainable recommendation: {} for disruption {}", id.value(), disruptionId);
        return recommendation;
    }

    /**
     * Executes audited human sign-off approving a recovery recommendation.
     * Mandatory human decision requirement: autonomous sign-off is prohibited.
     */
    public ExplainableRecommendation approveRecommendation(
            RecommendationId id,
            String authorizedUserId,
            String workspaceId
    ) {
        Objects.requireNonNull(authorizedUserId, "Authorizing human user must be specified");
        ExplainableRecommendation existing = recommendationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Recommendation not found: " + id.value()));

        if (existing.approved()) {
            throw new ConflictException("Recommendation is already approved: " + id.value());
        }

        Instant approvedAt = Instant.now();
        ExplainableRecommendation approved = existing.withApproval(authorizedUserId, approvedAt);
        recommendationRepository.save(approved);

        auditGovernanceService.logAction(
                UserId.of(authorizedUserId),
                WorkspaceId.of(workspaceId != null ? workspaceId : "WS-AUTO-01"),
                AuditAction.PLAN_APPROVED,
                id.value(),
                existing.stateVersion() != null ? existing.stateVersion() : "V1",
                "V_APPROVED",
                "Human manager approved explainable recovery recommendation " + id.value(),
                "CG-NONE"
        );

        log.info("Human manager {} approved recommendation {}", authorizedUserId, id.value());
        return approved;
    }

    /**
     * Executes audited human rejection of a recovery recommendation.
     */
    public ExplainableRecommendation rejectRecommendation(
            RecommendationId id,
            String rejectingUserId,
            String workspaceId,
            String reason
    ) {
        Objects.requireNonNull(rejectingUserId, "Rejecting human user must be specified");
        Objects.requireNonNull(reason, "Rejection reason must be provided");

        ExplainableRecommendation existing = recommendationRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Recommendation not found: " + id.value()));

        Instant rejectedAt = Instant.now();
        ExplainableRecommendation rejected = existing.withRejection(rejectingUserId, reason, rejectedAt);
        recommendationRepository.save(rejected);

        auditGovernanceService.logAction(
                UserId.of(rejectingUserId),
                WorkspaceId.of(workspaceId != null ? workspaceId : "WS-AUTO-01"),
                AuditAction.PLAN_REJECTED,
                id.value(),
                existing.stateVersion() != null ? existing.stateVersion() : "V1",
                "V_REJECTED",
                "Human manager rejected recommendation " + id.value() + ": " + reason,
                "CG-NONE"
        );

        log.info("Human manager {} rejected recommendation {}: {}", rejectingUserId, id.value(), reason);
        return rejected;
    }

    public Optional<ExplainableRecommendation> findById(RecommendationId id) {
        return recommendationRepository.findById(id);
    }

    public List<ExplainableRecommendation> findByDisruptionId(String disruptionId) {
        return recommendationRepository.findByDisruptionId(disruptionId);
    }

    public List<ExplainableRecommendation> findAll() {
        return recommendationRepository.findAll();
    }
}
