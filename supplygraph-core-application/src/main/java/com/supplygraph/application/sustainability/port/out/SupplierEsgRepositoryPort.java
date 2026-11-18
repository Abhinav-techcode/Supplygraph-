package com.supplygraph.application.sustainability.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.EsgScorecardId;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;

import java.util.List;
import java.util.Optional;

public interface SupplierEsgRepositoryPort {
    void save(SupplierEsgScorecard scorecard);
    Optional<SupplierEsgScorecard> findById(EsgScorecardId id);
    Optional<SupplierEsgScorecard> findBySupplierId(WorkspaceId workspaceId, NodeId supplierId);
    List<SupplierEsgScorecard> findByWorkspaceId(WorkspaceId workspaceId);
}

