package com.supplygraph.application.platform.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.ApiKeyId;
import com.supplygraph.domain.model.platform.ApiKeyRecord;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepositoryPort {
    void save(ApiKeyRecord record);
    Optional<ApiKeyRecord> findById(ApiKeyId id);
    List<ApiKeyRecord> findByWorkspaceId(WorkspaceId workspaceId);
    Optional<ApiKeyRecord> findByHashedToken(String hashedToken);
}

