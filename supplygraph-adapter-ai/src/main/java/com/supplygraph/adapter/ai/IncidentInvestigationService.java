package com.supplygraph.adapter.ai;

import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.adapter.ai.agent.*;
import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.application.agent.AgentLearningService;
import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.application.agent.SpecializedAgent;
import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.recovery.AgenticRecoveryOrchestrator;
import com.supplygraph.application.recovery.RecoveryProposal;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.builtin.*;
import com.supplygraph.concurguard.api.DefaultConcurGuardEngine;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Runs the eight specialist agents against the current digital twin for one disruption. Agents read the twin only through the bounded tool
 * boundary and their own snapshot; their proposals are arbitrated by ConcurGuard and recorded (with evidence) for audit and learning.
 * Agents never execute anything: the result is a proposal for a human.
 */
@Service
public class IncidentInvestigationService {
    public record Result(RecoveryProposal proposal, List<AgentInvestigationRecord> records) {}

    private final DigitalTwinRepositoryPort twinRepository;
    private final AgentInvestigationRepositoryPort investigations;
    private final AgentLearningService learning;

    public IncidentInvestigationService(DigitalTwinRepositoryPort twinRepository, AgentInvestigationRepositoryPort investigations, AgentLearningService learning) {
        this.twinRepository = twinRepository;
        this.investigations = investigations;
        this.learning = learning;
    }

    public static List<SpecializedAgent> allAgents() {
        return List.of(new CoordinatorAgent(), new RiskIntelligenceAgent(), new InventoryAgent(), new ProcurementAgent(), new LogisticsAgent(),
                new ProductionAgent(), new QualityAgent(), new RecoveryPlanningAgent());
    }

    public Result investigate(WorkspaceId ws, Disruption d) {
        DigitalTwinSnapshot snapshot = twinRepository.findByWorkspaceId(ws)
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found for workspace " + ws.value())).createSnapshot();
        long hours = d.expectedEndTime() != null ? Math.max(1, Duration.between(d.startTime(), d.expectedEndTime()).toHours()) : 72;
        DisruptionIncident incident = new DisruptionIncident(d.id().value(), ws, d.title(), d.description() == null ? "" : d.description(), d.industryCode(),
                d.affectedNodeIds().stream().map(NodeId::value).collect(Collectors.toSet()), d.affectedMaterialIds().stream().map(MaterialId::value).collect(Collectors.toSet()),
                hours, d.severity().name(), Instant.now());
        List<AgentTool> tools = List.of(new QueryInventoryTool(() -> snapshot), new QueryBomTool(() -> snapshot), new QueryLaneStatusTool(() -> snapshot), new QueryCapacityTool(java.util.Map::of));
        AgenticRecoveryOrchestrator orchestrator = new AgenticRecoveryOrchestrator(new DefaultConcurGuardEngine(), allAgents(), tools, investigations, learning);
        RecoveryProposal proposal = orchestrator.coordinateRecovery(incident, snapshot);
        List<AgentInvestigationRecord> records = investigations.findByWorkspaceId(ws).stream().filter(r -> r.disruptionId().equals(d.id().value())).toList();
        return new Result(proposal, records);
    }
}
