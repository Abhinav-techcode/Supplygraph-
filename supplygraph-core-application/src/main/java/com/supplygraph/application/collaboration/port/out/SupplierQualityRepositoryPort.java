package com.supplygraph.application.collaboration.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.QualityIncident8DId;

import java.util.List;
import java.util.Optional;

public interface SupplierQualityRepositoryPort {
    void save(QualityIncident8D incident);
    Optional<QualityIncident8D> findById(QualityIncident8DId id);
    List<QualityIncident8D> findByWorkspaceId(WorkspaceId workspaceId);
}

