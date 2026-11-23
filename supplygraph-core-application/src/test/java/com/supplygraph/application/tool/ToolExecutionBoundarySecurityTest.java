package com.supplygraph.application.tool;

import com.supplygraph.domain.error.SecurityViolationException;
import com.supplygraph.application.agent.AgentIdentity;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.concurguard.model.AgentId;
import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolExecutionBoundarySecurityTest {

    private WorkspaceId workspace1;
    private WorkspaceId workspace2;
    private AgentTool dummyInventoryTool;
    private ToolExecutionBoundary boundary;

    @BeforeEach
    void setUp() {
        workspace1 = WorkspaceId.of("WS-PROD-01");
        workspace2 = WorkspaceId.of("WS-ROGUE-02");

        dummyInventoryTool = new AgentTool() {
            @Override
            public String name() {
                return "query_inventory";
            }

            @Override
            public String description() {
                return "Dummy inventory lookup";
            }

            @Override
            public ToolExecutionResult execute(ToolExecutionRequest request) {
                return ToolExecutionResult.success(Map.of("stock", 100));
            }
        };

        boundary = new ToolExecutionBoundary(workspace1, 3, List.of(dummyInventoryTool));
    }

    @Test
    @DisplayName("Security: Cross-workspace tool execution must be rejected")
    void shouldRejectCrossWorkspaceExecution() {
        AgentIdentity identity = AgentIdentity.of(AgentRole.INVENTORY, Set.of("query_inventory"));
        ToolExecutionRequest crossWsRequest = new ToolExecutionRequest(
                "query_inventory",
                identity,
                workspace2, // Mismatched workspace!
                Map.of("materialId", "MAT-101")
        );

        assertThatThrownBy(() -> boundary.execute(crossWsRequest))
                .isInstanceOf(SecurityViolationException.class)
                .hasMessageContaining("Cross-workspace tool execution prohibited");

        // Verify violation is recorded in audit trail
        assertThat(boundary.auditTrail()).hasSize(1);
        assertThat(boundary.auditTrail().get(0).success()).isFalse();
        assertThat(boundary.auditTrail().get(0).errorMessage()).contains("Cross-workspace violation");
    }

    @Test
    @DisplayName("Security: Unauthorized tool invocation must be rejected")
    void shouldRejectUnauthorizedToolForAgent() {
        // Logistics agent does NOT have permission to query_inventory
        AgentIdentity logisticsIdentity = AgentIdentity.of(AgentRole.LOGISTICS, Set.of("query_lane_status"));
        ToolExecutionRequest request = new ToolExecutionRequest(
                "query_inventory",
                logisticsIdentity,
                workspace1,
                Map.of("materialId", "MAT-101")
        );

        assertThatThrownBy(() -> boundary.execute(request))
                .isInstanceOf(SecurityViolationException.class)
                .hasMessageContaining("is not permitted to execute tool: query_inventory");

        assertThat(boundary.auditTrail()).hasSize(1);
        assertThat(boundary.auditTrail().get(0).success()).isFalse();
    }

    @Test
    @DisplayName("Security: Rate limit violations must be rejected")
    void shouldRejectWhenAgentExceedsRequestLimit() {
        AgentIdentity identity = AgentIdentity.of(AgentRole.INVENTORY, Set.of("query_inventory"));
        ToolExecutionRequest request = new ToolExecutionRequest(
                "query_inventory",
                identity,
                workspace1,
                Map.of("materialId", "MAT-101")
        );

        // Max requests configured to 3 in setUp()
        boundary.execute(request);
        boundary.execute(request);
        boundary.execute(request);

        assertThat(boundary.getInvocationCount(identity.agentId())).isEqualTo(3);

        // 4th request must fail rate limit
        assertThatThrownBy(() -> boundary.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Request limit (3) exceeded");

        assertThat(boundary.auditTrail()).hasSize(4);
        assertThat(boundary.auditTrail().get(3).errorMessage()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Security: Authorized execution executes tool and records successful audit")
    void shouldExecuteAuthorizedToolSuccessfully() {
        AgentIdentity identity = AgentIdentity.of(AgentRole.INVENTORY, Set.of("query_inventory"));
        ToolExecutionRequest request = new ToolExecutionRequest(
                "query_inventory",
                identity,
                workspace1,
                Map.of("materialId", "MAT-101")
        );

        ToolExecutionResult result = boundary.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("stock", 100);
        assertThat(boundary.auditTrail()).hasSize(1);
        assertThat(boundary.auditTrail().get(0).success()).isTrue();
        assertThat(boundary.auditTrail().get(0).toolName()).isEqualTo("query_inventory");
        assertThat(boundary.auditTrail().get(0).agentId()).isEqualTo(identity.agentId());
    }
}
