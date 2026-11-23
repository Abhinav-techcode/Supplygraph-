package com.supplygraph.application.port.out;

import com.supplygraph.application.agent.AgentTrackRecord;

import java.time.Instant;
import java.util.List;

public interface AgentOutcomePort {
    void record(String workspaceId, String role, String incidentId, boolean success, String note, Instant at);
    List<AgentTrackRecord> trackRecords(String workspaceId);
}
