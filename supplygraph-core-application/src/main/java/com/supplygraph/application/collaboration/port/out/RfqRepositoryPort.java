package com.supplygraph.application.collaboration.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqId;

import java.util.List;
import java.util.Optional;

public interface RfqRepositoryPort {
    void save(RfqDocument rfq);
    Optional<RfqDocument> findById(RfqId id);
    List<RfqDocument> findByWorkspaceId(WorkspaceId workspaceId);
}

