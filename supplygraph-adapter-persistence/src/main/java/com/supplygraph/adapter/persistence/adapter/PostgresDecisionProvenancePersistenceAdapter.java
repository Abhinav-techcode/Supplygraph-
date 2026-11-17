package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.DecisionProvenanceEdgeJpaEntity;
import com.supplygraph.adapter.persistence.entity.DecisionProvenanceNodeJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataDecisionProvenanceEdgeRepository;
import com.supplygraph.adapter.persistence.repository.SpringDataDecisionProvenanceNodeRepository;
import com.supplygraph.application.port.out.DecisionProvenanceRepositoryPort;
import com.supplygraph.domain.model.provenance.graph.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgresDecisionProvenancePersistenceAdapter implements DecisionProvenanceRepositoryPort {

    private final SpringDataDecisionProvenanceNodeRepository nodeRepository;
    private final SpringDataDecisionProvenanceEdgeRepository edgeRepository;
    private final ObjectMapper objectMapper;

    public PostgresDecisionProvenancePersistenceAdapter(
            SpringDataDecisionProvenanceNodeRepository nodeRepository,
            SpringDataDecisionProvenanceEdgeRepository edgeRepository
    ) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void saveGraph(DecisionProvenanceGraph graph) {
        for (DecisionProvenanceNode node : graph.getAllNodes()) {
            String metadataJson = serialize(node.metadata());
            DecisionProvenanceNodeJpaEntity entity = new DecisionProvenanceNodeJpaEntity(
                    node.id().value(),
                    graph.workspaceId(),
                    graph.rootDisruptionId(),
                    node.type().name(),
                    node.label(),
                    node.entityReferenceId(),
                    node.timestamp(),
                    metadataJson
            );
            nodeRepository.save(entity);
        }

        for (DecisionProvenanceEdge edge : graph.edges()) {
            DecisionProvenanceEdgeJpaEntity entity = new DecisionProvenanceEdgeJpaEntity(
                    edge.id(),
                    graph.workspaceId(),
                    graph.rootDisruptionId(),
                    edge.sourceNodeId().value(),
                    edge.targetNodeId().value(),
                    edge.type().name(),
                    edge.explanation(),
                    edge.timestamp()
            );
            edgeRepository.save(entity);
        }
    }

    @Override
    public Optional<DecisionProvenanceGraph> findByRootDisruptionId(String disruptionId) {
        List<DecisionProvenanceNodeJpaEntity> nodeEntities = nodeRepository.findByDisruptionIdOrderByCreatedAtAsc(disruptionId);
        if (nodeEntities.isEmpty()) {
            return Optional.empty();
        }

        List<DecisionProvenanceEdgeJpaEntity> edgeEntities = edgeRepository.findByDisruptionIdOrderByCreatedAtAsc(disruptionId);
        String workspaceId = nodeEntities.get(0).getWorkspaceId();

        List<DecisionProvenanceNode> nodes = nodeEntities.stream().map(this::toNodeDomain).toList();
        List<DecisionProvenanceEdge> edges = edgeEntities.stream().map(this::toEdgeDomain).toList();

        return Optional.of(DecisionProvenanceGraph.create(workspaceId, disruptionId, nodes, edges));
    }

    @Override
    public Optional<DecisionProvenanceGraph> findByWorkspaceId(String workspaceId) {
        List<DecisionProvenanceNodeJpaEntity> nodeEntities = nodeRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        if (nodeEntities.isEmpty()) {
            return Optional.empty();
        }

        String disruptionId = nodeEntities.get(0).getDisruptionId();
        List<DecisionProvenanceEdgeJpaEntity> edgeEntities = edgeRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);

        List<DecisionProvenanceNode> nodes = nodeEntities.stream().map(this::toNodeDomain).toList();
        List<DecisionProvenanceEdge> edges = edgeEntities.stream().map(this::toEdgeDomain).toList();

        return Optional.of(DecisionProvenanceGraph.create(workspaceId, disruptionId, nodes, edges));
    }

    private DecisionProvenanceNode toNodeDomain(DecisionProvenanceNodeJpaEntity e) {
        Map<String, String> meta = deserialize(e.getMetadataJson(), new TypeReference<Map<String, String>>() {}, Map.of());
        return DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of(e.getId()),
                DecisionProvenanceNodeType.valueOf(e.getNodeType()),
                e.getLabel(),
                e.getEntityReferenceId(),
                e.getCreatedAt(),
                meta
        );
    }

    private DecisionProvenanceEdge toEdgeDomain(DecisionProvenanceEdgeJpaEntity e) {
        return DecisionProvenanceEdge.of(
                e.getId(),
                DecisionProvenanceNodeId.of(e.getSourceNodeId()),
                DecisionProvenanceNodeId.of(e.getTargetNodeId()),
                DecisionProvenanceEdgeType.valueOf(e.getEdgeType()),
                e.getExplanation(),
                e.getCreatedAt()
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef, T defaultVal) {
        if (json == null || json.isBlank()) return defaultVal;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
