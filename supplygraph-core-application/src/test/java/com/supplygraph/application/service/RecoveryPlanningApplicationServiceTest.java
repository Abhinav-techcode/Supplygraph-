package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoveryPlanningApplicationServiceTest {

    private RecoveryPlanRepositoryPort planRepo;
    private DisruptionRepositoryPort disruptionRepo;
    private DigitalTwinRepositoryPort twinRepo;
    private ImpactAssessmentRepositoryPort assessmentRepo;
    private RecoveryPlanningApplicationService service;

    private final WorkspaceId ws = WorkspaceId.of("ws-plan-test");
    private final Map<RecoveryPlanId, RecoveryPlan> storage = new HashMap<>();

    @BeforeEach
    void setUp() {
        planRepo = mock(RecoveryPlanRepositoryPort.class);
        disruptionRepo = mock(DisruptionRepositoryPort.class);
        twinRepo = mock(DigitalTwinRepositoryPort.class);
        assessmentRepo = mock(ImpactAssessmentRepositoryPort.class);
        storage.clear();

        doAnswer(inv -> {
            RecoveryPlan p = inv.getArgument(0);
            storage.put(p.id(), p);
            return null;
        }).when(planRepo).save(any(RecoveryPlan.class));

        doAnswer(inv -> {
            RecoveryPlanId id = inv.getArgument(0);
            return Optional.ofNullable(storage.get(id));
        }).when(planRepo).findById(any(RecoveryPlanId.class));

        service = new RecoveryPlanningApplicationService(planRepo, disruptionRepo, twinRepo, assessmentRepo);
    }

    @Test
    @DisplayName("Generate alternatives produces 3 candidate recovery plans and validates with ConcurGuard")
    void testMultiAlternativeGenerationAndValidation() {
        Instant now = Instant.now();
        Disruption disruption = Disruption.createDraft(
                ws, "PHARMACEUTICAL", "CONTAMINATION", "Cold Chain Excursion", "API spoiled",
                DisruptionCategory.INVENTORY_CONTAMINATION, DisruptionSeverity.CRITICAL,
                Set.of(), Set.of(), Set.of(),
                now, now.plusSeconds(86400 * 5), DisruptionImpactParameters.empty(), "Temp logger"
        );

        DigitalTwinState twinState = new DigitalTwinState(ws, StateVersion.of(2L), SupplyChainGraph.empty(), BillOfMaterials.empty());

        when(disruptionRepo.findById(disruption.id())).thenReturn(Optional.of(disruption));
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        List<RecoveryPlan> alternatives = service.generateAlternativePlans(disruption.id(), ws);
        assertThat(alternatives).hasSize(3);

        RecoveryPlan planAlpha = alternatives.get(0);
        assertThat(planAlpha.status()).isEqualTo(RecoveryPlanStatus.DRAFT);

        // Validate plan with ConcurGuard
        RecoveryPlan validated = service.validatePlanWithConcurGuard(planAlpha.id());
        assertThat(validated.concurGuardValidated()).isTrue();
        assertThat(validated.status()).isEqualTo(RecoveryPlanStatus.PENDING_APPROVAL);

        // Approve by human decision-maker
        RecoveryPlan approved = service.approvePlan(planAlpha.id(), "DIRECTOR_LOGISTICS", "Approved optimal buffer dispatch");
        assertThat(approved.status()).isEqualTo(RecoveryPlanStatus.APPROVED);
        assertThat(approved.approval().decision()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Stale plan cannot be approved if digital twin version has advanced")
    void testStalePlanApprovalRejected() {
        Instant now = Instant.now();
        Disruption disruption = Disruption.createDraft(
                ws, "PHARMACEUTICAL", "CONTAMINATION", "Cold Chain", "Desc",
                DisruptionCategory.INVENTORY_CONTAMINATION, DisruptionSeverity.CRITICAL,
                Set.of(), Set.of(), Set.of(),
                now, now.plusSeconds(86400), DisruptionImpactParameters.empty(), "Log"
        );

        // Twin was v2 when plan created
        DigitalTwinState twinState = new DigitalTwinState(ws, StateVersion.of(2L), SupplyChainGraph.empty(), BillOfMaterials.empty());
        when(disruptionRepo.findById(disruption.id())).thenReturn(Optional.of(disruption));
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        List<RecoveryPlan> alternatives = service.generateAlternativePlans(disruption.id(), ws);
        RecoveryPlan plan = service.validatePlanWithConcurGuard(alternatives.get(0).id());

        // Twin version advances to v3 before approval
        DigitalTwinState advancedTwin = new DigitalTwinState(ws, StateVersion.of(3L), SupplyChainGraph.empty(), BillOfMaterials.empty());
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(advancedTwin));

        assertThatThrownBy(() -> service.approvePlan(plan.id(), "CHIEF_OPERATING_OFFICER", "Approve stale"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stale plan cannot be approved");
    }
}

