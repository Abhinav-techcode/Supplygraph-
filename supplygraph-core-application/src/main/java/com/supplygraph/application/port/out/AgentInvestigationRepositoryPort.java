package com.supplygraph.application.port.out;

import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.domain.model.WorkspaceId;

import java.util.List;

public interface AgentInvestigationRepositoryPort {
    void save(AgentInvestigationRecord record);
    List<AgentInvestigationRecord> findByWorkspaceId(WorkspaceId workspaceId);
    List<AgentInvestigationRecord> findAll();
}

