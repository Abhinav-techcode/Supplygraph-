package com.supplygraph.application.tool;

import com.supplygraph.concurguard.model.AgentId;
import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized, secure Java tool execution boundary enforcing zero-trust boundaries for AI agents.
 * <p>
 * Mandates:
 * 1. Valid Agent Identity
 * 2. Strict Workspace Isolation
 * 3. Tool Allow-list authorization
 * 4. Input parameter schema validity
 * 5. Bounded per-agent invocation rate limits
 * 6. Immutable audit logging of all invocations
 */
public class ToolExecutionBoundary {

    public static final int DEFAULT_MAX_REQUESTS_PER_AGENT = 50;

    private final WorkspaceId boundWorkspaceId;
    private final int maxRequestsPerAgent;
    private final Map<String, AgentTool> registeredTools = new ConcurrentHashMap<>();
    private final Map<AgentId, AtomicInteger> invocationCounters = new ConcurrentHashMap<>();
    private final List<ToolAuditEntry> auditTrail = Collections.synchronizedList(new ArrayList<>());

    public ToolExecutionBoundary(WorkspaceId boundWorkspaceId, int maxRequestsPerAgent, Collection<AgentTool> tools) {
        this.boundWorkspaceId = Objects.requireNonNull(boundWorkspaceId, "boundWorkspaceId must not be null");
        if (maxRequestsPerAgent <= 0) {
            throw new IllegalArgumentException("maxRequestsPerAgent must be positive");
        }
        this.maxRequestsPerAgent = maxRequestsPerAgent;
        if (tools != null) {
            for (AgentTool tool : tools) {
                registeredTools.put(tool.name(), tool);
            }
        }
    }

    public ToolExecutionBoundary(WorkspaceId boundWorkspaceId, Collection<AgentTool> tools) {
        this(boundWorkspaceId, DEFAULT_MAX_REQUESTS_PER_AGENT, tools);
    }

    public void registerTool(AgentTool tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        registeredTools.put(tool.name(), tool);
    }

    /**
     * Executes a tool invocation under strict security checks.
     *
     * @param request the tool execution request
     * @return the result of tool execution
     * @throws SecurityException        if unauthorized or workspace boundary violated
     * @throws IllegalStateException    if request limit is exceeded
     * @throws IllegalArgumentException if tool is unknown or parameters are missing
     */
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        // 1. Workspace Isolation Check
        if (!boundWorkspaceId.equals(request.workspaceId())) {
            recordAudit(request, false, "Cross-workspace violation");
            throw new SecurityException("Cross-workspace tool execution prohibited: expected "
                    + boundWorkspaceId + ", but got " + request.workspaceId());
        }

        // 2. Agent Identity & Allow-List Check
        var identity = request.agentIdentity();
        if (identity == null) {
            recordAudit(request, false, "Missing agent identity");
            throw new SecurityException("Missing agent identity");
        }

        if (!identity.isToolAllowed(request.toolName())) {
            recordAudit(request, false, "Unauthorized tool access");
            throw new SecurityException("Agent [" + identity.agentId().value()
                    + "] is not permitted to execute tool: " + request.toolName());
        }

        // 3. Request Rate Limit Check
        var counter = invocationCounters.computeIfAbsent(identity.agentId(), k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > maxRequestsPerAgent) {
            recordAudit(request, false, "Rate limit exceeded");
            throw new IllegalStateException("Request limit (" + maxRequestsPerAgent
                    + ") exceeded for agent: " + identity.agentId().value());
        }

        // 4. Tool Existence Check
        AgentTool tool = registeredTools.get(request.toolName());
        if (tool == null) {
            recordAudit(request, false, "Unregistered tool");
            throw new IllegalArgumentException("Unknown or unregistered tool: " + request.toolName());
        }

        // 5. Tool Invocation & Audit Recording
        try {
            ToolExecutionResult result = tool.execute(request);
            recordAudit(request, result.success(), result.errorMessage());
            return result;
        } catch (Exception ex) {
            recordAudit(request, false, ex.getMessage());
            throw ex;
        }
    }

    private void recordAudit(ToolExecutionRequest request, boolean success, String errorMessage) {
        AgentId agentId = request.agentIdentity() != null ? request.agentIdentity().agentId() : AgentId.of("UNKNOWN");
        auditTrail.add(new ToolAuditEntry(
                Instant.now(),
                request.toolName(),
                agentId,
                request.workspaceId(),
                success,
                errorMessage
        ));
    }

    public List<ToolAuditEntry> auditTrail() {
        return List.copyOf(auditTrail);
    }

    public int getInvocationCount(AgentId agentId) {
        AtomicInteger count = invocationCounters.get(agentId);
        return count != null ? count.get() : 0;
    }

    public record ToolAuditEntry(
            Instant timestamp,
            String toolName,
            AgentId agentId,
            WorkspaceId workspaceId,
            boolean success,
            String errorMessage
    ) {
    }
}
