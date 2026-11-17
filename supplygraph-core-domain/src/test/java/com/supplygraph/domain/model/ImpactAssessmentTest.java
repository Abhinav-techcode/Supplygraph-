package com.supplygraph.domain.model;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.NodeType;
import com.supplygraph.domain.model.graph.TransportMode;
import com.supplygraph.domain.model.impact.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactAssessmentTest {

    @Test
    @DisplayName("Impact assessment holds structured consequences across nodes, materials, capacity, and logistics")
    void testImpactAssessmentCreation() {
        DisruptionId disruptionId = DisruptionId.generate();
        WorkspaceId ws = WorkspaceId.of("ws-pharma");
        StateVersion twinVer = StateVersion.of(5L);

        NodeImpact nodeImpact = new NodeImpact(
                NodeId.of("API-PLANT-1"),
                "Active Pharmaceutical Ingredient Plant",
                NodeType.MANUFACTURING_SITE,
                ImpactType.DIRECT,
                "API synthesis line suspended due to autoclave failure",
                BigDecimal.valueOf(100)
        );

        MaterialShortageImpact matShortage = new MaterialShortageImpact(
                MaterialId.of("API-COMPOUND-X"),
                "Compound X Sterile Bulk",
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(400),
                "KG",
                Instant.now().plusSeconds(86400 * 2)
        );

        CapacityImpact capImpact = new CapacityImpact(
                "API-PLANT-1",
                "API Plant 1",
                BigDecimal.valueOf(100),
                "48_HOUR_STERILIZATION"
        );

        LogisticsImpact logImpact = new LogisticsImpact(
                EdgeId.of("LANE-COLDCHAIN-1"),
                NodeId.of("API-PLANT-1"),
                NodeId.of("FILL-FINISH-SITE"),
                TransportMode.ROAD,
                7200L,
                BigDecimal.valueOf(2500)
        );

        ImpactAssessment assessment = ImpactAssessment.create(
                disruptionId,
                ws,
                twinVer,
                ImpactSeverity.HIGH,
                List.of(nodeImpact),
                List.of(matShortage),
                List.of(capImpact),
                List.of(logImpact),
                BigDecimal.valueOf(150000),
                List.of("Severe shortage risk for oncology fill-finish batch"),
                List.of("Inventory run-rate constant at 50kg/day"),
                List.of()
        );

        assertThat(assessment.disruptionId()).isEqualTo(disruptionId);
        assertThat(assessment.digitalTwinVersion()).isEqualTo(twinVer);
        assertThat(assessment.overallSeverity()).isEqualTo(ImpactSeverity.HIGH);
        assertThat(assessment.materialShortages()).hasSize(1);
        assertThat(assessment.materialShortages().get(0).shortageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(assessment.nodeImpacts()).hasSize(1);
        assertThat(assessment.totalEstimatedCost()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }
}

