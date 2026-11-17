package com.supplygraph.web.init;

import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWorkspaceRepository;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.ImpactAnalysisApplicationService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initializes authoritative demo digital twin and disruption data for interactive exploration
 * in the Vaadin Flow Recovery Decision Centre.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    public static final String DEMO_WORKSPACE_ID = "WS-AUTO-01";

    private final SpringDataWorkspaceRepository workspaceRepo;
    private final DigitalTwinRepositoryPort digitalTwinRepo;
    private final InventoryLedgerRepositoryPort inventoryRepo;
    private final DisruptionApplicationService disruptionService;
    private final ImpactAnalysisApplicationService impactService;
    private final RecoveryPlanningApplicationService recoveryService;

    public DemoDataInitializer(
            SpringDataWorkspaceRepository workspaceRepo,
            DigitalTwinRepositoryPort digitalTwinRepo,
            InventoryLedgerRepositoryPort inventoryRepo,
            DisruptionApplicationService disruptionService,
            ImpactAnalysisApplicationService impactService,
            RecoveryPlanningApplicationService recoveryService
    ) {
        this.workspaceRepo = workspaceRepo;
        this.digitalTwinRepo = digitalTwinRepo;
        this.inventoryRepo = inventoryRepo;
        this.disruptionService = disruptionService;
        this.impactService = impactService;
        this.recoveryService = recoveryService;
    }

    @Override
    public void run(String... args) {
        WorkspaceId wsId = WorkspaceId.of(DEMO_WORKSPACE_ID);
        if (workspaceRepo.existsById(DEMO_WORKSPACE_ID)) {
            log.info("Demo workspace {} already initialized", DEMO_WORKSPACE_ID);
            return;
        }

        log.info("Seeding demo digital twin and recovery decision data for {}", DEMO_WORKSPACE_ID);

        // 1. Workspace
        workspaceRepo.save(new WorkspaceJpaEntity(
                DEMO_WORKSPACE_ID,
                "Automotive Powertrain & Electronics Global Network",
                "AUTOMOTIVE",
                Instant.now(),
                Instant.now()
        ));

        // 2. Canonical Digital Twin Graph
        Supplier tier2Fab = new Supplier(
                NodeId.of("FAB-MUNICH"), "Munich Advanced Silicon Fab",
                "Munich, DE", true, 2, Set.of("ISO9001", "IATF16949"), 0.15
        );
        Supplier tier1Supplier = new Supplier(
                NodeId.of("SUP-KYOTO"), "Kyoto Microelectronics Corp",
                "Kyoto, JP", true, 1, Set.of("ISO9001", "IATF16949"), 0.10
        );
        ManufacturingSite assemblyPlant = new ManufacturingSite(
                NodeId.of("PLANT-STUTTGART"), "Stuttgart Powertrain Assembly",
                "Stuttgart, DE", true, Set.of("LINE-POWERTRAIN-1", "LINE-POWERTRAIN-2"), "ISO-7", 500
        );
        Warehouse hub = new Warehouse(
                NodeId.of("HUB-FRANKFURT"), "Frankfurt Global Logistics Hub",
                "Frankfurt, DE", true, BigDecimal.valueOf(25000), false, false
        );

        TransportationLane lane1 = new TransportationLane(
                EdgeId.of("LANE-MUNICH-STUTTGART"), NodeId.of("FAB-MUNICH"), NodeId.of("PLANT-STUTTGART"),
                TransportMode.ROAD, Duration.ofHours(6), BigDecimal.valueOf(1200), true
        );
        TransportationLane lane2 = new TransportationLane(
                EdgeId.of("LANE-KYOTO-FRA"), NodeId.of("SUP-KYOTO"), NodeId.of("HUB-FRANKFURT"),
                TransportMode.AIR, Duration.ofHours(36), BigDecimal.valueOf(8500), true
        );
        TransportationLane lane3 = new TransportationLane(
                EdgeId.of("LANE-FRA-STUTTGART"), NodeId.of("HUB-FRANKFURT"), NodeId.of("PLANT-STUTTGART"),
                TransportMode.ROAD, Duration.ofHours(4), BigDecimal.valueOf(900), true
        );

        SupplyChainGraph graph = new SupplyChainGraph(
                List.of(tier2Fab, tier1Supplier, assemblyPlant, hub),
                List.of(lane1, lane2, lane3)
        );

        // 3. Multi-tier BOM
        com.supplygraph.domain.model.bom.Component mcu = new com.supplygraph.domain.model.bom.Component(
                MaterialId.of("MCU-AUTO-32"), "MCU-AUTO-32-SKU", "Automotive 32-bit MCU",
                UnitOfMeasure.PIECE, 14
        );
        FinishedProduct ecu = new FinishedProduct(
                MaterialId.of("ECU-POWERTRAIN-V8"), "ECU-V8-SKU", "Powertrain Controller ECU",
                UnitOfMeasure.PIECE, "POWERTRAIN", 36
        );
        BomEntry bomEntry = new BomEntry(
                ecu.getId(), mcu.getId(), Quantity.of(BigDecimal.valueOf(2), UnitOfMeasure.PIECE),
                BigDecimal.ZERO, 1
        );
        BillOfMaterials bom = new BillOfMaterials(
                List.of(ecu, mcu),
                List.of(bomEntry)
        );

        // 4. Persist Digital Twin State
        DigitalTwinState twinState = new DigitalTwinState(wsId, StateVersion.of(1L), graph, bom);
        digitalTwinRepo.save(twinState);

        // 5. Initial Inventory Balances
        inventoryRepo.updateBalance(
                wsId,
                InventoryAccountId.of(NodeId.of("PLANT-STUTTGART"), MaterialId.of("MCU-AUTO-32"), BalanceCategory.ON_HAND_AVAILABLE),
                Quantity.of(BigDecimal.valueOf(4500), UnitOfMeasure.PIECE)
        );
        inventoryRepo.updateBalance(
                wsId,
                InventoryAccountId.of(NodeId.of("HUB-FRANKFURT"), MaterialId.of("MCU-AUTO-32"), BalanceCategory.ON_HAND_AVAILABLE),
                Quantity.of(BigDecimal.valueOf(1500), UnitOfMeasure.PIECE)
        );

        // 6. Active Disruption Incident
        Disruption disruption = disruptionService.createDraft(
                wsId, "AUTOMOTIVE", "CHIP_SHORTAGE",
                "Munich Cleanroom Flooding & Fab Outage",
                "Severe flash flooding in semiconductor fabrication zone has damaged photolithography cleanrooms, halting MCU-AUTO-32 wafer output.",
                DisruptionCategory.MANUFACTURING_SITE_OUTAGE,
                DisruptionSeverity.CRITICAL,
                Set.of(NodeId.of("FAB-MUNICH")),
                Set.of(EdgeId.of("LANE-MUNICH-STUTTGART")),
                Set.of(MaterialId.of("MCU-AUTO-32")),
                Instant.now(),
                Instant.now().plus(Duration.ofDays(21)),
                new DisruptionImpactParameters(
                        BigDecimal.valueOf(100),
                        86400L * 21,
                        21,
                        BigDecimal.valueOf(25000),
                        "PIECES",
                        Map.of("impactedLines", "3")
                ),
                "Official Force Majeure declaration issued by fab management."
        );
        disruptionService.validateDisruption(disruption.id());
        Disruption activeDisruption = disruptionService.activateDisruption(disruption.id());

        // 7. Impact Assessment
        impactService.analyzeCascadingImpact(activeDisruption.id(), wsId);

        // 8. Generate Default Recovery Alternatives
        recoveryService.generateAlternativePlans(activeDisruption.id(), wsId);

        log.info("Demo initialization complete: Active disruption {} with cascading impact and 3 recovery alternatives.",
                activeDisruption.id().value());
    }
}
