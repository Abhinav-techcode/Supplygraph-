package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.application.port.out.RecoveryPlanRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoveryPlanningApplicationServiceTest {

    private RecoveryPlanRepositoryPort planRepo;
    private DisruptionRepositoryPort disruptionRepo;
    private DigitalTwinRepositoryPort twinRepo;
    private ImpactAssessmentRepositoryPort assessmentRepo;
    private RecoveryPlanningApplicationService service;

    private final WorkspaceId ws = WorkspaceId.of("ws-plan-test");
    private final Map<RecoveryPlanId, RecoveryPlan> storage = new HashMap<>();

    @BeforeEach
    void setUp() {
        planRepo = mock(RecoveryPlanRepositoryPort.class);
        disruptionRepo = mock(DisruptionRepositoryPort.class);
        twinRepo = mock(DigitalTwinRepositoryPort.class);
        assessmentRepo = mock(ImpactAssessmentRepositoryPort.class);
        storage.clear();

        doAnswer(inv -> {
            RecoveryPlan p = inv.getArgument(0);
            storage.put(p.id(), p);
            return null;
        }).when(planRepo).save(any(RecoveryPlan.class));

        doAnswer(inv -> {
            RecoveryPlanId id = inv.getArgument(0);
            return Optional.ofNullable(storage.get(id));
        }).when(planRepo).findById(any(RecoveryPlanId.class));

        service = new RecoveryPlanningApplicationService(planRepo, disruptionRepo, twinRepo, assessmentRepo);
    }


    /** A small real network: FAB (disrupted) and SUP-2 feed PLANT; WH holds buffer stock of CHIP. */
    private DigitalTwinState realTwin() {
        var fab = new com.supplygraph.domain.model.graph.Supplier(com.supplygraph.domain.model.graph.NodeId.of("FAB"), "Fab", "Munich", true, 2, Set.of(), 0.15);
        var alt = new com.supplygraph.domain.model.graph.Supplier(com.supplygraph.domain.model.graph.NodeId.of("SUP-2"), "Alt", "Hsinchu", true, 2, Set.of(), 0.10);
        var wh = new com.supplygraph.domain.model.graph.Warehouse(com.supplygraph.domain.model.graph.NodeId.of("WH"), "Warehouse", "Ulm", true, new java.math.BigDecimal("500"), false, false);
        var plant = new com.supplygraph.domain.model.graph.ManufacturingSite(com.supplygraph.domain.model.graph.NodeId.of("PLANT"), "Plant", "Stuttgart", true, Set.of("L1"), "", 10);
        java.util.function.BiFunction<String[], Integer, com.supplygraph.domain.model.graph.TransportationLane> lane = (n, h) -> new com.supplygraph.domain.model.graph.TransportationLane(
                com.supplygraph.domain.model.graph.EdgeId.of(n[0]), com.supplygraph.domain.model.graph.NodeId.of(n[1]), com.supplygraph.domain.model.graph.NodeId.of(n[2]), com.supplygraph.domain.model.graph.TransportMode.ROAD, java.time.Duration.ofHours(h), java.math.BigDecimal.ONE, true);
        var graph = new SupplyChainGraph(List.of(fab, alt, wh, plant), List.of(lane.apply(new String[]{"FAB-PLANT", "FAB", "PLANT"}, 6), lane.apply(new String[]{"ALT-PLANT", "SUP-2", "PLANT"}, 20), lane.apply(new String[]{"WH-PLANT", "WH", "PLANT"}, 5)));
        DigitalTwinState s = new DigitalTwinState(ws, StateVersion.of(2L), graph, BillOfMaterials.empty());
        var acct = com.supplygraph.domain.model.inventory.InventoryAccountId.of(com.supplygraph.domain.model.graph.NodeId.of("WH"), com.supplygraph.domain.model.bom.MaterialId.of("CHIP"), com.supplygraph.domain.model.inventory.BalanceCategory.ON_HAND_AVAILABLE);
        var off = com.supplygraph.domain.model.inventory.InventoryAccountId.of(com.supplygraph.domain.model.graph.NodeId.of("WH"), com.supplygraph.domain.model.bom.MaterialId.of("CHIP"), com.supplygraph.domain.model.inventory.BalanceCategory.EXTERNAL_OFFSET);
        var q = com.supplygraph.domain.model.common.Quantity.of(1000, com.supplygraph.domain.model.common.UnitOfMeasure.PIECE);
        s.getInventoryLedger().postTransaction(new com.supplygraph.domain.model.inventory.InventoryTransaction(com.supplygraph.domain.model.inventory.TransactionId.generate(), Instant.now(), "SEED", "t",
                List.of(new com.supplygraph.domain.model.inventory.InventoryPosting("a", acct, com.supplygraph.domain.model.inventory.PostingType.DEBIT, q),
                        new com.supplygraph.domain.model.inventory.InventoryPosting("b", off, com.supplygraph.domain.model.inventory.PostingType.CREDIT, q))));
        return s;
    }

    private Disruption realDisruption() {
        Instant now = Instant.now();
        return Disruption.createDraft(ws, "AUTOMOTIVE", "SHUTDOWN", "Fab outage", "d", DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.CRITICAL,
                Set.of(com.supplygraph.domain.model.graph.NodeId.of("FAB")), Set.of(), Set.of(com.supplygraph.domain.model.bom.MaterialId.of("CHIP")), now, now.plusSeconds(86400 * 2),
                new DisruptionImpactParameters(java.math.BigDecimal.valueOf(100), 86400L * 2, 2, java.math.BigDecimal.valueOf(500), "PCS", java.util.Map.of()), "e");
    }

    @Test
    @DisplayName("Generate alternatives derives 2 candidate recovery plans from the twin and validates with ConcurGuard")
    void testMultiAlternativeGenerationAndValidation() {
        Instant now = Instant.now();
        Disruption disruption = realDisruption();
        DigitalTwinState twinState = realTwin();

        when(disruptionRepo.findById(disruption.id())).thenReturn(Optional.of(disruption));
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        List<RecoveryPlan> alternatives = service.generateAlternativePlans(disruption.id(), ws);
        assertThat(alternatives).hasSize(2);   // buffer release + alternative supplier; no stock at the plant, so no resequencing option
        assertThat(alternatives).extracting(RecoveryPlan::planName).anyMatch(n -> n.contains("WH"));   // derived from the twin, not a template

        RecoveryPlan planAlpha = alternatives.get(0);
        assertThat(planAlpha.status()).isEqualTo(RecoveryPlanStatus.DRAFT);

        // Validate plan with ConcurGuard
        RecoveryPlan validated = service.validatePlanWithConcurGuard(planAlpha.id());
        assertThat(validated.concurGuardValidated()).isTrue();
        assertThat(validated.status()).isEqualTo(RecoveryPlanStatus.PENDING_APPROVAL);

        // Approve by human decision-maker
        RecoveryPlan approved = service.approvePlan(planAlpha.id(), "DIRECTOR_LOGISTICS", "Approved optimal buffer dispatch");
        assertThat(approved.status()).isEqualTo(RecoveryPlanStatus.APPROVED);
        assertThat(approved.approval().decision()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Stale plan cannot be approved if digital twin version has advanced")
    void testStalePlanApprovalRejected() {
        Instant now = Instant.now();
        Disruption disruption = realDisruption();
        // Twin was v2 when plan created
        DigitalTwinState twinState = realTwin();
        when(disruptionRepo.findById(disruption.id())).thenReturn(Optional.of(disruption));
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(twinState));

        List<RecoveryPlan> alternatives = service.generateAlternativePlans(disruption.id(), ws);
        RecoveryPlan plan = service.validatePlanWithConcurGuard(alternatives.get(0).id());

        // Twin version advances to v3 before approval
        DigitalTwinState advancedTwin = new DigitalTwinState(ws, StateVersion.of(3L), SupplyChainGraph.empty(), BillOfMaterials.empty());
        when(twinRepo.findByWorkspaceId(ws)).thenReturn(Optional.of(advancedTwin));

        assertThatThrownBy(() -> service.approvePlan(plan.id(), "CHIEF_OPERATING_OFFICER", "Approve stale"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stale plan cannot be approved");
    }
}

