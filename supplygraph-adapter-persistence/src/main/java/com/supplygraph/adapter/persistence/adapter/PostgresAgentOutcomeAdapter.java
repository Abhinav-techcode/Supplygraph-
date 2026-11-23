package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.application.agent.AgentTrackRecord;
import com.supplygraph.application.port.out.AgentOutcomePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Durable outcomes (human acceptance / execution verification) per agent role, from which agent confidence is calibrated. */
@Component
public class PostgresAgentOutcomeAdapter implements AgentOutcomePort {
    private final JdbcTemplate jdbc;

    public PostgresAgentOutcomeAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void record(String workspaceId, String role, String incidentId, boolean success, String note, Instant at) {
        jdbc.update("INSERT INTO agent_outcomes (outcome_id, workspace_id, role, incident_id, success, note, recorded_at) VALUES (?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), workspaceId, role, incidentId, success, note == null ? null : note.substring(0, Math.min(500, note.length())), Timestamp.from(at));
    }

    @Override
    public List<AgentTrackRecord> trackRecords(String workspaceId) {
        return jdbc.query("SELECT role, COUNT(*) AS n, SUM(CASE WHEN success THEN 1 ELSE 0 END) AS ok FROM agent_outcomes WHERE workspace_id = ? GROUP BY role",
                (rs, i) -> new AgentTrackRecord(rs.getString("role"), rs.getInt("n"), rs.getInt("ok")), workspaceId);
    }
}
