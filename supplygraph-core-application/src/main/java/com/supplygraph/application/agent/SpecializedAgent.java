package com.supplygraph.application.agent;

import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.replanning.ReplanningScope;

/**
 * Standard contract for each of the 8 bounded specialized agents.
 * <p>
 * Agents are strictly proposal-only: they never mutate domain entities, execute financial transactions,
 * or bypass ConcurGuard validation.
 */
public interface SpecializedAgent {

    AgentRole role();

    AgentIdentity identity();

    /**
     * Conducts initial investigation into a disruption incident and proposes candidate actions.
     *
     * @param context      immutable investigation context
     * @param toolBoundary secure zero-trust tool execution boundary
     * @return structured, verifiable agent proposal
     */
    AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary);

    /**
     * Conducts bounded replanning when ConcurGuard identifies conflicts affecting this agent's intents.
     *
     * @param context      immutable investigation context
     * @param scope        selective replanning scope identifying affected intents
     * @param toolBoundary secure zero-trust tool execution boundary
     * @return revised agent proposal
     */
    AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary);
}
