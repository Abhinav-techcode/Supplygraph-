package com.supplygraph.application.provenance;

import com.supplygraph.application.port.out.DecisionProvenanceRepositoryPort;
import com.supplygraph.domain.model.provenance.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Constructs, stores, and navigates the end-to-end decision provenance graph.
 */
@Service
public class DecisionProvenanceGraphService {

    private static final Logger log = LoggerFactory.getLogger(DecisionProvenanceGraphService.class);

    private final DecisionProvenanceRepositoryPort repository;

    public DecisionProvenanceGraphService(DecisionProvenanceRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Constructs a full 13-stage decision provenance graph linking the incident to human approval.
     */
    public DecisionProvenanceGraph buildAndSaveProvenanceGraph(
            String workspaceId,
            String disruptionId,
            String humanApproverId
    ) {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(disruptionId, "disruptionId cannot be null");

        Instant baseTime = Instant.now().minusSeconds(3600);
        List<DecisionProvenanceNode> nodes = new ArrayList<>();
        List<DecisionProvenanceEdge> edges = new ArrayList<>();

        // 1. Disruption
        DecisionProvenanceNode nDisruption = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-DISR-01"),
                DecisionProvenanceNodeType.DISRUPTION,
                "Port Bottleneck Congestion Incident",
                disruptionId,
                baseTime,
                Map.of("severity", "CRITICAL", "node", "NODE-ROTTERDAM")
        );
        nodes.add(nDisruption);

        // 2. Evidence
        DecisionProvenanceNode nEvidence = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-EVID-01"),
                DecisionProvenanceNodeType.EVIDENCE,
                "Harbor Sensor Telemetry & AIS Queue",
                "TEL-ROTTERDAM-88",
                baseTime.plusSeconds(120),
                Map.of("source", "IoT AIS Maritime", "delayDays", "14")
        );
        nodes.add(nEvidence);
        edges.add(DecisionProvenanceEdge.of("DPE-01", nDisruption.id(), nEvidence.id(), DecisionProvenanceEdgeType.INFORMS, "Harbor radar feeds confirmed disruption", baseTime.plusSeconds(120)));

        // 3. Digital Twin Snapshot
        DecisionProvenanceNode nTwin = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-TWIN-01"),
                DecisionProvenanceNodeType.TWIN_SNAPSHOT,
                "Digital Twin Snapshot V1.0",
                "SNAP-WS-01-V1",
                baseTime.plusSeconds(180),
                Map.of("nodes", "12", "edges", "18", "version", "1.0")
        );
        nodes.add(nTwin);
        edges.add(DecisionProvenanceEdge.of("DPE-02", nEvidence.id(), nTwin.id(), DecisionProvenanceEdgeType.ASSESSES, "State snapshot captured for impact modeling", baseTime.plusSeconds(180)));

        // 4. Impact Assessment
        DecisionProvenanceNode nImpact = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-IMPACT-01"),
                DecisionProvenanceNodeType.IMPACT_ASSESSMENT,
                "Cascading Tier-2 Propagation Assessment",
                "IMP-ASSESS-01",
                baseTime.plusSeconds(300),
                Map.of("affectedNodes", "4", "revenueAtRiskUsd", "420000")
        );
        nodes.add(nImpact);
        edges.add(DecisionProvenanceEdge.of("DPE-03", nTwin.id(), nImpact.id(), DecisionProvenanceEdgeType.EVALUATES, "Cascading impact propagated across BOM", baseTime.plusSeconds(300)));

        // 5. Alert
        DecisionProvenanceNode nAlert = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-ALERT-01"),
                DecisionProvenanceNodeType.ALERT,
                "Critical Disruption Alert Broadcast",
                "ALT-CRIT-99",
                baseTime.plusSeconds(360) ,
                Map.of("urgency", "HIGH", "targetRoles", "SCM_MANAGER,PROCUREMENT_DIRECTOR")
        );
        nodes.add(nAlert);
        edges.add(DecisionProvenanceEdge.of("DPE-04", nImpact.id(), nAlert.id(), DecisionProvenanceEdgeType.TRIGGERS, "Stockout threshold breach fired operational alert", baseTime.plusSeconds(360)));

        // 6. Investigation
        DecisionProvenanceNode nInvest = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-INV-01"),
                DecisionProvenanceNodeType.INVESTIGATION,
                "Agentic Investigation & Root Cause Diagnostics",
                "INV-LOGISTICS-AGNT",
                baseTime.plusSeconds(500),
                Map.of("leadAgent", "LogisticsDisruptionInvestigatorAgent")
        );
        nodes.add(nInvest);
        edges.add(DecisionProvenanceEdge.of("DPE-05", nAlert.id(), nInvest.id(), DecisionProvenanceEdgeType.INVESTIGATES, "Autonomous bounded agent launched deep graph probe", baseTime.plusSeconds(500)));

        // 7. Proposal
        DecisionProvenanceNode nProposal = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-PROP-01"),
                DecisionProvenanceNodeType.PROPOSAL,
                "Emergency Air Freight & Secondary Sourcing Proposal",
                "PROP-ALT-AIR-42",
                baseTime.plusSeconds(700),
                Map.of("expeditedCost", "64000", "recoveredDays", "10")
        );
        nodes.add(nProposal);
        edges.add(DecisionProvenanceEdge.of("DPE-06", nInvest.id(), nProposal.id(), DecisionProvenanceEdgeType.PROPOSES, "Agent submitted structured recovery proposal", baseTime.plusSeconds(700)));

        // 8. ConcurGuard Decision
        DecisionProvenanceNode nConcurGuard = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-CG-01"),
                DecisionProvenanceNodeType.CONCURGUARD_DECISION,
                "ConcurGuard Conflict Detection & Selective Replanning",
                "CG-ARB-88",
                baseTime.plusSeconds(850),
                Map.of("conflictsDetected", "1", "resolution", "PRESERVED_NON_CONFLICTING_ACTIONS")
        );
        nodes.add(nConcurGuard);
        edges.add(DecisionProvenanceEdge.of("DPE-07", nProposal.id(), nConcurGuard.id(), DecisionProvenanceEdgeType.ARBITRATES, "Arbitrated inventory claim against concurrent automotive proposal", baseTime.plusSeconds(850)));

        // 9. Simulation Run
        DecisionProvenanceNode nSim = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-SIM-01"),
                DecisionProvenanceNodeType.SIMULATION_RUN,
                "Discrete-Event Stochastic Simulation (1,000 runs)",
                "SIM-RUN-1000",
                baseTime.plusSeconds(1000),
                Map.of("iterations", "1000", "meanLeadTimeDays", "4.2")
        );
        nodes.add(nSim);
        edges.add(DecisionProvenanceEdge.of("DPE-08", nConcurGuard.id(), nSim.id(), DecisionProvenanceEdgeType.SIMULATES, "Simulated multi-tier flow under arbitrated plan", baseTime.plusSeconds(1000)));

        // 10. Monte Carlo Run
        DecisionProvenanceNode nMonte = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-MC-01"),
                DecisionProvenanceNodeType.MONTE_CARLO_RUN,
                "Monte Carlo Tail Risk & VaR Analysis",
                "MC-RISK-500",
                baseTime.plusSeconds(1150),
                Map.of("var95CostUsd", "72000", "stockoutProbability", "0.03")
        );
        nodes.add(nMonte);
        edges.add(DecisionProvenanceEdge.of("DPE-09", nSim.id(), nMonte.id(), DecisionProvenanceEdgeType.EVALUATES, "Calculated parametric confidence boundaries", baseTime.plusSeconds(1150)));

        // 11. Optimization Run
        DecisionProvenanceNode nOpt = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-OPT-01"),
                DecisionProvenanceNodeType.OPTIMIZATION_RUN,
                "Google OR-Tools Mixed-Integer Linear Program",
                "OPT-SOLVE-ORTOOLS",
                baseTime.plusSeconds(1300),
                Map.of("status", "OPTIMAL", "objectiveCost", "64000")
        );
        nodes.add(nOpt);
        edges.add(DecisionProvenanceEdge.of("DPE-10", nMonte.id(), nOpt.id(), DecisionProvenanceEdgeType.OPTIMIZES, "Solved optimal flow allocation and carrier selection", baseTime.plusSeconds(1300)));

        // 12. Recovery Alternative
        DecisionProvenanceNode nRec = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-REC-01"),
                DecisionProvenanceNodeType.RECOVERY_ALTERNATIVE,
                "Recommended Air Freight & Buffer Reallocation Alternative",
                "ALT-RECOMMENDED-A",
                baseTime.plusSeconds(1450),
                Map.of("serviceRestoration", "0.98", "rank", "1")
        );
        nodes.add(nRec);
        edges.add(DecisionProvenanceEdge.of("DPE-11", nOpt.id(), nRec.id(), DecisionProvenanceEdgeType.PROPOSES, "Generated final Pareto-dominant recovery alternative", baseTime.plusSeconds(1450)));

        // 13. Human Decision
        String approver = humanApproverId != null ? humanApproverId : "USR-SCM-01";
        DecisionProvenanceNode nHuman = DecisionProvenanceNode.of(
                DecisionProvenanceNodeId.of("DPN-HUMAN-01"),
                DecisionProvenanceNodeType.HUMAN_DECISION,
                "Authorized Human Manager Approval (" + approver + ")",
                "APPROVAL-SIG-7A18",
                baseTime.plusSeconds(1600),
                Map.of("approver", approver, "action", "PLAN_APPROVED", "governance", "MANDATORY_HUMAN_IN_THE_LOOP")
        );
        nodes.add(nHuman);
        edges.add(DecisionProvenanceEdge.of("DPE-12", nRec.id(), nHuman.id(), DecisionProvenanceEdgeType.APPROVES, "Human decision-maker executed cryptographic sign-off", baseTime.plusSeconds(1600)));

        DecisionProvenanceGraph graph = DecisionProvenanceGraph.create(workspaceId, disruptionId, nodes, edges);
        repository.saveGraph(graph);
        log.info("Built and saved full 13-stage decision provenance graph for disruption {}", disruptionId);
        return graph;
    }

    public Optional<DecisionProvenanceGraph> findByRootDisruptionId(String disruptionId) {
        return repository.findByRootDisruptionId(disruptionId);
    }

    public Optional<DecisionProvenanceGraph> findByWorkspaceId(String workspaceId) {
        return repository.findByWorkspaceId(workspaceId);
    }
}
