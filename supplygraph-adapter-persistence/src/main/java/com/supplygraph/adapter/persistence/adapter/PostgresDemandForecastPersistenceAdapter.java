package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.DemandForecastJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataDemandForecastRepository;
import com.supplygraph.application.planning.port.out.DemandForecastRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.DemandForecastId;
import com.supplygraph.domain.model.planning.ForecastAccuracyMetrics;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PostgresDemandForecastPersistenceAdapter implements DemandForecastRepositoryPort {

    private final SpringDataDemandForecastRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresDemandForecastPersistenceAdapter(SpringDataDemandForecastRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void save(DemandForecast forecast) {
        try {
            String quantitiesJson = objectMapper.writeValueAsString(forecast.forecastedQuantities());
            String baselineJson = objectMapper.writeValueAsString(forecast.historicalBaseline());
            String overridesJson = objectMapper.writeValueAsString(forecast.periodOverrides());

            DemandForecastJpaEntity entity = new DemandForecastJpaEntity(
                    forecast.id().value(),
                    forecast.workspaceId().value(),
                    forecast.materialId().value(),
                    forecast.destinationLocationId().value(),
                    forecast.method().name(),
                    quantitiesJson,
                    baselineJson,
                    overridesJson,
                    forecast.accuracyMetrics().meanAbsoluteDeviation(),
                    forecast.accuracyMetrics().meanAbsolutePercentageError(),
                    forecast.accuracyMetrics().rootMeanSquareError(),
                    forecast.accuracyMetrics().trackingSignal(),
                    forecast.generatedAt()
            );

            repository.save(entity);
        } catch (Exception e) {
            throw new StorageFailureException("Failed to persist demand forecast: " + forecast.id().value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DemandForecast> findById(DemandForecastId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandForecast> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private DemandForecast toDomain(DemandForecastJpaEntity entity) {
        try {
            List<Double> quantities = objectMapper.readValue(entity.getForecastedQuantitiesJson(), new TypeReference<>() {});
            List<HistoricalDemandPoint> baseline = objectMapper.readValue(entity.getHistoricalBaselineJson(), new TypeReference<>() {});
            Map<Integer, Double> overrides = entity.getPeriodOverridesJson() != null
                    ? objectMapper.readValue(entity.getPeriodOverridesJson(), new TypeReference<>() {})
                    : Collections.emptyMap();

            return new DemandForecast(
                    new DemandForecastId(entity.getId()),
                    new WorkspaceId(entity.getWorkspaceId()),
                    new MaterialId(entity.getMaterialId()),
                    new NodeId(entity.getDestinationNodeId()),
                    ForecastMethod.valueOf(entity.getMethod()),
                    quantities,
                    baseline,
                    new ForecastAccuracyMetrics(entity.getMad(), entity.getMape(), entity.getRmse(), entity.getTrackingSignal()),
                    overrides,
                    entity.getGeneratedAt()
            );
        } catch (Exception e) {
            throw new StorageFailureException("Failed to deserialize demand forecast entity: " + entity.getId(), e);
        }
    }
}
