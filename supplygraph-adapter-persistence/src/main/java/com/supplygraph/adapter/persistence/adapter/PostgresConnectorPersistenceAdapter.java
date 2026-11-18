package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.ConnectorJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataConnectorRepository;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class PostgresConnectorPersistenceAdapter implements ConnectorRepositoryPort {
    private final SpringDataConnectorRepository repository;

    public PostgresConnectorPersistenceAdapter(SpringDataConnectorRepository repository) { this.repository = repository; }

    @Override @Transactional
    public Connector save(Connector c) {
        ConnectorJpaEntity e = repository.findById(c.id().value()).orElseGet(ConnectorJpaEntity::new);
        e.setId(c.id().value());
        e.setWorkspaceId(c.workspaceId().value());
        e.setName(c.name());
        e.setCategory(c.category().name());
        e.setMode(c.mode().name());
        e.setAdapterType(c.adapterType());
        e.setStatus(c.status().name());
        ConnectorConfiguration cfg = c.configuration();
        e.setBaseUri(cfg != null && cfg.baseUri() != null ? cfg.baseUri().toString() : null);
        e.setCredentialRef(cfg != null && cfg.credential() != null ? cfg.credential().value() : null);
        e.setAllowedHosts(cfg != null ? String.join(",", cfg.allowedHosts()) : "");
        e.setTimeoutMillis(cfg != null ? cfg.timeoutMillis() : null);
        e.setMaxResponseBytes(cfg != null ? cfg.maxResponseBytes() : null);
        e.setMaxRetries(cfg != null ? cfg.maxRetries() : null);
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        e.setVersion(c.version());
        return toDomain(repository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public Optional<Connector> findById(ConnectorId id) { return repository.findById(id.value()).map(this::toDomain); }

    @Override @Transactional(readOnly = true)
    public List<Connector> findByWorkspace(WorkspaceId ws) {
        return repository.findByWorkspaceIdOrderByCreatedAtDesc(ws.value()).stream().map(this::toDomain).toList();
    }

    private Connector toDomain(ConnectorJpaEntity e) {
        ConnectorConfiguration cfg = null;
        if (e.getTimeoutMillis() != null) {
            List<String> hosts = e.getAllowedHosts().isBlank() ? List.of() : Arrays.asList(e.getAllowedHosts().split(","));
            cfg = new ConnectorConfiguration(
                    e.getBaseUri() != null ? URI.create(e.getBaseUri()) : null,
                    e.getCredentialRef() != null ? new CredentialReference(e.getCredentialRef()) : null,
                    hosts, e.getTimeoutMillis(), e.getMaxResponseBytes(), e.getMaxRetries());
        }
        return new Connector(ConnectorId.of(e.getId()), WorkspaceId.of(e.getWorkspaceId()), e.getName(),
                ConnectorCategory.valueOf(e.getCategory()), ConnectorMode.valueOf(e.getMode()), e.getAdapterType(),
                ConnectorStatus.valueOf(e.getStatus()), cfg, e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
