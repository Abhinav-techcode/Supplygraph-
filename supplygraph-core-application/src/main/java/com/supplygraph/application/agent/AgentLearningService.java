package com.supplygraph.application.agent;

import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.application.port.out.AgentOutcomePort;
import com.supplygraph.domain.model.WorkspaceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Closes the learning loop: when a human approves or rejects a plan, or an execution is verified or fails, the agents that contributed
 * proposals to that incident get an outcome. Their next proposals carry a confidence calibrated from that history.
 */
@Service
public class AgentLearningService {
    private final AgentOutcomePort outcomes;
    private final AgentInvestigationRepositoryPort investigations;

    @Autowired
    public AgentLearningService(AgentOutcomePort outcomes, AgentInvestigationRepositoryPort investigations) {
        this.outcomes = outcomes;
        this.investigations = investigations;
    }

    public Map<String, AgentTrackRecord> trackRecords(WorkspaceId ws) {
        Map<String, AgentTrackRecord> m = new LinkedHashMap<>();
        for (AgentRole r : AgentRole.values()) m.put(r.name(), AgentTrackRecord.empty(r.name()));
        outcomes.trackRecords(ws.value()).forEach(t -> m.put(t.role(), t));
        return m;
    }

    /** Records the outcome for every agent that proposed at least one action for the incident. Returns how many agents were credited. */
    public int recordIncidentOutcome(WorkspaceId ws, String incidentId, boolean success, String note) {
        Set<String> roles = new LinkedHashSet<>();
        for (AgentInvestigationRecord r : investigations.findByWorkspaceId(ws))
            if (incidentId.equals(r.disruptionId()) && r.intentsGenerated() > 0) roles.add(r.role());
        Instant now = Instant.now();
        roles.forEach(role -> outcomes.record(ws.value(), role, incidentId, success, note, now));
        return roles.size();
    }
}
