package com.supplygraph.application.agent;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.AgentId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Structured, verifiable proposal produced by a bounded agent.
 * Contains candidate {@link ActionIntent}s accompanied by transparent evidence,
 * explicit assumptions, quantified confidence, and known operational limitations.
 */
public record AgentProposal(
        String proposalId,
        AgentRole agentRole,
        AgentId agentId,
        List<ActionIntent> intents,
        List<String> evidence,
        List<String> assumptions,
        double confidence,
        List<String> limitations,
        Instant proposedAt
) {
    public AgentProposal {
        Objects.requireNonNull(proposalId, "proposalId must not be null");
        Objects.requireNonNull(agentRole, "agentRole must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        intents = intents != null ? List.copyOf(intents) : List.of();
        evidence = evidence != null ? List.copyOf(evidence) : List.of();
        assumptions = assumptions != null ? List.copyOf(assumptions) : List.of();
        limitations = limitations != null ? List.copyOf(limitations) : List.of();
        Objects.requireNonNull(proposedAt, "proposedAt must not be null");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new ValidationException("Confidence must be within [0.0, 1.0], was: " + confidence);
        }
    }

    public static AgentProposal of(
            String proposalId,
            AgentRole agentRole,
            ActionIntent intent,
            List<String> evidence,
            List<String> assumptions,
            double confidence,
            List<String> limitations
    ) {
        return new AgentProposal(
                proposalId,
                agentRole,
                agentRole.agentId(),
                List.of(intent),
                evidence,
                assumptions,
                confidence,
                limitations,
                Instant.now()
        );
    }
}
