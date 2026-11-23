package com.supplygraph.adapter.ai.workflow;

import com.supplygraph.adapter.ai.IncidentInvestigationService;
import com.supplygraph.application.agent.*;
import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.application.recovery.AgenticRecoveryOrchestrator;
import com.supplygraph.application.recovery.RecoveryProposal;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.builtin.*;
import com.supplygraph.concurguard.api.DefaultConcurGuardEngine;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** All eight agents + ConcurGuard + audit trail + learning, against one coherent twin. */
@DisplayName("Agentic recovery workflow (agents -> ConcurGuard -> audit -> learning)")
class EndToEndDisruptionRecoveryWorkflowTest {

    /** Minimal in-memory ports; the real ones are JDBC adapters (covered by persistence tests). */
    static final class MemInvestigations implements AgentInvestigationRepositoryPort {
        final List<AgentInvestigationRecord> rows = new ArrayList<>();
        public void save(AgentInvestigationRecord r) { rows.add(r); }
        public List<AgentInvestigationRecord> findByWorkspaceId(WorkspaceId ws) { return rows.stream().filter(r -> r.workspaceId().equals(ws.value())).toList(); }
        public List<AgentInvestigationRecord> findAll() { return rows; }
    }

    static final class MemOutcomes implements com.supplygraph.application.port.out.AgentOutcomePort {
        final List<Object[]> rows = new ArrayList<>();
        public void record(String ws, String role, String inc, boolean ok, String note, Instant at) { rows.add(new Object[]{ws, role, ok}); }
        public List<AgentTrackRecord> trackRecords(String ws) {
            Map<String, int[]> m = new LinkedHashMap<>();
            for (Object[] r : rows) { int[] c = m.computeIfAbsent((String) r[1], k -> new int[2]); c[0]++; if ((Boolean) r[2]) c[1]++; }
            return m.entrySet().stream().map(e -> new AgentTrackRecord(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
        }
    }

    private AgenticRecoveryOrchestrator orchestrator(DigitalTwinSnapshot s, MemInvestigations repo, AgentLearningService learning) {
        List<AgentTool> tools = List.of(new QueryInventoryTool(() -> s), new QueryBomTool(() -> s), new QueryLaneStatusTool(() -> s), new QueryCapacityTool(Map::of));
        return new AgenticRecoveryOrchestrator(new DefaultConcurGuardEngine(), IncidentInvestigationService.allAgents(), tools, repo, learning);
    }

    @Test
    @DisplayName("A supplier outage yields arbitrated, evidence-backed proposals and one audit record per agent")
    void supplierOutageEndToEnd() {
        DigitalTwinSnapshot s = com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.snapshot(5000, 2000);
        MemInvestigations repo = new MemInvestigations();
        RecoveryProposal proposal = orchestrator(s, repo, null).coordinateRecovery(com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.incident(Set.of("SUP-A"), 72), s);

        assertThat(proposal.approvedIntents()).isNotEmpty();
        assertThat(proposal.approvedIntents()).as(String.valueOf(proposal.arbitratedTradeOffs())).extracting(i -> i.actionType().name()).contains("EMERGENCY_BUFFER_DISPATCH", "EXPEDITE_PURCHASE_ORDER", "REROUTE_SHIPMENT");
        assertThat(proposal.auditTrail()).anyMatch(a -> a.toolName().equals("query_inventory") && a.success());
        assertThat(repo.rows).hasSize(8);
        assertThat(repo.rows).extracting(AgentInvestigationRecord::role).containsExactlyInAnyOrder("COORDINATOR", "RISK_INTELLIGENCE", "INVENTORY", "PROCUREMENT", "LOGISTICS", "PRODUCTION", "QUALITY", "RECOVERY_PLANNING");
        AgentInvestigationRecord inv = repo.rows.stream().filter(r -> r.role().equals("INVENTORY")).findFirst().orElseThrow();
        assertThat(inv.outcome()).isEqualTo("ARBITRATED_ALLOW");
        assertThat(inv.toolsInvoked()).isPositive();
        assertThat(inv.summary()).contains("HUB");
        AgentInvestigationRecord coord = repo.rows.stream().filter(r -> r.role().equals("COORDINATOR")).findFirst().orElseThrow();
        assertThat(coord.outcome()).isEqualTo("ADVISORY");
        assertThat(coord.intentsGenerated()).isZero();
        assertThat(proposal.totalEstimatedCost()).isNotNegative();
    }

    @Test
    @DisplayName("An incident that touches nothing in the twin produces no approved actions")
    void unknownIncidentProducesNothing() {
        DigitalTwinSnapshot s = com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.snapshot(5000, 2000);
        MemInvestigations repo = new MemInvestigations();
        RecoveryProposal proposal = orchestrator(s, repo, null).coordinateRecovery(com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.incident(Set.of("GHOST"), 72), s);
        assertThat(proposal.approvedIntents()).isEmpty();
        assertThat(repo.rows).allMatch(r -> r.outcome().equals("ADVISORY"));
    }

    @Test
    @DisplayName("Track record counts real decisions only: nothing is scored before people have decided")
    void trackRecordCountsDecisions() {
        DigitalTwinSnapshot s = com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.snapshot(5000, 2000);
        MemInvestigations repo = new MemInvestigations();
        MemOutcomes outcomes = new MemOutcomes();
        AgentLearningService learning = new AgentLearningService(outcomes, repo);
        WorkspaceId ws = WorkspaceId.of("WS-AGENT-TEST");
        DisruptionIncident incident = com.supplygraph.adapter.ai.agent.AgentTestTwinAccess.incident(Set.of("SUP-A"), 72);

        assertThat(learning.trackRecords(ws).get("INVENTORY").summary()).isEqualTo("No decisions yet");
        orchestrator(s, repo, learning).coordinateRecovery(incident, s);
        double completenessBefore = repo.rows.stream().filter(r -> r.role().equals("INVENTORY")).findFirst().orElseThrow().confidenceScore();

        assertThat(learning.recordIncidentOutcome(ws, incident.incidentId(), false, "Human rejected the plan")).isEqualTo(4);   // only agents that proposed an action
        assertThat(learning.trackRecords(ws).get("COORDINATOR").trials()).isZero();                                          // advisory agents are not counted
        learning.recordIncidentOutcome(ws, incident.incidentId(), true, "accepted");
        learning.recordIncidentOutcome(ws, incident.incidentId(), true, "accepted");
        assertThat(learning.trackRecords(ws).get("INVENTORY").summary()).isEqualTo("Accepted 2 of 3 decisions");

        repo.rows.clear();
        orchestrator(s, repo, learning).coordinateRecovery(incident, s);
        double completenessAfter = repo.rows.stream().filter(r -> r.role().equals("INVENTORY")).findFirst().orElseThrow().confidenceScore();
        assertThat(completenessAfter).isEqualTo(completenessBefore);   // decisions never inflate or deflate the measured completeness
    }
}
