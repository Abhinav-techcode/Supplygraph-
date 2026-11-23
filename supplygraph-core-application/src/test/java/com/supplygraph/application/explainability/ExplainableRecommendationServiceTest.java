package com.supplygraph.application.explainability;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.ExplainableRecommendationRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.explainability.ExplainableRecommendation;
import com.supplygraph.domain.model.explainability.RecommendationId;
import com.supplygraph.domain.model.recovery.PlanAlternativeId;
import com.supplygraph.domain.model.recovery.RecoveryPlanAlternative;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExplainableRecommendationServiceTest {

    private ExplainableRecommendationRepositoryPort recommendationRepository;
    private RecoveryPlanRepositoryPort recoveryPlanRepository;
    private AuditGovernanceService auditGovernanceService;
    private ExplainableRecommendationService service;

    @BeforeEach
    void setUp() {
        recommendationRepository = mock(ExplainableRecommendationRepositoryPort.class);
        recoveryPlanRepository = mock(RecoveryPlanRepositoryPort.class);
        auditGovernanceService = mock(AuditGovernanceService.class);
        service = new ExplainableRecommendationService(recommendationRepository, recoveryPlanRepository, auditGovernanceService);
    }

    @Test
    @DisplayName("Should generate explainable recommendation with evidence, constraints, and trade-offs requiring human approval")
    void shouldGenerateExplainableRecommendation() {
        RecoveryPlanAlternative alt = new RecoveryPlanAlternative(
                PlanAlternativeId.of("ALT-01"),
                "Air Freight Expedited",
                "Reroutes critical subassemblies via air freight",
                BigDecimal.valueOf(65000),
                36.0,
                98.0,
                List.of("Action 1", "Action 2")
        );

        ExplainableRecommendation rec = service.generateRecommendation("DISR-100", "V1", alt);

        assertThat(rec).isNotNull();
        assertThat(rec.disruptionId()).isEqualTo("DISR-100");
        assertThat(rec.requiresHumanApproval()).isTrue();
        assertThat(rec.approved()).isFalse();
        // only facts the option itself states are recorded; no invented evidence, constraints, trade-offs or scores
        assertThat(rec.evidenceList()).hasSize(1);
        assertThat(rec.evidenceList().get(0).confidenceScore()).isEqualTo(com.supplygraph.domain.model.explainability.DecisionEvidence.NOT_MEASURED);
        assertThat(rec.evidenceList().get(0).summary()).contains("65000", "36.0 hours", "98.0%");
        assertThat(rec.constraintsEvaluated()).isEmpty();
        assertThat(rec.tradeOffs()).isEmpty();
        assertThat(rec.primaryActions()).containsExactly("Action 1", "Action 2");
        assertThat(rec.uncertaintyFactors()).containsKey("Not assessed");

        verify(recommendationRepository, times(1)).save(any(ExplainableRecommendation.class));
    }

    @Test
    @DisplayName("Should allow human approval and reject double approval")
    void shouldAllowHumanApprovalAndLogAudit() {
        RecoveryPlanAlternative alt = new RecoveryPlanAlternative(
                PlanAlternativeId.of("ALT-01"),
                "Air Freight Expedited",
                "Description",
                BigDecimal.valueOf(65000),
                36.0,
                98.0,
                List.of()
        );
        ExplainableRecommendation rec = service.generateRecommendation("DISR-100", "V1", alt);

        when(recommendationRepository.findById(rec.id())).thenReturn(Optional.of(rec));

        ExplainableRecommendation approved = service.approveRecommendation(rec.id(), "USR-HUMAN-01", "WS-01");

        assertThat(approved.approved()).isTrue();
        assertThat(approved.approvedBy()).isEqualTo("USR-HUMAN-01");
        assertThat(approved.approvedAt()).isNotNull();

        verify(auditGovernanceService, times(1)).logAction(any(), any(), any(), eq(rec.id().value()), any(), any(), any(), any());

        // Attempting double approval must fail
        when(recommendationRepository.findById(rec.id())).thenReturn(Optional.of(approved));
        assertThatThrownBy(() -> service.approveRecommendation(rec.id(), "USR-ANOTHER", "WS-01"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already approved");
    }
}
