package com.supplygraph.application.sustainability.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntryId;

import java.util.List;
import java.util.Optional;

public interface CircularLedgerRepositoryPort {
    void save(CircularLedgerEntry entry);
    Optional<CircularLedgerEntry> findById(CircularLedgerEntryId id);
    List<CircularLedgerEntry> findByWorkspaceId(WorkspaceId workspaceId);
}

