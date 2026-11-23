package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.error.StorageFailureException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.OperationalEventJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataOperationalEventRepository;
import com.supplygraph.application.port.out.OperationalEventRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresOperationalEventRepositoryAdapter implements OperationalEventRepositoryPort {

    private final SpringDataOperationalEventRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresOperationalEventRepositoryAdapter(SpringDataOperationalEventRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void save(OperationalEvent event) {
        String payloadJson = serialize(event.getPayload());
        String provenanceJson = serialize(event.getProvenance());

        OperationalEventJpaEntity entity = new OperationalEventJpaEntity(
                event.getEventId().value(),
                event.getEventType().name(),
                event.getSource().name(),
                event.getEventTimestamp(),
                event.getReceivedTimestamp(),
                event.getAffectedEntityId(),
                event.getAffectedEntityType(),
                event.getWorkspaceId().value(),
                payloadJson,
                provenanceJson,
                event.getSchemaVersion(),
                event.getIdempotencyKey(),
                event.getStatus().name(),
                event.getRejectionReason().name()
        );
        repository.save(entity);
    }

    @Override
    public Optional<OperationalEvent> findById(EventId eventId) {
        return repository.findById(eventId.value()).map(this::toDomain);
    }

    @Override
    public Optional<OperationalEvent> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<OperationalEvent> findByWorkspaceId(WorkspaceId workspaceId, int limit) {
        return repository.findByWorkspaceIdOrderByEventTimestampDesc(workspaceId.value(), PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<OperationalEvent> findRecent(int limit) {
        return repository.findAllByOrderByEventTimestampDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    private OperationalEvent toDomain(OperationalEventJpaEntity entity) {
        Map<String, String> payload = deserialize(entity.getPayloadJson(), new TypeReference<Map<String, String>>() {});
        EventProvenance provenance = deserialize(entity.getProvenanceJson(), new TypeReference<EventProvenance>() {});

        return new OperationalEvent(
                EventId.of(entity.getId()),
                EventType.valueOf(entity.getEventType()),
                EventSource.valueOf(entity.getSource()),
                entity.getEventTimestamp(),
                entity.getReceivedTimestamp(),
                entity.getAffectedEntityId(),
                entity.getAffectedEntityType(),
                WorkspaceId.of(entity.getWorkspaceId()),
                payload != null ? payload : Map.of(),
                provenance != null ? provenance : EventProvenance.internal("UNKNOWN"),
                entity.getSchemaVersion(),
                entity.getIdempotencyKey(),
                EventStatus.valueOf(entity.getStatus()),
                EventRejectionReason.valueOf(entity.getRejectionReason())
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresOperationalEventRepositoryAdapter)", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> ref) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            throw new StorageFailureException("Stored data could not be processed (PostgresOperationalEventRepositoryAdapter)", e);
        }
    }
}

