package com.supplygraph.application.sustainability.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecordId;

import java.util.List;
import java.util.Optional;

public interface CarbonEmissionRepositoryPort {
    void save(CarbonEmissionRecord record);
    Optional<CarbonEmissionRecord> findById(CarbonEmissionRecordId id);
    List<CarbonEmissionRecord> findByWorkspaceId(WorkspaceId workspaceId);
}

