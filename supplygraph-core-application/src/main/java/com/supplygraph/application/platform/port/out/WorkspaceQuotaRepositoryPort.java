package com.supplygraph.application.platform.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaRecord;

import java.util.Optional;

public interface WorkspaceQuotaRepositoryPort {
    void save(WorkspaceQuotaRecord record);
    Optional<WorkspaceQuotaRecord> findById(WorkspaceQuotaId id);
    Optional<WorkspaceQuotaRecord> findByWorkspaceId(WorkspaceId workspaceId);
}

