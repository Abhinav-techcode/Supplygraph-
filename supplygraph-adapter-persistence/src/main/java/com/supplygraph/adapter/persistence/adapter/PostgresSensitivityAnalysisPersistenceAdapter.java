package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.SensitivityReportJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataSensitivityReportRepository;
import com.supplygraph.application.port.out.SensitivityAnalysisRepositoryPort;
import com.supplygraph.domain.model.sensitivity.SensitivityAnalysisReport;
import com.supplygraph.domain.model.sensitivity.SensitivityTrialResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresSensitivityAnalysisPersistenceAdapter implements SensitivityAnalysisRepositoryPort {

    private final SpringDataSensitivityReportRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresSensitivityAnalysisPersistenceAdapter(SpringDataSensitivityReportRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(SensitivityAnalysisReport report) {
        String trialsJson = serialize(report.trials());
        SensitivityReportJpaEntity entity = new SensitivityReportJpaEntity(
                report.reportId(),
                report.workspaceId(),
                report.alternativeId(),
                report.baselineCostUsd(),
                report.baselineRecoveryDays(),
                report.baselineServiceLevelPct(),
                trialsJson,
                report.overallRobustnessScore(),
                report.generatedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<SensitivityAnalysisReport> findById(String reportId) {
        return repository.findById(reportId).map(this::toDomain);
    }

    @Override
    public List<SensitivityAnalysisReport> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<SensitivityAnalysisReport> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private SensitivityAnalysisReport toDomain(SensitivityReportJpaEntity e) {
        List<SensitivityTrialResult> trials = deserialize(e.getTrialsJson(), new TypeReference<List<SensitivityTrialResult>>() {}, List.of());
        return SensitivityAnalysisReport.of(
                e.getId(),
                e.getWorkspaceId(),
                e.getAlternativeId(),
                e.getBaselineCostUsd(),
                e.getBaselineRecoveryDays(),
                e.getBaselineServicePct(),
                trials,
                e.getOverallRobustnessScore(),
                e.getGeneratedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresSensitivityAnalysisPersistenceAdapter)", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef, T defaultVal) {
        if (json == null || json.isBlank()) return defaultVal;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresSensitivityAnalysisPersistenceAdapter)", e);
        }
    }
}

