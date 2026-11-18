package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.ExecutionPolicy;

import java.util.Optional;

public interface ExecutionPolicyRepositoryPort {
    Optional<ExecutionPolicy> find(WorkspaceId ws);
    ExecutionPolicy save(ExecutionPolicy policy);
}
