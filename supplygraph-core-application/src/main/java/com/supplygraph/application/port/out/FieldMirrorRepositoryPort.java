package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.sync.FieldMirror;

import java.util.List;
import java.util.Optional;

public interface FieldMirrorRepositoryPort {
    Optional<FieldMirror> find(WorkspaceId ws, ConnectorId connector, String entityType, String entityKey, String field);
    FieldMirror save(FieldMirror mirror);
    List<FieldMirror> listByWorkspace(WorkspaceId ws, int limit);
}
