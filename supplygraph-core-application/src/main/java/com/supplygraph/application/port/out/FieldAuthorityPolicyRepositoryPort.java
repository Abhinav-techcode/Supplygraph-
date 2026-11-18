package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sync.FieldAuthorityPolicy;

import java.util.List;

public interface FieldAuthorityPolicyRepositoryPort {
    List<FieldAuthorityPolicy> findByWorkspace(WorkspaceId ws);
    FieldAuthorityPolicy save(FieldAuthorityPolicy policy);
}
