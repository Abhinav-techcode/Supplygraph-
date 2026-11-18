package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.platform.ApiKeyJpaEntity;
import com.supplygraph.adapter.persistence.repository.platform.SpringDataApiKeyRepository;
import com.supplygraph.application.platform.port.out.ApiKeyRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.ApiKeyId;
import com.supplygraph.domain.model.platform.ApiKeyRecord;
import com.supplygraph.domain.model.platform.ApiScope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PostgresApiKeyPersistenceAdapter implements ApiKeyRepositoryPort {

    private final SpringDataApiKeyRepository repository;

    public PostgresApiKeyPersistenceAdapter(SpringDataApiKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ApiKeyRecord record) {
        String scopesCsv = record.scopes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        ApiKeyJpaEntity entity = new ApiKeyJpaEntity(
                record.id().value(),
                record.workspaceId().value(),
                record.keyName(),
                record.hashedToken(),
                scopesCsv,
                record.active(),
                record.createdAt()
        );
        repository.save(entity);
    }

    @Override
    public Optional<ApiKeyRecord> findById(ApiKeyId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ApiKeyRecord> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ApiKeyRecord> findByHashedToken(String hashedToken) {
        return repository.findByHashedToken(hashedToken).map(this::toDomain);
    }

    private ApiKeyRecord toDomain(ApiKeyJpaEntity entity) {
        List<ApiScope> scopes;
        if (entity.getScopesCsv() == null || entity.getScopesCsv().isBlank()) {
            scopes = Collections.emptyList();
        } else {
            scopes = Arrays.stream(entity.getScopesCsv().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(ApiScope::valueOf)
                    .toList();
        }

        return new ApiKeyRecord(
                new ApiKeyId(entity.getId()),
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getKeyName(),
                entity.getHashedToken(),
                scopes,
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}

