package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.EnvironmentalScenarioRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenarioId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EnvironmentalScenarioService {

    private final EnvironmentalScenarioRepositoryPort scenarioRepository;

    public EnvironmentalScenarioService(EnvironmentalScenarioRepositoryPort scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    public EnvironmentalScenario simulateScenario(
            WorkspaceId workspaceId,
            String scenarioName,
            double baselineCo2Tons,
            double projectedCo2Tons,
            BigDecimal totalAbatementCostUsd
    ) {
        double tonsAvoided = Math.max(0.001, baselineCo2Tons - projectedCo2Tons);
        double reductionPct = baselineCo2Tons > 0 ? (tonsAvoided / baselineCo2Tons) * 100.0 : 0.0;

        BigDecimal costPerTon = totalAbatementCostUsd.divide(BigDecimal.valueOf(tonsAvoided), 2, RoundingMode.HALF_UP);

        EnvironmentalScenario scenario = new EnvironmentalScenario(
                EnvironmentalScenarioId.generate(),
                workspaceId,
                scenarioName,
                baselineCo2Tons,
                projectedCo2Tons,
                reductionPct,
                totalAbatementCostUsd,
                costPerTon,
                Instant.now()
        );

        scenarioRepository.save(scenario);
        return scenario;
    }

    public Optional<EnvironmentalScenario> getScenario(EnvironmentalScenarioId id) {
        return scenarioRepository.findById(id);
    }

    public List<EnvironmentalScenario> listScenarios(WorkspaceId workspaceId) {
        return scenarioRepository.findByWorkspaceId(workspaceId);
    }
}

