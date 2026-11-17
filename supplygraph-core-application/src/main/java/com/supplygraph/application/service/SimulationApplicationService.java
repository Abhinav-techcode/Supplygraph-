package com.supplygraph.application.service;

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
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + workspaceId.value()));
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + disruptionId.value()));

        RecoveryPlan plan = null;
        if (planId != null) {
            plan = recoveryPlanRepository.findById(planId).orElse(null);
        }

        DigitalTwinSnapshot snapshot = twinState.createSnapshot();
        SimulationResult result = engine.run(snapshot, disruption, plan, config);

        simulationRunRepository.save(result, workspaceId, disruptionId, planId != null ? planId.value() : null);
        return result;
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

