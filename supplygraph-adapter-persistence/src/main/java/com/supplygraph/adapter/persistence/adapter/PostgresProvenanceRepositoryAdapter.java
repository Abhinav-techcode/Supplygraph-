package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.ProvenanceRecordJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataProvenanceRepository;
import com.supplygraph.application.port.out.ProvenanceRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.provenance.ProvenanceId;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PostgresProvenanceRepositoryAdapter implements ProvenanceRepositoryPort {

    private final SpringDataProvenanceRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresProvenanceRepositoryAdapter(SpringDataProvenanceRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public ProvenanceRecord save(ProvenanceRecord record) {
        String attrsJson = serialize(record.attributes());
        ProvenanceRecordJpaEntity entity = new ProvenanceRecordJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.entityId(),
                record.entityType(),
                record.type().name(),
                record.sourceReference(),
                record.stateVersion(),
                record.timestamp(),
                attrsJson
        );
        ProvenanceRecordJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ProvenanceRecord> findById(ProvenanceId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ProvenanceRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceIdOrderByTimestampDesc(workspaceId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ProvenanceRecord> findByEntityId(String entityId) {
        return repository.findByEntityIdOrderByTimestampDesc(entityId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ProvenanceRecord> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private ProvenanceRecord toDomain(ProvenanceRecordJpaEntity entity) {
        Map<String, String> attrs = deserialize(entity.getAttributesJson(), new TypeReference<Map<String, String>>() {});
        return new ProvenanceRecord(
                ProvenanceId.of(entity.getId()),
                WorkspaceId.of(entity.getWorkspaceId()),
                entity.getEntityId(),
                entity.getEntityType(),
                ProvenanceType.valueOf(entity.getProvenanceType()),
                entity.getSourceReference(),
                entity.getStateVersion(),
                entity.getTimestamp(),
                attrs != null ? attrs : Map.of()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T deserialize(String json, TypeReference<T> ref) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            return null;
        }
    }
}

