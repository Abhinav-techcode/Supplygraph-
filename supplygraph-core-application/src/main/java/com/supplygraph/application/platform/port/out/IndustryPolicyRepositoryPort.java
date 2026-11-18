package com.supplygraph.application.platform.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.IndustryPolicyId;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;

import java.util.List;
import java.util.Optional;

public interface IndustryPolicyRepositoryPort {
    void save(IndustryPolicyRecord record);
    Optional<IndustryPolicyRecord> findById(IndustryPolicyId id);
    List<IndustryPolicyRecord> findByWorkspaceId(WorkspaceId workspaceId);
    List<IndustryPolicyRecord> findByWorkspaceIdAndIndustryCode(WorkspaceId workspaceId, String industryCode);
}

