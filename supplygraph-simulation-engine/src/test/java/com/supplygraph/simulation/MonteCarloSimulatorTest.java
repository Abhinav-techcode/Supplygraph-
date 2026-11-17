package com.supplygraph.simulation;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.engine.SimulationConfiguration;
import com.supplygraph.simulation.montecarlo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonteCarloSimulatorTest {

    private DigitalTwinSnapshot createSnapshot() {
        return new DigitalTwinSnapshot(
                WorkspaceId.of("ws-mc-test"),
                StateVersion.of(1L),
                Instant.now(),
                SupplyChainGraph.empty(),
                BillOfMaterials.empty(),
                Map.of()
        );
    }

    private Disruption createDisruption() {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        return Disruption.createDraft(
                WorkspaceId.of("ws-mc-test"), "AUTOMOTIVE", "CHIP_CRISIS", "MCU Delay", "Port congestion",
                DisruptionCategory.TRANSPORTATION_DELAY, DisruptionSeverity.MEDIUM,
                Set.of(), Set.of(), Set.of(),
                now, now.plusSeconds(86400 * 3),
                DisruptionImpactParameters.ofDelay(86400 * 3), "Carrier tracking"
        );
    }

    @Test
    @DisplayName("Monte Carlo results are strictly reproducible when using the same random seed")
    void testSeededReproducibility() {
        DigitalTwinSnapshot snapshot = createSnapshot();
        Disruption disruption = createDisruption();
        Instant start = Instant.parse("2026-11-17T00:00:00Z");
        SimulationConfiguration simConfig = new SimulationConfiguration(start, start.plusSeconds(86400 * 4), 3600L * 12, 100, true);

        ProbabilityDistribution dist = new NormalDistribution(1.0, 0.15, 0.5, 2.0);
        MonteCarloParameter param = new MonteCarloParameter("leadTimeMultiplier", "GLOBAL", dist);

        long fixedSeed = 424242L;
        MonteCarloConfiguration config1 = new MonteCarloConfiguration(fixedSeed, 20, 100, List.of(param), simConfig);
        MonteCarloConfiguration config2 = new MonteCarloConfiguration(fixedSeed, 20, 100, List.of(param), simConfig);

        MonteCarloSimulator simulator = new MonteCarloSimulator();
        MonteCarloSummaryResult res1 = simulator.run(snapshot, disruption, null, config1);
        MonteCarloSummaryResult res2 = simulator.run(snapshot, disruption, null, config2);

        assertThat(res1.meanDelayDays()).isEqualTo(res2.meanDelayDays());
        assertThat(res1.medianDelayDays()).isEqualTo(res2.medianDelayDays());
        assertThat(res1.p90DelayDays()).isEqualTo(res2.p90DelayDays());
        assertThat(res1.p95DelayDays()).isEqualTo(res2.p95DelayDays());
        assertThat(res1.slaViolationProbability()).isEqualTo(res2.slaViolationProbability());
    }

    @Test
    @DisplayName("Distributions enforce mathematical constraints and valid sampling intervals")
    void testDistributionInvariants() {
        assertThatThrownBy(() -> new UniformDistribution(10.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new TriangularDistribution(5.0, 3.0, 10.0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DiscreteDistribution(List.of(1.0, 2.0), List.of(0.4, 0.4))) // sum != 1.0
                .isInstanceOf(IllegalArgumentException.class);
    }
}

