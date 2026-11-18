package com.supplygraph.domain.model.connector;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.Objects;

/** Connector aggregate: registration, configuration, activation and deactivation. Immutable; transitions return new instances. */
public record Connector(
        ConnectorId id,
        WorkspaceId workspaceId,
        String name,
        ConnectorCategory category,
        ConnectorMode mode,
        String adapterType,
        ConnectorStatus status,
        ConnectorConfiguration configuration,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public Connector {
        Objects.requireNonNull(id); Objects.requireNonNull(workspaceId); Objects.requireNonNull(category);
        Objects.requireNonNull(mode); Objects.requireNonNull(status); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        if (name == null || name.isBlank() || name.length() > 120) throw new IllegalArgumentException("name must be 1..120 characters");
        if (adapterType == null || adapterType.isBlank()) throw new IllegalArgumentException("adapterType is required");
    }

    public static Connector register(WorkspaceId ws, String name, ConnectorCategory category, ConnectorMode mode, String adapterType) {
        Instant now = Instant.now();
        return new Connector(ConnectorId.generate(), ws, name, category, mode, adapterType, ConnectorStatus.REGISTERED, null, now, now, 0L);
    }

    public Connector configure(ConnectorConfiguration cfg) {
        Objects.requireNonNull(cfg, "configuration");
        if (mode == ConnectorMode.LIVE && (cfg.baseUri() == null || cfg.credential() == null)) {
            throw new IllegalArgumentException("LIVE connectors require a base URI and a credential reference");
        }
        return transition(ConnectorStatus.CONFIGURED, cfg);
    }

    public Connector activate() {
        if (configuration == null) throw new IllegalStateException("Connector must be configured before activation");
        return transition(ConnectorStatus.ACTIVE, configuration);
    }

    public Connector deactivate() { return transition(ConnectorStatus.INACTIVE, configuration); }

    public boolean isSimulated() { return mode == ConnectorMode.SANDBOX_SIMULATED; }

    private Connector transition(ConnectorStatus next, ConnectorConfiguration cfg) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Invalid connector transition " + status + " -> " + next);
        }
        return new Connector(id, workspaceId, name, category, mode, adapterType, next, cfg, createdAt, Instant.now(), version + 1);
    }
}
