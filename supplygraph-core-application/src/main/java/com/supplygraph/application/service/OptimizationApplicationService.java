package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.OptimizationRunRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.optimization.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service managing constraint-based recovery optimization runs.
 */
@Service
public class OptimizationApplicationService {

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final DisruptionRepositoryPort disruptionRepository;
    private final OptimizationRunRepositoryPort optimizationRunRepository;
    private final OrToolsRecoveryOptimizer optimizer = new OrToolsRecoveryOptimizer();

    public OptimizationApplicationService(
            DigitalTwinRepositoryPort digitalTwinRepository,
            DisruptionRepositoryPort disruptionRepository,
            OptimizationRunRepositoryPort optimizationRunRepository
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.optimizationRunRepository = Objects.requireNonNull(optimizationRunRepository, "OptimizationRunRepositoryPort must not be null");
    }

    @Transactional
    public OptimizationResult solve(WorkspaceId workspaceId, DisruptionId disruptionId, RecoveryOptimizationProblem problem) {
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + disruptionId.value()));

        OptimizationResult result = optimizer.solve(problem);
        optimizationRunRepository.save(result, workspaceId, disruptionId);
        return result;
    }

    @Transactional
    public OptimizationResult solveRecoveryProblem(WorkspaceId workspaceId, DisruptionId disruptionId, RecoveryOptimizationProblem problem) {
        return solve(workspaceId, disruptionId, problem);
    }

    @Transactional
    public OptimizationResult solveRecovery(
            WorkspaceId workspaceId,
            DisruptionId disruptionId,
            BigDecimal demand,
            List<AlternativeOption> candidates,
            OptimizationObjective objective,
            OptimizationWeights weights,
            int timeLimitSeconds
    ) {
        com.supplygraph.domain.model.twin.DigitalTwinState state = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found: " + workspaceId.value()));
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + disruptionId.value()));
        com.supplygraph.domain.model.twin.DigitalTwinSnapshot snapshot = state.createSnapshot();

        RecoveryOptimizationProblem problem = new RecoveryOptimizationProblem(
                disruption,
                snapshot,
                demand,
                candidates,
                List.of(),
                objective,
                weights,
                timeLimitSeconds
        );
        return solve(workspaceId, disruptionId, problem);
    }

    @Transactional(readOnly = true)
    public List<OptimizationResult> listRuns(WorkspaceId workspaceId) {
        return optimizationRunRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<OptimizationResult> listOptimizationRuns(WorkspaceId workspaceId) {
        return listRuns(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<OptimizationResult> getRun(String runId) {
        return optimizationRunRepository.findById(runId);
    }
}

