package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.provenance.ProvenanceId;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for recording and querying data provenance records.
 */
public interface ProvenanceRepositoryPort {

    ProvenanceRecord save(ProvenanceRecord record);

    Optional<ProvenanceRecord> findById(ProvenanceId id);

    List<ProvenanceRecord> findByWorkspaceId(WorkspaceId workspaceId);

    List<ProvenanceRecord> findByEntityId(String entityId);

    List<ProvenanceRecord> findAll();
}

