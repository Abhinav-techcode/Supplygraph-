package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ConnectorId;

import java.util.List;
import java.util.Optional;

public interface ConnectorRepositoryPort {
    Connector save(Connector connector);
    Optional<Connector> findById(ConnectorId id);
    List<Connector> findByWorkspace(WorkspaceId workspaceId);
}
