package com.supplygraph.domain.model;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisruptionLifecycleTest {

    @Test
    @DisplayName("Disruption lifecycle follows DRAFT -> VALIDATED -> ACTIVE -> RESOLVED")
    void testDisruptionLifecycleProgression() {
        WorkspaceId ws = WorkspaceId.of("ws-auto-1");
        Instant now = Instant.now();
        Instant end = now.plusSeconds(86400 * 5);

        Disruption draft = Disruption.createDraft(
                ws,
                "AUTOMOTIVE",
                "CHIP_SHORTAGE",
                "Microcontroller Fab Shutdown",
                "Tier-2 supplier facility shut down due to wafer defect",
                DisruptionCategory.SUPPLIER_SHUTDOWN,
                DisruptionSeverity.CRITICAL,
                Set.of(NodeId.of("FAB-TAIWAN-1")),
                Set.of(EdgeId.of("LANE-TAIWAN-DETROIT")),
                Set.of(MaterialId.of("MCU-AUTO-32")),
                now,
                end,
                new DisruptionImpactParameters(BigDecimal.valueOf(100), 86400L * 5, 5, BigDecimal.valueOf(50000), "PIECES", Map.of()),
                "Supplier formal force majeure notice"
        );

        assertThat(draft.status()).isEqualTo(DisruptionStatus.DRAFT);
        assertThat(draft.version()).isEqualTo(0L);

        // Transition to VALIDATED
        Disruption validated = draft.validate(now.plusSeconds(60));
        assertThat(validated.status()).isEqualTo(DisruptionStatus.VALIDATED);
        assertThat(validated.version()).isEqualTo(1L);

        // Transition to ACTIVE
        Disruption active = validated.activate(now.plusSeconds(120));
        assertThat(active.status()).isEqualTo(DisruptionStatus.ACTIVE);
        assertThat(active.version()).isEqualTo(2L);

        // Transition to RESOLVED
        Disruption resolved = active.resolve(now.plusSeconds(86400 * 5));
        assertThat(resolved.status()).isEqualTo(DisruptionStatus.RESOLVED);
        assertThat(resolved.version()).isEqualTo(3L);
        assertThat(resolved.status().isTerminal()).isTrue();

        // Terminal state cannot transition
        assertThatThrownBy(() -> resolved.activate(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid disruption lifecycle transition");
    }

    @Test
    @DisplayName("Disruption cannot transition directly from DRAFT to RESOLVED")
    void testInvalidDisruptionTransition() {
        Disruption draft = Disruption.createDraft(
                WorkspaceId.of("ws-1"),
                "EV_BATTERY",
                "LITHIUM_SPIKE",
                "Lithium Shortage",
                "Raw lithium supply constraint",
                DisruptionCategory.RAW_MATERIAL_SHORTAGE,
                DisruptionSeverity.HIGH,
                Set.of(), Set.of(), Set.of(MaterialId.of("LITHIUM-HYDROXIDE")),
                Instant.now(), Instant.now().plusSeconds(3600),
                DisruptionImpactParameters.empty(),
                "Market alert"
        );

        assertThatThrownBy(() -> draft.resolve(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Invalid impact parameters and dates are rejected")
    void testDisruptionValidationInvariants() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600);

        assertThatThrownBy(() -> Disruption.createDraft(
                WorkspaceId.of("ws-1"), "GENERIC", "TEST", "Title", "Desc",
                DisruptionCategory.DEMAND_SURGE, DisruptionSeverity.MEDIUM,
                Set.of(), Set.of(), Set.of(),
                now, past, // End before start
                DisruptionImpactParameters.empty(), "Source"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DisruptionImpactParameters(
                BigDecimal.valueOf(150), // > 100%
                100L, 2, BigDecimal.TEN, "UNIT", Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

