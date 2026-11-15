package com.supplygraph.concurguard.arbitration;

import com.supplygraph.concurguard.api.ArbitrationStatus;
import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.conflict.Severity;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.concurguard.model.AgentId;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArbitrationPrecedenceTest {

    private final ConcurGuardArbitrator arbitrator = new ConcurGuardArbitrator();
    private final StateVersion v1 = StateVersion.of(1L);

    @Test
    @DisplayName("Precedence 1: Industry Policy and Safety Violations are strictly REJECTED")
    void shouldRejectPolicyViolationsFirst() {
        ActionIntent intent = ActionIntent.builder()
                .id("I-POL")
                .agentId(AgentId.QUALITY)
                .actionType(ActionType.QUARANTINE_LOT)
                .targetEntityId("LOT-1")
                .riskScore(0.9)
                .build();

        Conflict policyConflict = Conflict.unary(
                ConflictType.INDUSTRY_POLICY_VIOLATION, intent.id(), "REGULATION",
                "Unapproved high-risk pharmaceutical deviation", Severity.CRITICAL);

        Map<IntentId, ArbitrationDecision> decisions = arbitrator.arbitrate(
                List.of(intent), List.of(policyConflict), v1);

        assertThat(decisions.get(intent.id()).outcome()).isEqualTo(ArbitrationStatus.REJECT);
        assertThat(decisions.get(intent.id()).reasonCode()).isEqualTo("POLICY_VIOLATION");
    }

    @Test
    @DisplayName("Precedence 2: Dependency Deadlocks trigger REQUEST_REPLANNING")
    void shouldRequestReplanningForDeadlocks() {
        ActionIntent intent = ActionIntent.builder()
                .id("I-DEADLOCK")
                .agentId(AgentId.LOGISTICS)
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("LANE-1")
                .build();

        Conflict depConflict = Conflict.of(
                ConflictType.DEPENDENCY_CONFLICT, intent.id(), IntentId.of("I-OTHER"),
                "CYCLE", "Circular dependency", Severity.CRITICAL);

        Map<IntentId, ArbitrationDecision> decisions = arbitrator.arbitrate(
                List.of(intent), List.of(depConflict), v1);

        assertThat(decisions.get(intent.id()).outcome()).isEqualTo(ArbitrationStatus.REQUEST_REPLANNING);
    }

    @Test
    @DisplayName("Precedence 5: Scarcity Contention favors lower risk and lower cost proposal")
    void shouldAwardScarcityContentionToOptimalProposal() {
        ActionIntent winner = ActionIntent.builder()
                .id("I-WINNER")
                .agentId(AgentId.INVENTORY)
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("STOCK-A")
                .riskScore(0.1)
                .estimatedCost(new BigDecimal("500"))
                .build();

        ActionIntent loser = ActionIntent.builder()
                .id("I-LOSER")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("STOCK-A")
                .riskScore(0.4)
                .estimatedCost(new BigDecimal("1200"))
                .build();

        Conflict qtyConflict = Conflict.of(
                ConflictType.QUANTITY_CONFLICT, winner.id(), loser.id(),
                "STOCK-A", "Competing inventory claims exceed stock", Severity.HIGH);

        Map<IntentId, ArbitrationDecision> decisions = arbitrator.arbitrate(
                List.of(winner, loser), List.of(qtyConflict), v1);

        assertThat(decisions.get(winner.id()).outcome()).isEqualTo(ArbitrationStatus.ALLOW);
        assertThat(decisions.get(loser.id()).outcome()).isEqualTo(ArbitrationStatus.REQUEST_REPLANNING);
        assertThat(decisions.get(loser.id()).reasonCode()).isEqualTo("RESOURCE_CONTENTION_LOST");
    }
}
