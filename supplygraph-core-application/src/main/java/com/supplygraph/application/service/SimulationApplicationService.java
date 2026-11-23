package com.supplygraph.application.service;

import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.application.port.out.SimulationRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.simulation.engine.DiscreteEventSimulationEngine;
import com.supplygraph.simulation.engine.SimulationConfiguration;
import com.supplygraph.simulation.engine.SimulationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service managing discrete-event simulation execution and result lifecycle.
 */
@Service
public class SimulationApplicationService {

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final DisruptionRepositoryPort disruptionRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final SimulationRunRepositoryPort simulationRunRepository;
    private final DiscreteEventSimulationEngine engine = new DiscreteEventSimulationEngine();

    public SimulationApplicationService(
            DigitalTwinRepositoryPort digitalTwinRepository,
            DisruptionRepositoryPort disruptionRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            SimulationRunRepositoryPort simulationRunRepository
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.simulationRunRepository = Objects.requireNonNull(simulationRunRepository, "SimulationRunRepositoryPort must not be null");
    }

    @Transactional
    public SimulationResult runSimulation(
            WorkspaceId workspaceId,
            DisruptionId disruptionId,
            RecoveryPlanId planId,
            SimulationConfiguration config
    ) {
        DigitalTwinState twinState = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found: " + workspaceId.value()));
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Disruption not found: " + disruptionId.value()));

        RecoveryPlan plan = null;
        if (planId != null) {
            plan = recoveryPlanRepository.findById(planId).orElse(null);
        }

        DigitalTwinSnapshot snapshot = twinState.createSnapshot();
        SimulationResult result = engine.run(snapshot, disruption, plan, config);

        simulationRunRepository.save(result, workspaceId, disruptionId, planId != null ? planId.value() : null);
        return result;
    }

    /** One simulated future for a "what if this site stops?" question. */
    public record WhatIfOutcome(String name, String description, SimulationResult result) {}

    /**
     * Asks: if these sites stop for {@code outageDays}, what happens over {@code horizonDays}, with no action and with each recovery option the twin can derive?
     * Nothing is saved: the incident and plans are built only for this run.
     */
    @Transactional(readOnly = true)
    public List<WhatIfOutcome> whatIf(WorkspaceId workspaceId, java.util.Set<com.supplygraph.domain.model.graph.NodeId> failedSites, int outageDays, int horizonDays) {
        DigitalTwinState twin = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("There is no supply chain yet. Build or import one first."));
        if (failedSites == null || failedSites.isEmpty()) throw new com.supplygraph.domain.error.ValidationException("Choose which site stops.");
        if (outageDays < 1 || horizonDays < outageDays) throw new com.supplygraph.domain.error.ValidationException("The look-ahead must be at least as long as the outage.");
        java.time.Instant start = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        Disruption d = whatIfDisruption(workspaceId, twin, failedSites, outageDays, start);
        DigitalTwinSnapshot snapshot = twin.createSnapshot();
        SimulationConfiguration config = new SimulationConfiguration(start, start.plusSeconds(horizonDays * 86400L), 86400L, 50000, true);

        List<WhatIfOutcome> out = new java.util.ArrayList<>();
        out.add(new WhatIfOutcome("Do nothing", "No recovery action is taken.", engine.run(snapshot, d, null, config)));
        for (var option : RecoveryOptionDeriver.derive(d, twin)) {
            RecoveryPlan plan = RecoveryPlan.createDraft(d.id(), workspaceId, snapshot.version(), com.supplygraph.domain.model.impact.ImpactAssessmentId.generate(),
                    option.name(), option.description(), List.of(option.action()), null);
            out.add(new WhatIfOutcome(option.name(), option.description(), engine.run(snapshot, d, plan, config)));
        }
        return out;
    }

    private static Disruption whatIfDisruption(WorkspaceId workspaceId, DigitalTwinState twin, java.util.Set<com.supplygraph.domain.model.graph.NodeId> failedSites, int outageDays, java.time.Instant start) {
        var first = twin.getGraph().getNode(failedSites.iterator().next());
        var category = switch (first.getType()) {
            case LOGISTICS_HUB, WAREHOUSE -> com.supplygraph.domain.model.disruption.DisruptionCategory.LOGISTICS_HUB_CLOSURE;
            case MANUFACTURING_SITE -> com.supplygraph.domain.model.disruption.DisruptionCategory.MANUFACTURING_SITE_OUTAGE;
            default -> com.supplygraph.domain.model.disruption.DisruptionCategory.SUPPLIER_SHUTDOWN;
        };
        return Disruption.createDraft(workspaceId, "GENERAL", "WHAT_IF", (failedSites.size() == 1 ? first.getName() : failedSites.size() + " sites including " + first.getName()) + " stop for " + outageDays + " days", "What-if question asked in the simulator",
                category, com.supplygraph.domain.model.disruption.DisruptionSeverity.HIGH, failedSites, java.util.Set.of(), java.util.Set.of(),
                start, start.plusSeconds(outageDays * 86400L),
                new com.supplygraph.domain.model.disruption.DisruptionImpactParameters(java.math.BigDecimal.valueOf(100), outageDays * 86400L, outageDays, java.math.BigDecimal.ZERO, "UNIT", java.util.Map.of()),
                "Simulator what-if");
    }

    /** For each scenario (a set of sites that stop together): how many units go unsupplied with no recovery action. Shows which failures matter most. */
    @Transactional(readOnly = true)
    public List<java.math.BigDecimal> impactOfEach(WorkspaceId workspaceId, List<java.util.Set<com.supplygraph.domain.model.graph.NodeId>> scenarios, int outageDays, int horizonDays) {
        DigitalTwinState twin = digitalTwinRepository.findByWorkspaceId(workspaceId).orElse(null);
        List<java.math.BigDecimal> out = new java.util.ArrayList<>();
        if (twin == null) return out;
        java.time.Instant start = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        DigitalTwinSnapshot snapshot = twin.createSnapshot();
        SimulationConfiguration config = new SimulationConfiguration(start, start.plusSeconds(Math.max(horizonDays, outageDays) * 86400L), 86400L, 50000, true);
        for (var failed : scenarios) {
            if (failed.isEmpty()) { out.add(java.math.BigDecimal.ZERO); continue; }
            Disruption d = whatIfDisruption(workspaceId, twin, failed, outageDays, start);
            out.add(engine.run(snapshot, d, null, config).totalShortageQuantity());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SimulationResult> listRuns(WorkspaceId workspaceId) {
        return simulationRunRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<SimulationResult> listSimulationRuns(WorkspaceId workspaceId) {
        return listRuns(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<SimulationResult> getRun(String simulationId) {
        return simulationRunRepository.findById(simulationId);
    }
}

