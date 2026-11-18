package com.supplygraph.application.sustainability.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;

import java.util.List;
import java.util.Optional;

public interface TraceabilityRepositoryPort {
    void saveDag(WorkspaceId workspaceId, GenealogyDAG dag);
    Optional<GenealogyDAG> findDagByWorkspaceId(WorkspaceId workspaceId);
    void saveNode(WorkspaceId workspaceId, TraceabilityNode node);
    Optional<TraceabilityNode> findNodeById(TraceabilityNodeId id);
}

