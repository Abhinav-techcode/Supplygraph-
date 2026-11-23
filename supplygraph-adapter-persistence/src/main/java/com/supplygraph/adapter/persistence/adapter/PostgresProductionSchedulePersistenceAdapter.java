package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ProductionScheduleJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataProductionScheduleRepository;
import com.supplygraph.application.planning.port.out.ProductionScheduleRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ProductionScheduleId;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresProductionSchedulePersistenceAdapter implements ProductionScheduleRepositoryPort {

    private final SpringDataProductionScheduleRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresProductionSchedulePersistenceAdapter(SpringDataProductionScheduleRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void save(FiniteProductionSchedule schedule) {
        try {
            String jobsJson = objectMapper.writeValueAsString(schedule.scheduledJobs());

            ProductionScheduleJpaEntity entity = new ProductionScheduleJpaEntity(
                    schedule.id().value(),
                    schedule.workspaceId().value(),
                    schedule.plantId().value(),
                    schedule.scheduleName(),
                    jobsJson,
                    schedule.averageCapacityUtilization(),
                    schedule.generatedAt()
            );

            repository.save(entity);
        } catch (Exception e) {
            throw new StorageFailureException("Failed to persist production schedule: " + schedule.id().value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiniteProductionSchedule> findById(ProductionScheduleId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiniteProductionSchedule> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private FiniteProductionSchedule toDomain(ProductionScheduleJpaEntity entity) {
        try {
            List<ScheduledProductionJob> jobs = objectMapper.readValue(entity.getScheduledJobsJson(), new TypeReference<>() {});

            return new FiniteProductionSchedule(
                    new ProductionScheduleId(entity.getId()),
                    new WorkspaceId(entity.getWorkspaceId()),
                    new NodeId(entity.getPlantId()),
                    entity.getScheduleName(),
                    jobs,
                    entity.getAverageCapacityUtilization(),
                    entity.getGeneratedAt()
            );
        } catch (Exception e) {
            throw new StorageFailureException("Failed to deserialize production schedule entity: " + entity.getId(), e);
        }
    }
}
