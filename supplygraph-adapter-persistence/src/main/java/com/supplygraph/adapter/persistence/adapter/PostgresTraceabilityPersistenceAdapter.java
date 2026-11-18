package com.supplygraph.adapter.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.adapter.persistence.entity.GenealogyDagJpaEntity;
import com.supplygraph.adapter.persistence.entity.TraceabilityNodeJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataGenealogyDagRepository;
import com.supplygraph.adapter.persistence.repository.SpringDataTraceabilityNodeRepository;
import com.supplygraph.application.sustainability.port.out.TraceabilityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.GenealogyEdge;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresTraceabilityPersistenceAdapter implements TraceabilityRepositoryPort {

    private final SpringDataGenealogyDagRepository dagRepository;
    private final SpringDataTraceabilityNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    public PostgresTraceabilityPersistenceAdapter(
            SpringDataGenealogyDagRepository dagRepository,
            SpringDataTraceabilityNodeRepository nodeRepository
    ) {
        this.dagRepository = dagRepository;
        this.nodeRepository = nodeRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @Transactional
    public void saveDag(WorkspaceId workspaceId, GenealogyDAG dag) {
        try {
            String nodesJson = objectMapper.writeValueAsString(dag.getAllNodes());
            String edgesJson = objectMapper.writeValueAsString(dag.getAllEdges());

            GenealogyDagJpaEntity entity = new GenealogyDagJpaEntity(
                    workspaceId.value(),
                    nodesJson,
                    edgesJson,
                    Instant.now()
            );

            dagRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist genealogy DAG for workspace: " + workspaceId.value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GenealogyDAG> findDagByWorkspaceId(WorkspaceId workspaceId) {
        return dagRepository.findById(workspaceId.value()).map(entity -> {
            try {
                List<TraceabilityNode> nodes = objectMapper.readValue(entity.getNodesJson(), new TypeReference<>() {});
                List<GenealogyEdge> edges = objectMapper.readValue(entity.getEdgesJson(), new TypeReference<>() {});

                GenealogyDAG dag = new GenealogyDAG();
                nodes.forEach(dag::addNode);
                edges.forEach(dag::addEdge);
                return dag;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize genealogy DAG for workspace: " + workspaceId.value(), e);
            }
        });
    }

    @Override
    @Transactional
    public void saveNode(WorkspaceId workspaceId, TraceabilityNode node) {
        TraceabilityNodeJpaEntity entity = new TraceabilityNodeJpaEntity(
                node.id().value(),
                workspaceId.value(),
                node.batchNumber(),
                node.type().name(),
                node.materialId().value(),
                node.locationId().value(),
                node.quantity(),
                node.unitOfMeasure(),
                node.custodyHolder(),
                node.recordedAt()
        );

        nodeRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TraceabilityNode> findNodeById(TraceabilityNodeId id) {
        return nodeRepository.findById(id.value()).map(this::toDomain);
    }

    private TraceabilityNode toDomain(TraceabilityNodeJpaEntity entity) {
        return new TraceabilityNode(
                new TraceabilityNodeId(entity.getId()),
                entity.getBatchNumber(),
                TraceabilityNodeType.valueOf(entity.getNodeType()),
                new MaterialId(entity.getMaterialId()),
                new NodeId(entity.getLocationId()),
                entity.getQuantity(),
                entity.getUnitOfMeasure(),
                entity.getCustodyHolder(),
                entity.getRecordedAt()
        );
    }
}

