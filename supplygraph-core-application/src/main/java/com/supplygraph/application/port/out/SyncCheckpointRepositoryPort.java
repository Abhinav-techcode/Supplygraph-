package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.sync.SyncCheckpoint;

import java.util.Optional;

public interface SyncCheckpointRepositoryPort {
    Optional<SyncCheckpoint> find(ConnectorId id);
    SyncCheckpoint save(SyncCheckpoint checkpoint);
}
