package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.integration.ImportJob;
import com.supplygraph.domain.model.integration.ImportJobId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for enterprise data import jobs and audit logs.
 */
public interface ImportJobRepositoryPort {

    void save(ImportJob job);

    Optional<ImportJob> findById(ImportJobId id);

    List<ImportJob> findByWorkspaceId(String workspaceId);
}
