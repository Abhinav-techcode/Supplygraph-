package com.supplygraph.domain.model.platform;

import com.supplygraph.domain.model.WorkspaceId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ApiKeyRecord(
        ApiKeyId id,
        WorkspaceId workspaceId,
        String keyName,
        String hashedToken,
        List<ApiScope> scopes,
        boolean active,
        Instant createdAt
) implements Serializable {
    public ApiKeyRecord {
        Objects.requireNonNull(id, "ApiKeyId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(keyName, "KeyName cannot be null");
        Objects.requireNonNull(hashedToken, "HashedToken cannot be null");
        Objects.requireNonNull(scopes, "Scopes cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        scopes = Collections.unmodifiableList(List.copyOf(scopes));
    }

    public ApiKeyRecord withActive(boolean newActive) {
        return new ApiKeyRecord(id, workspaceId, keyName, hashedToken, scopes, newActive, createdAt);
    }
}

