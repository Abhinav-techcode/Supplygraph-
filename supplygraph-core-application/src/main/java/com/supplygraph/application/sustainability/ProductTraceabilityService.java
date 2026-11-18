package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.TraceabilityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.GenealogyEdge;
import com.supplygraph.domain.model.sustainability.RecallImpactResult;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductTraceabilityService {

    private final TraceabilityRepositoryPort traceabilityRepository;

    public ProductTraceabilityService(TraceabilityRepositoryPort traceabilityRepository) {
        this.traceabilityRepository = traceabilityRepository;
    }

    public GenealogyDAG getOrCreateDag(WorkspaceId workspaceId) {
        return traceabilityRepository.findDagByWorkspaceId(workspaceId)
                .orElseGet(() -> {
                    GenealogyDAG dag = new GenealogyDAG();
                    traceabilityRepository.saveDag(workspaceId, dag);
                    return dag;
                });
    }

    public void registerNode(WorkspaceId workspaceId, TraceabilityNode node) {
        GenealogyDAG dag = getOrCreateDag(workspaceId);
        dag.addNode(node);
        traceabilityRepository.saveDag(workspaceId, dag);
        traceabilityRepository.saveNode(workspaceId, node);
    }

    public void linkNodes(WorkspaceId workspaceId, TraceabilityNodeId source, TraceabilityNodeId target, double qty, String details) {
        GenealogyDAG dag = getOrCreateDag(workspaceId);
        dag.addEdge(new GenealogyEdge(source, target, qty, details));
        traceabilityRepository.saveDag(workspaceId, dag);
    }

    public List<TraceabilityNode> traceBackwardToOrigin(WorkspaceId workspaceId, TraceabilityNodeId targetLotId) {
        GenealogyDAG dag = getOrCreateDag(workspaceId);
        return dag.backwardTrace(targetLotId);
    }

    public RecallImpactResult executeRecallImpactAnalysis(WorkspaceId workspaceId, String taintedBatchNumber) {
        GenealogyDAG dag = getOrCreateDag(workspaceId);
        TraceabilityNode taintedNode = dag.findByBatchNumber(taintedBatchNumber)
                .orElseThrow(() -> new IllegalArgumentException("Batch number not found in genealogy ledger: " + taintedBatchNumber));

        List<TraceabilityNode> affectedNodes = dag.forwardTrace(taintedNode.id());

        List<NodeId> affectedLocations = affectedNodes.stream()
                .map(TraceabilityNode::locationId)
                .distinct()
                .toList();

        List<String> affectedShipments = affectedNodes.stream()
                .filter(n -> n.type() == TraceabilityNodeType.SHIPMENT_CONSIGNMENT)
                .map(TraceabilityNode::batchNumber)
                .toList();

        double totalVolume = affectedNodes.stream()
                .mapToDouble(TraceabilityNode::quantity)
                .sum();

        return new RecallImpactResult(
                taintedBatchNumber,
                affectedNodes,
                affectedLocations,
                affectedShipments,
                totalVolume
        );
    }
}

