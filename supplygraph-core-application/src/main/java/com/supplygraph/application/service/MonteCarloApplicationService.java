package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.MonteCarloRunRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.simulation.montecarlo.MonteCarloConfiguration;
import com.supplygraph.simulation.montecarlo.MonteCarloSimulator;
import com.supplygraph.simulation.montecarlo.MonteCarloSummaryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service managing seeded Monte Carlo uncertainty analysis runs.
 */
@Service
public class MonteCarloApplicationService {

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final DisruptionRepositoryPort disruptionRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final MonteCarloRunRepositoryPort monteCarloRunRepository;
    private final MonteCarloSimulator simulator = new MonteCarloSimulator();

    public MonteCarloApplicationService(
            DigitalTwinRepositoryPort digitalTwinRepository,
            DisruptionRepositoryPort disruptionRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            MonteCarloRunRepositoryPort monteCarloRunRepository
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.monteCarloRunRepository = Objects.requireNonNull(monteCarloRunRepository, "MonteCarloRunRepositoryPort must not be null");
    }

    @Transactional
    public MonteCarloSummaryResult runMonteCarlo(
            WorkspaceId workspaceId,
            DisruptionId disruptionId,
            RecoveryPlanId planId,
            MonteCarloConfiguration config
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
        MonteCarloSummaryResult summary = simulator.run(snapshot, disruption, plan, config);

        monteCarloRunRepository.save(summary, workspaceId, disruptionId, planId != null ? planId.value() : null);
        return summary;
    }

    @Transactional
    public MonteCarloSummaryResult runAnalysis(
            WorkspaceId workspaceId,
            DisruptionId disruptionId,
            RecoveryPlanId planId,
            MonteCarloConfiguration config
    ) {
        return runMonteCarlo(workspaceId, disruptionId, planId, config);
    }

    @Transactional(readOnly = true)
    public List<MonteCarloSummaryResult> listRuns(WorkspaceId workspaceId) {
        return monteCarloRunRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<MonteCarloSummaryResult> listAnalysisRuns(WorkspaceId workspaceId) {
        return listRuns(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<MonteCarloSummaryResult> getRun(String runId) {
        return monteCarloRunRepository.findById(runId);
    }
}

