package com.supplygraph.domain.model;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryPlanTest {

    @Test
    @DisplayName("Recovery plan requires simulation and ConcurGuard validation before human approval")
    void testRecoveryPlanLifecycleAndApproval() {
        DisruptionId disruptionId = DisruptionId.generate();
        WorkspaceId ws = WorkspaceId.of("ws-semi");
        StateVersion currentTwinVersion = StateVersion.of(3L);

        PlanAction action1 = new PlanAction(
                "ACT-1",
                PlanActionType.ALTERNATIVE_SUPPLIER,
                "Qualify backup foundry in Dresden for wafers",
                NodeId.of("FAB-DRESDEN"),
                MaterialId.of("SILICON-WAFER-300MM"),
                BigDecimal.valueOf(10000),
                "WAFERS",
                BigDecimal.valueOf(50000),
                48L,
                Map.of("qualificationStatus", "ISO_VERIFIED")
        );

        RecoveryPlan plan = RecoveryPlan.createDraft(
                disruptionId,
                ws,
                currentTwinVersion,
                ImpactAssessmentId.generate(),
                "Plan Alpha: European Foundry Reallocation",
                "Reroutes wafer demand to certified European backup capacity",
                List.of(action1),
                new PlanComparisonMetrics(BigDecimal.valueOf(50000), 4.5, 98.5, BigDecimal.ZERO, BigDecimal.valueOf(10000), 75.0, 0.2, 0.85)
        );

        assertThat(plan.status()).isEqualTo(RecoveryPlanStatus.DRAFT);

        // Cannot approve directly from DRAFT
        assertThatThrownBy(() -> plan.approve("VP_SUPPLY_CHAIN", "Immediate need", currentTwinVersion, Instant.now()))
                .isInstanceOf(IllegalStateException.class);

        // Mark simulated
        RecoveryPlan simulated = plan.markSimulated("SIM-RUN-101", null, Instant.now());
        assertThat(simulated.status()).isEqualTo(RecoveryPlanStatus.SIMULATED);

        // Mark validated by ConcurGuard with 0 conflicts
        RecoveryPlan validated = simulated.markValidated(true, List.of(), Instant.now());
        assertThat(validated.status()).isEqualTo(RecoveryPlanStatus.VALIDATED);
        assertThat(validated.concurGuardValidated()).isTrue();

        // Move to PENDING_APPROVAL
        RecoveryPlan pending = validated.markPendingApproval(Instant.now());
        assertThat(pending.status()).isEqualTo(RecoveryPlanStatus.PENDING_APPROVAL);

        // Stale twin check: if twin version changed to 4, approving should fail
        StateVersion changedTwinVersion = StateVersion.of(4L);
        assertThatThrownBy(() -> pending.approve("VP_SUPPLY_CHAIN", "Approved", changedTwinVersion, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stale plan cannot be approved");

        // Approved with matching twin version
        RecoveryPlan approved = pending.approve("VP_SUPPLY_CHAIN", "Approved as least cost and robust", currentTwinVersion, Instant.now());
        assertThat(approved.status()).isEqualTo(RecoveryPlanStatus.APPROVED);
        assertThat(approved.approval()).isNotNull();
        assertThat(approved.approval().approvedBy()).isEqualTo("VP_SUPPLY_CHAIN");
        assertThat(approved.approval().decision()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Plan with ConcurGuard conflict cannot be approved")
    void testConcurGuardConflictPreventsApproval() {
        DisruptionId disruptionId = DisruptionId.generate();
        WorkspaceId ws = WorkspaceId.of("ws-semi");
        StateVersion currentTwinVersion = StateVersion.of(1L);

        RecoveryPlan plan = RecoveryPlan.createDraft(
                disruptionId, ws, currentTwinVersion, ImpactAssessmentId.generate(),
                "Conflicting Plan", "Desc", List.of(), null
        );

        RecoveryPlan simulated = plan.markSimulated("SIM-1", null, Instant.now());
        // Validation fails due to ConcurGuard conflict
        RecoveryPlan invalidPlan = simulated.markValidated(false, List.of("CONFLICT-RESOURCE-OVERALLOCATION"), Instant.now());

        assertThatThrownBy(() -> invalidPlan.markPendingApproval(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without passing ConcurGuard validation");
    }
}

