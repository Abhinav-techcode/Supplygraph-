package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.DataQualityFindingJpaEntity;
import com.supplygraph.adapter.persistence.entity.DataQualityReportJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataQualityReportRepository;
import com.supplygraph.application.port.out.DataQualityRepositoryPort;
import com.supplygraph.domain.model.quality.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class PostgresDataQualityPersistenceAdapter implements DataQualityRepositoryPort {

    private final SpringDataQualityReportRepository repository;

    public PostgresDataQualityPersistenceAdapter(SpringDataQualityReportRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(DataQualityReport report) {
        DataQualityReportJpaEntity entity = repository.findById(report.id().value()).orElseGet(DataQualityReportJpaEntity::new);
        entity.setId(report.id().value());
        entity.setWorkspaceId(report.workspaceId());
        entity.setEvaluatedAt(report.evaluatedAt());
        entity.setOverallScore(BigDecimal.valueOf(report.overallScore()));
        entity.setTotalEntities(report.totalEntitiesEvaluated());
        entity.setCleanRecords(report.cleanRecordsCount());
        entity.setFlawedRecords(report.flawedRecordsCount());

        entity.getFindings().clear();
        for (DataQualityFinding finding : report.findings()) {
            DataQualityFindingJpaEntity fe = new DataQualityFindingJpaEntity();
            fe.setReport(entity);
            fe.setRuleId(finding.ruleId());
            fe.setDimension(finding.dimension().name());
            fe.setEntityId(finding.entityId());
            fe.setFieldName(finding.field());
            fe.setActualValue(finding.actualValue());
            fe.setMessage(finding.message());
            fe.setSeverity(finding.severity().name());
            fe.setSuggestedRemediation(finding.suggestedRemediation());
            entity.getFindings().add(fe);
        }

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataQualityReport> findById(DataQualityReportId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataQualityReport> findLatestByWorkspaceId(String workspaceId) {
        return repository.findFirstByWorkspaceIdOrderByEvaluatedAtDesc(workspaceId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataQualityReport> findHistoryByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceIdOrderByEvaluatedAtDesc(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    private DataQualityReport toDomain(DataQualityReportJpaEntity entity) {
        List<DataQualityFinding> findings = new ArrayList<>();
        if (entity.getFindings() != null) {
            for (DataQualityFindingJpaEntity f : entity.getFindings()) {
                findings.add(new DataQualityFinding(
                        f.getRuleId(),
                        QualityDimension.valueOf(f.getDimension()),
                        f.getEntityId(),
                        f.getFieldName(),
                        f.getActualValue(),
                        f.getMessage(),
                        DataQualitySeverity.valueOf(f.getSeverity()),
                        f.getSuggestedRemediation()
                ));
            }
        }

        Map<QualityDimension, Double> dimensionScores = new EnumMap<>(QualityDimension.class);
        for (QualityDimension dim : QualityDimension.values()) {
            dimensionScores.put(dim, entity.getOverallScore().doubleValue());
        }

        return new DataQualityReport(
                DataQualityReportId.of(entity.getId()),
                entity.getWorkspaceId(),
                entity.getEvaluatedAt(),
                entity.getOverallScore().doubleValue(),
                dimensionScores,
                findings,
                entity.getTotalEntities(),
                entity.getCleanRecords(),
                entity.getFlawedRecords()
        );
    }
}
