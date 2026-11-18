package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.NetworkResilienceAssessmentJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataResilienceRepository;
import com.supplygraph.application.port.out.ResilienceRepositoryPort;
import com.supplygraph.domain.model.intelligence.NetworkResilienceAssessment;
import com.supplygraph.domain.model.intelligence.ResilienceAssessmentId;
import com.supplygraph.domain.model.intelligence.ResilienceFactor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresResiliencePersistenceAdapter implements ResilienceRepositoryPort {

    private final SpringDataResilienceRepository repository;

    public PostgresResiliencePersistenceAdapter(SpringDataResilienceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(NetworkResilienceAssessment assessment) {
        NetworkResilienceAssessmentJpaEntity entity = repository.findById(assessment.id().value()).orElseGet(NetworkResilienceAssessmentJpaEntity::new);
        entity.setId(assessment.id().value());
        entity.setWorkspaceId(assessment.workspaceId());
        entity.setEvaluatedAt(assessment.assessedAt());
        entity.setOverallResilienceIndex(BigDecimal.valueOf(assessment.overallResilienceIndex()));
        entity.setSpofCount(assessment.singlePointsOfFailureCount());
        entity.setSupplierConcentrationHHI(BigDecimal.valueOf(assessment.supplierConcentrationHHI()));
        entity.setCriticalPathDepth(assessment.criticalPathDepth());
        entity.setAltCoverageRatio(BigDecimal.valueOf(assessment.alternativeSupplierCoverageRatio()));
        entity.setBottlenecksCount(assessment.recoveryBottlenecksCount());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NetworkResilienceAssessment> findById(ResilienceAssessmentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NetworkResilienceAssessment> findLatestByWorkspaceId(String workspaceId) {
        return repository.findFirstByWorkspaceIdOrderByAssessedAtDesc(workspaceId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetworkResilienceAssessment> findHistoryByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceIdOrderByAssessedAtDesc(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    private NetworkResilienceAssessment toDomain(NetworkResilienceAssessmentJpaEntity entity) {
        // Reconstruct factors based on persisted metrics
        double spofScore = Math.max(0.0, 100.0 - (entity.getSpofCount() * 25.0));
        double hhiScore = Math.max(0.0, 100.0 - (entity.getSupplierConcentrationHHI().doubleValue() / 100.0));
        double altScore = entity.getAltCoverageRatio().doubleValue() * 100.0;
        double depthScore = Math.max(0.0, 100.0 - (entity.getCriticalPathDepth() * 12.0));
        double bottleneckScore = Math.max(0.0, 100.0 - (entity.getBottlenecksCount() * 20.0));

        List<ResilienceFactor> factors = List.of(
                ResilienceFactor.of("Single Point of Failure Exposure", spofScore, 0.25, "Evaluated based on unredundant sole-feeder supplier nodes"),
                ResilienceFactor.of("Supplier Market Concentration (HHI)", hhiScore, 0.20, "Herfindahl-Hirschman index across primary component sourcing tiers"),
                ResilienceFactor.of("Alternative Supplier Coverage", altScore, 0.25, "Ratio of components with qualified secondary/tertiary backup sources"),
                ResilienceFactor.of("Topological Critical Path Depth", depthScore, 0.15, "Longest acyclic dependency sequence from raw extract to final customer"),
                ResilienceFactor.of("Logistics & Buffer Bottleneck Margin", bottleneckScore, 0.15, "Resilience against extended transit times and transshipment delays")
        );

        List<String> vulns = new ArrayList<>();
        if (entity.getSpofCount() > 0) {
            vulns.add("Network exhibits " + entity.getSpofCount() + " unmitigated single-points-of-failure");
        }
        if (entity.getBottlenecksCount() > 0) {
            vulns.add("Identified " + entity.getBottlenecksCount() + " logistics transit bottlenecks exceeding 14 days lead time");
        }

        List<String> mitigations = new ArrayList<>();
        if (entity.getSpofCount() > 0) {
            mitigations.add("Qualify secondary regional suppliers to eliminate single-point dependencies");
        }

        return new NetworkResilienceAssessment(
                ResilienceAssessmentId.of(entity.getId()),
                entity.getWorkspaceId(),
                entity.getEvaluatedAt(),
                entity.getOverallResilienceIndex().doubleValue(),
                entity.getSpofCount(),
                entity.getSupplierConcentrationHHI().doubleValue(),
                entity.getCriticalPathDepth(),
                entity.getAltCoverageRatio().doubleValue(),
                entity.getBottlenecksCount(),
                factors,
                vulns,
                mitigations
        );
    }
}

