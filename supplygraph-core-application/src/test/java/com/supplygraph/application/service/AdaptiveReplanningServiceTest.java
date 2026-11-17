package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.application.port.out.ReplanningRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.event.EventProvenance;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.monitoring.MonitoringRuleId;
import com.supplygraph.domain.model.monitoring.MonitoringSeverity;
import com.supplygraph.domain.model.monitoring.RiskAlert;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Adaptive Replanning Service Tests")
class AdaptiveReplanningServiceTest {

    private ReplanningRunRepositoryPort replanningRunRepo;
    private RecoveryPlanRepositoryPort recoveryPlanRepo;
    private DigitalTwinRepositoryPort digitalTwinRepo;
    private AdaptiveReplanningService replanningService;

    @BeforeEach
    void setUp() {
        replanningRunRepo = mock(ReplanningRunRepositoryPort.class);
        recoveryPlanRepo = mock(RecoveryPlanRepositoryPort.class);
        digitalTwinRepo = mock(DigitalTwinRepositoryPort.class);
        replanningService = new AdaptiveReplanningService(replanningRunRepo, recoveryPlanRepo, digitalTwinRepo);
    }

    @Test
    @DisplayName("Should trigger selective replanning when operational alert invalidates plan actions")
    void shouldTriggerSelectiveReplanning() {
        WorkspaceId wsId = WorkspaceId.of("WS-TEST-1");
        DisruptionId disId = DisruptionId.generate();

        PlanAction action1 = new PlanAction(
                "ACT-1", PlanActionType.ALTERNATIVE_SUPPLIER, "Procure from Fab Munich",
                NodeId.of("FAB-MUNICH"), MaterialId.of("MAT-CHIP"), BigDecimal.valueOf(1000), "PCS",
                BigDecimal.valueOf(25000), 24L, Map.of()
        );
        PlanAction action2 = new PlanAction(
                "ACT-2", PlanActionType.INVENTORY_REALLOCATION, "Rebalance Frankfurt hub",
                NodeId.of("HUB-FRANKFURT"), MaterialId.of("MAT-CHIP"), BigDecimal.valueOf(500), "PCS",
                BigDecimal.valueOf(5000), 12L, Map.of()
        );

        RecoveryPlan plan = RecoveryPlan.createDraft(
                disId, wsId, StateVersion.of(1L), ImpactAssessmentId.of("ASSESS-1"),
                "Plan Alpha", "Initial recovery", List.of(action1, action2), PlanComparisonMetrics.defaultMetrics()
        );
        plan = plan.markValidated(true, List.of(), Instant.now()).markPendingApproval(Instant.now());

        when(recoveryPlanRepo.findAllByWorkspaceId(wsId)).thenReturn(List.of(plan));

        DigitalTwinState twinState = new DigitalTwinState(wsId, StateVersion.of(1L), null, null);
        when(digitalTwinRepo.findByWorkspaceId(wsId)).thenReturn(Optional.of(twinState));

        RiskAlert alert = RiskAlert.createActive(
                wsId, MonitoringRuleId.of("RULE-1"), MonitoringSeverity.CRITICAL,
                "Fab Munich Flooding", "Outage halting production",
                "FAB-MUNICH", "NODE", EventId.of("EVT-1"), StateVersion.of(1L), Map.of()
        );
        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_CAPACITY_CHANGED, EventSource.EXTERNAL_INTEGRATION,
                Instant.now(), "FAB-MUNICH", "NODE", wsId, Map.of(),
                EventProvenance.internal("system"), "K1"
        );

        Optional<ReplanningRun> runOpt = replanningService.triggerAdaptiveReplanning(alert, event);

        assertThat(runOpt).isPresent();
        ReplanningRun run = runOpt.get();
        assertThat(run.getStatus()).isEqualTo(ReplanningStatus.READY_FOR_REVIEW);
        assertThat(run.getAffectedAgentIds()).contains("ProcurementAgent");
        assertThat(run.getPreservedActionIds()).contains("ACT-2");
        assertThat(run.getReconsideredActionIds()).contains("ACT-1");
        assertThat(run.getRevisedPlanId()).isNotNull();

        // Verify that the revised plan is saved in pending approval state (not auto-approved)
        verify(recoveryPlanRepo).save(argThat(revised ->
                revised.status() == RecoveryPlanStatus.PENDING_APPROVAL
                        && revised.actions().size() == 2
        ));
    }
}

