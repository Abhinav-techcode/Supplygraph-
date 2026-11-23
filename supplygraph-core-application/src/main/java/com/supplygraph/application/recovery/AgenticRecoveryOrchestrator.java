package com.supplygraph.application.recovery;

import com.supplygraph.application.agent.AgentInvestigationContext;
import com.supplygraph.application.agent.AgentProposal;
import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.application.agent.SpecializedAgent;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.api.ArbitrationStatus;
import com.supplygraph.concurguard.api.ConcurGuardEngine;
import com.supplygraph.concurguard.arbitration.ArbitrationDecision;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.AgentId;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.concurguard.replanning.ReplanningResult;
import com.supplygraph.concurguard.replanning.ReplanningScope;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * End-to-end recovery scenario execution orchestrator.
 * <p>
 * Coordinates bounded specialized agents, executes read-only tool inspection under
 * zero-trust boundaries, routes candidate proposals to the formal ConcurGuard engine,
 * coordinates selective replanning, and constructs an auditable {@link RecoveryProposal}
 * strictly in proposal mode (never auto-executing state mutation).
 */
public class AgenticRecoveryOrchestrator {

    private final ConcurGuardEngine concurGuardEngine;
    private final List<SpecializedAgent> specializedAgents;
    private final Collection<AgentTool> registeredTools;
    private final com.supplygraph.application.port.out.AgentInvestigationRepositoryPort investigationRepo;
    private final com.supplygraph.application.agent.AgentLearningService learning;

    public AgenticRecoveryOrchestrator(
            ConcurGuardEngine concurGuardEngine,
            List<SpecializedAgent> specializedAgents,
            Collection<AgentTool> registeredTools
    ) {
        this(concurGuardEngine, specializedAgents, registeredTools, null);
    }

    public AgenticRecoveryOrchestrator(
            ConcurGuardEngine concurGuardEngine,
            List<SpecializedAgent> specializedAgents,
            Collection<AgentTool> registeredTools,
            com.supplygraph.application.port.out.AgentInvestigationRepositoryPort investigationRepo
    ) {
        this(concurGuardEngine, specializedAgents, registeredTools, investigationRepo, null);
    }

    public AgenticRecoveryOrchestrator(
            ConcurGuardEngine concurGuardEngine,
            List<SpecializedAgent> specializedAgents,
            Collection<AgentTool> registeredTools,
            com.supplygraph.application.port.out.AgentInvestigationRepositoryPort investigationRepo,
            com.supplygraph.application.agent.AgentLearningService learning
    ) {
        this.learning = learning;
        this.concurGuardEngine = Objects.requireNonNull(concurGuardEngine, "concurGuardEngine must not be null");
        this.specializedAgents = specializedAgents != null ? List.copyOf(specializedAgents) : List.of();
        this.registeredTools = registeredTools != null ? List.copyOf(registeredTools) : List.of();
        this.investigationRepo = investigationRepo;
    }

    /**
     * Executes the multi-agent recovery workflow for a given disruption incident.
     *
     * @param incident disruption incident payload
     * @param snapshot authoritative digital twin snapshot
     * @return auditable, comprehensive recovery proposal
     */
    public RecoveryProposal coordinateRecovery(DisruptionIncident incident, DigitalTwinSnapshot snapshot) {
        Objects.requireNonNull(incident, "incident must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        // 1. Initialize context & zero-trust tool boundary
        AgentInvestigationContext context = new AgentInvestigationContext(incident, snapshot,
                Map.of());
        ToolExecutionBoundary toolBoundary = new ToolExecutionBoundary(incident.workspaceId(), registeredTools);

        // 2. Dispatch investigation across all specialized agents
        Map<IntentId, SpecializedAgent> intentOwners = new LinkedHashMap<>();
        Map<IntentId, ActionIntent> candidateIntents = new LinkedHashMap<>();
        List<AgentProposal> initialProposals = new ArrayList<>();

        for (SpecializedAgent agent : specializedAgents) {
            AgentProposal proposal = agent.investigate(context, toolBoundary);
            initialProposals.add(proposal);
            for (ActionIntent intent : proposal.intents()) {
                candidateIntents.put(intent.id(), intent);
                intentOwners.put(intent.id(), agent);
            }
        }

        // 3. Delegate to ConcurGuard Selective Replanning Engine
        ReplanningResult result = concurGuardEngine.getReplanningCoordinator().coordinate(
                candidateIntents.values(),
                snapshot,
                (affectedIds, iteration) -> {
                    List<ActionIntent> replacementIntents = new ArrayList<>();
                    // Group affected intents by responsible agent
                    Map<SpecializedAgent, Set<IntentId>> agentAffectedMap = new HashMap<>();
                    for (IntentId affId : affectedIds) {
                        SpecializedAgent owner = intentOwners.get(affId);
                        if (owner != null) {
                            agentAffectedMap.computeIfAbsent(owner, k -> new HashSet<>()).add(affId);
                        }
                    }

                    // Selectively invoke replanning on affected agents only
                    for (Map.Entry<SpecializedAgent, Set<IntentId>> entry : agentAffectedMap.entrySet()) {
                        SpecializedAgent agent = entry.getKey();
                        Set<IntentId> directIds = entry.getValue();
                        Set<IntentId> preserved = new HashSet<>(candidateIntents.keySet());
                        preserved.removeAll(directIds);
                        ReplanningScope scope = new ReplanningScope(
                                preserved,
                                directIds,
                                List.of(),
                                iteration
                        );
                        AgentProposal replannedProposal = agent.replan(context, scope, toolBoundary);
                        for (ActionIntent repIntent : replannedProposal.intents()) {
                            replacementIntents.add(repIntent);
                            intentOwners.put(repIntent.id(), agent);
                        }
                    }
                    return replacementIntents;
                }
        );

        // 4. Distinguish Approved, Rejected, and Preserved Intents
        List<ActionIntent> approvedIntents = new ArrayList<>();
        List<ActionIntent> rejectedIntents = new ArrayList<>();
        List<ActionIntent> preservedIntents = new ArrayList<>();
        List<String> arbitratedTradeOffs = new ArrayList<>();

        for (ActionIntent intent : result.finalPlan()) {
            ArbitrationDecision decision = result.finalDecisions().get(intent.id());
            if (decision != null && decision.outcome() == ArbitrationStatus.ALLOW) {
                approvedIntents.add(intent);
                if (result.preservedIntents().contains(intent.id())) {
                    preservedIntents.add(intent);
                }
            } else {
                rejectedIntents.add(intent);
            }
        }

        for (ArbitrationDecision dec : result.finalDecisions().values()) {
            if (dec.outcome() != ArbitrationStatus.ALLOW) {
                arbitratedTradeOffs.add("Arbitrated intent [" + dec.intentId().value()
                        + "] with outcome: " + dec.outcome() + " - " + dec.explanation());
            }
        }
        result.escalationReason().ifPresent(reason ->
                arbitratedTradeOffs.add("Escalation Advisory: " + reason));

        // Persist one auditable investigation record per agent, with the outcome ConcurGuard actually produced.
        if (investigationRepo != null) {
            Map<com.supplygraph.concurguard.model.AgentId, Integer> toolCalls = new HashMap<>();
            for (var e : toolBoundary.auditTrail()) toolCalls.merge(e.agentId(), 1, Integer::sum);
            Set<IntentId> allowedIds = new HashSet<>();
            approvedIntents.forEach(i -> allowedIds.add(i.id()));
            for (int i = 0; i < specializedAgents.size(); i++) {
                SpecializedAgent agent = specializedAgents.get(i);
                AgentProposal pr = initialProposals.get(i);
                List<ActionIntent> mine = result.finalPlan().stream().filter(it -> it.agentId().equals(agent.identity().agentId())).toList();
                String outcome = mine.isEmpty() ? (pr.intents().isEmpty() ? "ADVISORY" : "REPLACED_BY_REPLANNING")
                        : mine.stream().anyMatch(it -> allowedIds.contains(it.id())) ? "ARBITRATED_ALLOW" : "REJECTED_BY_CONCURGUARD";
                String target = !pr.intents().isEmpty() ? pr.intents().get(0).targetEntityId() : incident.affectedNodeIds().stream().findFirst().orElse("GLOBAL");
                investigationRepo.save(new com.supplygraph.application.agent.AgentInvestigationRecord(
                        "INV-" + UUID.randomUUID().toString().substring(0, 8),
                        incident.workspaceId().value(),
                        incident.incidentId(),
                        agent.identity().role().displayName(),
                        agent.identity().role().name(),
                        target,
                        toolCalls.getOrDefault(agent.identity().agentId(), 0),
                        pr.intents().size(),
                        pr.confidence(),
                        outcome,
                        Instant.now(),
                        String.join(" | ", pr.evidence())));
            }
        }

        // 5. Calculate Metrics
        BigDecimal totalEstimatedCost = approvedIntents.stream().map(ActionIntent::estimatedCost).reduce(BigDecimal.ZERO, BigDecimal::add); // agents do not price actions; zero means "not estimated"
        long recoveryTimeObjectiveHours = Math.max(incident.estimatedDowntimeHours(), 24L);

        double totalConfidence = initialProposals.stream()
                .mapToDouble(AgentProposal::confidence)
                .average()
                .orElse(0.0);

        // Penalize slightly if replanning iterations were needed
        double overallConfidence = Math.max(0.1, Math.min(1.0, totalConfidence - (result.iterationsCompleted() * 0.05)));

        String riskAssessment = "Disruption severity [" + incident.severity() + "]. "
                + approvedIntents.size() + " recovery actions approved. "
                + preservedIntents.size() + " independent actions preserved without replanning. "
                + (result.success() ? "Arbitration converged deterministically." : "Arbitration escalated to human reviewer.");

        String proposalId = "REC-PROP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new RecoveryProposal(
                proposalId,
                incident.incidentId(),
                incident.workspaceId(),
                approvedIntents,
                rejectedIntents,
                preservedIntents,
                arbitratedTradeOffs,
                totalEstimatedCost,
                recoveryTimeObjectiveHours,
                overallConfidence,
                riskAssessment,
                toolBoundary.auditTrail(),
                Instant.now()
        );
    }
}
