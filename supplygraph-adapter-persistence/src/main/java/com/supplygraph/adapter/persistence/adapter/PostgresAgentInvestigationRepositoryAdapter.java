package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/** Durable store of agent investigations: survives restarts and feeds the Agent History page and the learning loop. */
@Component
public class PostgresAgentInvestigationRepositoryAdapter implements AgentInvestigationRepositoryPort {
    private final JdbcTemplate jdbc;

    public PostgresAgentInvestigationRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(AgentInvestigationRecord r) {
        Objects.requireNonNull(r, "record must not be null");
        jdbc.update("INSERT INTO agent_investigations (investigation_id, workspace_id, disruption_id, agent_name, role, target_entity, tools_invoked, intents_generated, confidence, outcome, summary, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                r.investigationId(), r.workspaceId(), r.disruptionId(), r.agentName(), r.role(), r.targetEntity(), r.toolsInvoked(), r.intentsGenerated(),
                r.confidenceScore(), r.outcome(), r.summary(), Timestamp.from(r.timestamp()));
    }

    @Override
    public List<AgentInvestigationRecord> findByWorkspaceId(WorkspaceId ws) {
        if (ws == null) return findAll();
        return jdbc.query("SELECT * FROM agent_investigations WHERE workspace_id = ? ORDER BY created_at DESC", (rs, i) -> map(rs), ws.value());
    }

    @Override
    public List<AgentInvestigationRecord> findAll() {
        return jdbc.query("SELECT * FROM agent_investigations ORDER BY created_at DESC", (rs, i) -> map(rs));
    }

    private static AgentInvestigationRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AgentInvestigationRecord(rs.getString("investigation_id"), rs.getString("workspace_id"), rs.getString("disruption_id"), rs.getString("agent_name"),
                rs.getString("role"), rs.getString("target_entity"), rs.getInt("tools_invoked"), rs.getInt("intents_generated"), rs.getDouble("confidence"),
                rs.getString("outcome"), rs.getTimestamp("created_at").toInstant(), rs.getString("summary") == null ? "" : rs.getString("summary"));
    }
}
