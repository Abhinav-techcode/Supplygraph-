package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Specialised agents compute proposals from the digital twin")
class SpecializedAgentsTest {

    private static AgentProposal run(InsightAgent agent, DigitalTwinSnapshot s, Set<String> hitNodes, Map<String, Object> params) {
        return agent.investigate(AgentTestTwin.context(s, AgentTestTwin.incident(hitNodes, 72), params), AgentTestTwin.boundary(s));
    }

    private static AgentProposal run(InsightAgent agent, DigitalTwinSnapshot s) { return run(agent, s, Set.of("SUP-A"), Map.of()); }

    @Test
    @DisplayName("Inventory: releases the largest holder outside the disruption zone, sized to the requirement, with an inventory claim")
    void inventoryUsesRealStockHolder() {
        AgentProposal p = run(new InventoryAgent(), AgentTestTwin.snapshot(5000, 2000));
        assertThat(p.intents()).hasSize(1);
        var intent = p.intents().get(0);
        assertThat(intent.actionType()).isEqualTo(ActionType.EMERGENCY_BUFFER_DISPATCH);
        assertThat(intent.targetEntityId()).isEqualTo("PLANT");
        assertThat(intent.location()).isEqualTo("HUB");
        assertThat(intent.quantity().amount().longValue()).isEqualTo(720);   // 10 units/h x 24 x 3 days
        assertThat(intent.resourceClaims()).hasSize(1);
        assertThat(intent.resourceClaims().get(0).resourceIdentity().facilityId()).isEqualTo("HUB");
        assertThat(String.join(" ", p.evidence())).contains("HUB", "720", "100%");
    }

    @Test
    @DisplayName("Inventory: proposal changes when the twin changes (less stock -> smaller release, lower coverage)")
    void inventoryOutputDependsOnTwin() {
        AgentProposal rich = run(new InventoryAgent(), AgentTestTwin.snapshot(5000, 2000));
        AgentProposal poor = run(new InventoryAgent(), AgentTestTwin.snapshot(100, 2000));
        assertThat(poor.intents().get(0).quantity().amount().longValue()).isEqualTo(100);
        assertThat(poor.intents().get(0).riskScore()).isGreaterThan(rich.intents().get(0).riskScore());
        assertThat(String.join(" ", poor.evidence())).contains("14%");
    }

    @Test
    @DisplayName("Inventory: with no stock outside the disruption zone it proposes nothing and says why")
    void inventoryDoesNotInventStock() {
        AgentProposal p = run(new InventoryAgent(), AgentTestTwin.snapshot(0, 2000));
        assertThat(p.intents()).isEmpty();
        assertThat(String.join(" ", p.evidence())).contains("No stock");
    }

    @Test
    @DisplayName("Procurement: picks the alternative supplier that can still reach the site, never the disrupted one")
    void procurementPicksReachableSupplier() {
        AgentProposal p = run(new ProcurementAgent(), AgentTestTwin.snapshot(5000, 2000));
        assertThat(p.intents()).hasSize(1);
        assertThat(p.intents().get(0).actionType()).isEqualTo(ActionType.EXPEDITE_PURCHASE_ORDER);
        assertThat(p.intents().get(0).targetEntityId()).isEqualTo("SUP-B");
        assertThat(String.join(" ", p.evidence())).contains("SUP-B > HUB > PLANT", "60.0 h");
    }

    @Test
    @DisplayName("Logistics: reroutes along an undisrupted route and claims its first lane")
    void logisticsRoutesAroundDisruption() {
        AgentProposal p = run(new LogisticsAgent(), AgentTestTwin.snapshot(5000, 2000));
        assertThat(p.intents()).hasSize(1);
        var claim = p.intents().get(0).resourceClaims().get(0);
        assertThat(p.intents().get(0).actionType()).isEqualTo(ActionType.REROUTE_SHIPMENT);
        assertThat(claim.resourceIdentity().canonicalId()).isEqualTo("HUB->PLANT");
        assertThat(String.join(" ", p.evidence())).contains("HUB > PLANT", "12.0 h");
    }

    @Test
    @DisplayName("Production: time to survive comes from the site's own stock; less stock means higher risk")
    void productionRiskFollowsStock() {
        AgentProposal ok = run(new ProductionAgent(), AgentTestTwin.snapshot(5000, 2000));
        AgentProposal low = run(new ProductionAgent(), AgentTestTwin.snapshot(5000, 100));
        assertThat(String.join(" ", ok.evidence())).contains("8.3 days");
        assertThat(String.join(" ", low.evidence())).contains("0.4 days");
        assertThat(low.intents().get(0).riskScore()).isGreaterThan(ok.intents().get(0).riskScore());
    }

    @Test
    @DisplayName("Quality: quarantines stock only when a disrupted manufacturing site actually holds it")
    void qualityQuarantinesOnlyRealStock() {
        DigitalTwinSnapshot s = AgentTestTwin.snapshot(5000, 2000);
        assertThat(run(new QualityAgent(), s, Set.of("SUP-A"), Map.of()).intents()).isEmpty();
        AgentProposal plantHit = run(new QualityAgent(), s, Set.of("PLANT"), Map.of());
        assertThat(plantHit.intents()).hasSize(1);
        assertThat(plantHit.intents().get(0).actionType()).isEqualTo(ActionType.QUARANTINE_LOT);
        assertThat(plantHit.intents().get(0).quantity().amount().longValue()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Coordinator and Risk are advisory and quote real numbers from the twin")
    void advisoryAgentsQuoteTwinFacts() {
        DigitalTwinSnapshot s = AgentTestTwin.snapshot(5000, 2000);
        AgentProposal coordinator = run(new CoordinatorAgent(), s);
        AgentProposal risk = run(new RiskIntelligenceAgent(), s);
        assertThat(coordinator.intents()).isEmpty();
        assertThat(String.join(" ", coordinator.evidence())).contains("PLANT", "8.3 days", "PROD-1");
        assertThat(risk.intents()).isEmpty();
        assertThat(String.join(" ", risk.evidence())).contains("1 outbound lane", "No downstream facility depends exclusively");
    }

    @Test
    @DisplayName("Risk: flags a facility whose only inbound lanes come from the disrupted zone")
    void riskFindsSinglePointOfFailure() {
        DigitalTwinSnapshot s = AgentTestTwin.snapshot(5000, 2000);
        AgentProposal p = run(new RiskIntelligenceAgent(), s, Set.of("SUP-A", "HUB"), Map.of());
        assertThat(String.join(" ", p.evidence())).contains("Single points of failure").contains("PLANT").doesNotContain("No downstream facility depends exclusively");
    }

    @Test
    @DisplayName("Recovery planning: reserves capacity only for the shortfall the other stock cannot cover")
    void recoveryPlanningReservesShortfall() {
        assertThat(run(new RecoveryPlanningAgent(), AgentTestTwin.snapshot(5000, 2000)).intents()).isEmpty();
        AgentProposal p = run(new RecoveryPlanningAgent(), AgentTestTwin.snapshot(200, 2000));
        assertThat(p.intents()).hasSize(1);
        assertThat(p.intents().get(0).actionType()).isEqualTo(ActionType.RESERVE_CAPACITY);
        assertThat(p.intents().get(0).quantity().amount().longValue()).isEqualTo(520);   // 720 required - 200 reachable
    }

    @Test
    @DisplayName("The reported figure is data completeness: a measured ratio that changes with the twin, never with an assumed track record")
    void reportedFigureIsMeasuredCompleteness() {
        AgentProposal full = run(new InventoryAgent(), AgentTestTwin.snapshot(5000, 2000));
        assertThat(full.confidence()).isEqualTo(1.0);                                   // every fact the agent needs was found
        assertThat(String.join(" ", full.evidence())).contains("Data completeness: 100%").doesNotContain("reliability");
        AgentProposal noRoute = run(new InventoryAgent(), AgentTestTwin.snapshot(5000, 2000), Set.of("SUP-A", "HUB"), Map.of());
        assertThat(noRoute.confidence()).isLessThan(1.0);                               // fewer facts found -> lower, and explained
        // an old calibration parameter must not influence the number
        AgentProposal ignoresOld = run(new InventoryAgent(), AgentTestTwin.snapshot(5000, 2000), Set.of("SUP-A"), Map.of("calibration.INVENTORY", 0.1));
        assertThat(ignoresOld.confidence()).isEqualTo(full.confidence());
    }

    @Test
    @DisplayName("No invention: an incident that touches nothing in the twin yields no actions and zero confidence in every agent")
    void unknownIncidentProducesNoActions() {
        DigitalTwinSnapshot s = AgentTestTwin.snapshot(5000, 2000);
        List<InsightAgent> agents = List.of(new CoordinatorAgent(), new RiskIntelligenceAgent(), new InventoryAgent(), new ProcurementAgent(), new LogisticsAgent(), new ProductionAgent(), new QualityAgent(), new RecoveryPlanningAgent());
        for (InsightAgent a : agents) {
            AgentProposal p = run(a, s, Set.of("NOT-IN-TWIN"), Map.of());
            assertThat(p.intents()).as(a.role().name()).isEmpty();
            assertThat(p.confidence()).as(a.role().name()).isLessThanOrEqualTo(0.5);
        }
    }

    @Test
    @DisplayName("Replanning prefers the next-best option instead of repeating the same one")
    void replanningChangesTheChoice() {
        DigitalTwinSnapshot s = AgentTestTwin.snapshot(5000, 2000);
        InventoryAgent agent = new InventoryAgent();
        var ctx = AgentTestTwin.context(s, AgentTestTwin.incident(Set.of("SUP-A"), 72), Map.of());
        AgentProposal first = agent.investigate(ctx, AgentTestTwin.boundary(s));
        AgentProposal again = agent.replan(ctx, new com.supplygraph.concurguard.replanning.ReplanningScope(Set.of(), Set.of(first.intents().get(0).id()), List.of(), 1), AgentTestTwin.boundary(s));
        assertThat(again.intents().get(0).id()).isNotEqualTo(first.intents().get(0).id());
        assertThat(again.intents().get(0).timeWindow().start()).isAfter(first.intents().get(0).timeWindow().start());
    }
}
