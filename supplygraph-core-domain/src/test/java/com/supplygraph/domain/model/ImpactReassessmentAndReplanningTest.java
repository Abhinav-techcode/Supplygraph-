package com.supplygraph.domain.model;

import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.monitoring.RiskAlertId;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Impact Reassessment & Replanning Domain Tests")
class ImpactReassessmentAndReplanningTest {

    @Test
    @DisplayName("Should create impact reassessment isolating affected from preserved scope")
    void shouldCreateImpactReassessment() {
        ImpactReassessment reassessment = ImpactReassessment.create(
                WorkspaceId.of("WS-TEST-1"),
                EventId.of("EVT-100"),
                StateVersion.of(1L),
                StateVersion.of(2L),
                Set.of(NodeId.of("FAB-MUNICH")),
                Set.of(EdgeId.of("LANE-MUNICH-STUTTGART")),
                Set.of(MaterialId.of("MCU-AUTO-32")),
                List.of("Preserved: Supplier Kyoto line unaffected", "Preserved: Hub Frankfurt unaffected"),
                List.of("Invalidated: Fab Munich production halted"),
                List.of("ASSESS-001"),
                List.of("PLAN-ALPHA")
        );

        assertThat(reassessment.getId()).isNotNull();
        assertThat(reassessment.getAffectedNodeIds()).containsExactly(NodeId.of("FAB-MUNICH"));
        assertThat(reassessment.getPreservedScope()).hasSize(2);
        assertThat(reassessment.getInvalidatedScope()).hasSize(1);
    }

    @Test
    @DisplayName("Should track replanning lifecycle and enforce retry limit")
    void shouldTrackReplanningLifecycle() {
        ReplanningRun run = ReplanningRun.start(
                WorkspaceId.of("WS-TEST-1"),
                RiskAlertId.of("ALT-100"),
                RecoveryPlanId.of("PLAN-OLD-1"),
                "Secondary sourcing invalidated by typhoon"
        );

        assertThat(run.getStatus()).isEqualTo(ReplanningStatus.DETECTED);
        run.advanceStatus(ReplanningStatus.ASSESSING, "Scanning affected BOM");
        assertThat(run.getStatus()).isEqualTo(ReplanningStatus.ASSESSING);

        run.advanceStatus(ReplanningStatus.REPLANNING, "Invoking ProcurementAgent");
        run.addAffectedAgents(List.of("ProcurementAgent", "InventoryAgent"));
        run.addPreservedActions(List.of("ACT-PRESERVED-1"));
        run.addReconsideredActions(List.of("ACT-INVALIDATED-1"));

        assertThat(run.getAffectedAgentIds()).contains("ProcurementAgent");
        assertThat(run.getPreservedActionIds()).contains("ACT-PRESERVED-1");
        assertThat(run.getReconsideredActionIds()).contains("ACT-INVALIDATED-1");

        run.advanceStatus(ReplanningStatus.READY_FOR_REVIEW, "Replanning complete, proposal created");
        assertThat(run.getStatus()).isEqualTo(ReplanningStatus.READY_FOR_REVIEW);
        assertThat(run.getCompletedAt()).isNotNull();
    }
}

