package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ImpactReassessmentJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataImpactReassessmentRepository;
import com.supplygraph.application.port.out.ImpactReassessmentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.reassessment.ImpactReassessment;
import com.supplygraph.domain.model.reassessment.ImpactReassessmentId;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PostgresImpactReassessmentRepositoryAdapter implements ImpactReassessmentRepositoryPort {

    private final SpringDataImpactReassessmentRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresImpactReassessmentRepositoryAdapter(SpringDataImpactReassessmentRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(ImpactReassessment reassessment) {
        String nodesJson = serialize(reassessment.getAffectedNodeIds().stream().map(NodeId::value).toList());
        String lanesJson = serialize(reassessment.getAffectedLaneIds().stream().map(EdgeId::value).toList());
        String matsJson = serialize(reassessment.getAffectedMaterialIds().stream().map(MaterialId::value).toList());
        String preservedJson = serialize(reassessment.getPreservedScope());
        String invalidatedJson = serialize(reassessment.getInvalidatedScope());
        String assessmentsJson = serialize(reassessment.getAffectedAssessmentIds());
        String plansJson = serialize(reassessment.getAffectedPlanIds());

        ImpactReassessmentJpaEntity entity = new ImpactReassessmentJpaEntity(
                reassessment.getId().value(),
                reassessment.getWorkspaceId().value(),
                reassessment.getTriggerEventId() != null ? reassessment.getTriggerEventId().value() : null,
                reassessment.getPreviousStateVersion().value(),
                reassessment.getNewStateVersion().value(),
                nodesJson,
                lanesJson,
                matsJson,
                preservedJson,
                invalidatedJson,
                assessmentsJson,
                plansJson,
                reassessment.getReassessedAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<ImpactReassessment> findById(ImpactReassessmentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ImpactReassessment> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByReassessedAtDesc(workspaceId.value())
                .stream().map(this::toDomain).toList();
    }

    private ImpactReassessment toDomain(ImpactReassessmentJpaEntity entity) {
        List<String> nodeStrs = deserialize(entity.getAffectedNodeIds(), new TypeReference<List<String>>() {});
        List<String> laneStrs = deserialize(entity.getAffectedLaneIds(), new TypeReference<List<String>>() {});
        List<String> matStrs = deserialize(entity.getAffectedMaterialIds(), new TypeReference<List<String>>() {});
        List<String> preserved = deserialize(entity.getPreservedScopeJson(), new TypeReference<List<String>>() {});
        List<String> invalidated = deserialize(entity.getInvalidatedScopeJson(), new TypeReference<List<String>>() {});
        List<String> assessments = deserialize(entity.getAffectedAssessmentIds(), new TypeReference<List<String>>() {});
        List<String> plans = deserialize(entity.getAffectedPlanIds(), new TypeReference<List<String>>() {});

        Set<NodeId> nodes = nodeStrs != null ? nodeStrs.stream().map(NodeId::of).collect(Collectors.toSet()) : Set.of();
        Set<EdgeId> lanes = laneStrs != null ? laneStrs.stream().map(EdgeId::of).collect(Collectors.toSet()) : Set.of();
        Set<MaterialId> mats = matStrs != null ? matStrs.stream().map(MaterialId::of).collect(Collectors.toSet()) : Set.of();

        return new ImpactReassessment(
                ImpactReassessmentId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                entity.getTriggerEventId() != null ? EventId.of(entity.getTriggerEventId()) : null,
                StateVersion.of(entity.getPreviousVersion()),
                StateVersion.of(entity.getNewVersion()),
                nodes,
                lanes,
                mats,
                preserved != null ? preserved : List.of(),
                invalidated != null ? invalidated : List.of(),
                assessments != null ? assessments : List.of(),
                plans != null ? plans : List.of(),
                entity.getReassessedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresImpactReassessmentRepositoryAdapter)", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> ref) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresImpactReassessmentRepositoryAdapter)", e);
        }
    }
}

