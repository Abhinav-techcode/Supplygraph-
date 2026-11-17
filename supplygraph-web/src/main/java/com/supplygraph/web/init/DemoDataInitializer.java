package com.supplygraph.web.init;

import com.supplygraph.adapter.persistence.entity.WorkspaceJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataWorkspaceRepository;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.benchmark.BenchmarkExperimentRunner;
import com.supplygraph.application.port.out.BenchmarkExperimentRepositoryPort;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.application.port.out.MonitoringRuleRepositoryPort;
import com.supplygraph.application.port.out.UserProfileRepositoryPort;
import com.supplygraph.application.provenance.DataProvenanceService;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.ImpactAnalysisApplicationService;
import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.benchmark.BenchmarkBaseline;
import com.supplygraph.domain.model.benchmark.BenchmarkScenarioType;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.event.EventProvenance;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.monitoring.MonitoringEvaluationTrigger;
import com.supplygraph.domain.model.monitoring.MonitoringRule;
import com.supplygraph.domain.model.monitoring.MonitoringSeverity;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
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
 * Initializes authoritative demo digital twin, disruption, continuous intelligence, enterprise security,
 * and provenance data for interactive exploration in SupplyGraph.
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
    private final MonitoringRuleRepositoryPort monitoringRuleRepo;
    private final OperationalEventIngestionService eventIngestionService;
    private final UserProfileRepositoryPort userProfileRepo;
    private final AuditGovernanceService auditGovernanceService;
    private final DataProvenanceService provenanceService;
    private final BenchmarkExperimentRunner benchmarkRunner;

    public DemoDataInitializer(
            SpringDataWorkspaceRepository workspaceRepo,
            DigitalTwinRepositoryPort digitalTwinRepo,
            InventoryLedgerRepositoryPort inventoryRepo,
            DisruptionApplicationService disruptionService,
            ImpactAnalysisApplicationService impactService,
            RecoveryPlanningApplicationService recoveryService,
            MonitoringRuleRepositoryPort monitoringRuleRepo,
            OperationalEventIngestionService eventIngestionService,
            UserProfileRepositoryPort userProfileRepo,
            AuditGovernanceService auditGovernanceService,
            DataProvenanceService provenanceService,
            BenchmarkExperimentRunner benchmarkRunner
    ) {
        this.workspaceRepo = workspaceRepo;
        this.digitalTwinRepo = digitalTwinRepo;
        this.inventoryRepo = inventoryRepo;
        this.disruptionService = disruptionService;
        this.impactService = impactService;
        this.recoveryService = recoveryService;
        this.monitoringRuleRepo = monitoringRuleRepo;
        this.eventIngestionService = eventIngestionService;
        this.userProfileRepo = userProfileRepo;
        this.auditGovernanceService = auditGovernanceService;
        this.provenanceService = provenanceService;
        this.benchmarkRunner = benchmarkRunner;
    }

    @Override
    public void run(String... args) {
        WorkspaceId wsId = WorkspaceId.of(DEMO_WORKSPACE_ID);
        if (workspaceRepo.existsById(DEMO_WORKSPACE_ID)) {
            log.info("Demo workspace {} already initialized", DEMO_WORKSPACE_ID);
            return;
        }

        log.info("Seeding demo digital twin, recovery decision, and security data for {}", DEMO_WORKSPACE_ID);

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

        // 9. Seed Default Continuous Monitoring Rules
        monitoringRuleRepo.save(MonitoringRule.of(
                "RULE-SUP-DELAY", "Supplier Delivery Delay Threshold",
                "Alerts when an inbound supplier shipment experiences transit delay exceeding 24 hours.",
                "AUTOMOTIVE", MonitoringSeverity.HIGH, BigDecimal.valueOf(24),
                MonitoringEvaluationTrigger.ON_EVENT, "SUPPLIER_DELAY_THRESHOLD"
        ));
        monitoringRuleRepo.save(MonitoringRule.of(
                "RULE-CAP-DROP", "Operational Capacity Drop Threshold",
                "Alerts when plant or vendor throughput capacity diminishes by 20% or more.",
                "ALL", MonitoringSeverity.CRITICAL, BigDecimal.valueOf(20),
                MonitoringEvaluationTrigger.ON_EVENT, "CAPACITY_DROP_THRESHOLD"
        ));
        monitoringRuleRepo.save(MonitoringRule.of(
                "RULE-SAFE-STOCK", "Inventory Safety Stock Buffer Breach",
                "Alerts when warehouse on-hand physical stock drops below minimum safety threshold (2,000 units).",
                "AUTOMOTIVE", MonitoringSeverity.MEDIUM, BigDecimal.valueOf(2000),
                MonitoringEvaluationTrigger.ON_EVENT, "INVENTORY_SAFETY_STOCK_BREACH"
        ));
        monitoringRuleRepo.save(MonitoringRule.of(
                "RULE-QUAL-HOLD", "Quality Containment Hold Incident",
                "Alerts immediately when incoming material batches are quarantined on quality hold.",
                "ALL", MonitoringSeverity.CRITICAL, BigDecimal.ZERO,
                MonitoringEvaluationTrigger.ON_EVENT, "QUALITY_HOLD_INCIDENT"
        ));

        // 10. Ingest Initial Seed Telemetry Events
        OperationalEvent seedEvent1 = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now().minus(Duration.ofHours(2)),
                "FAB-MUNICH",
                "NODE",
                wsId,
                Map.of("delayHours", "36", "carrier", "DHL_FREIGHT", "route", "LANE-MUNICH-STUTTGART"),
                EventProvenance.imported("ERP_SAP", "BATCH-SEED-01", "system_telemetry"),
                "SEED-EVT-01"
        );
        eventIngestionService.ingestEvent(seedEvent1);

        OperationalEvent seedEvent2 = OperationalEvent.createIngested(
                EventType.INVENTORY_LEVEL_CHANGED,
                EventSource.INTERNAL_WORKFLOW,
                Instant.now().minus(Duration.ofMinutes(45)),
                "HUB-FRANKFURT",
                "NODE",
                wsId,
                Map.of("onHandQuantity", "1500", "unit", "PIECES"),
                EventProvenance.internal("warehouse_wms"),
                "SEED-EVT-02"
        );
        eventIngestionService.ingestEvent(seedEvent2);

        // 11. Seed Enterprise Users (RBAC)
        userProfileRepo.save(UserProfile.create(
                "USR-ADMIN-01", "admin", "admin@supplygraph.io",
                "Enterprise System Administrator", UserRole.SYSTEM_ADMIN, Set.of(wsId)
        ));
        userProfileRepo.save(UserProfile.create(
                "USR-SCM-01", "sc_manager", "scm@supplygraph.io",
                "Lead Supply Chain Manager", UserRole.SUPPLY_CHAIN_MANAGER, Set.of(wsId)
        ));
        userProfileRepo.save(UserProfile.create(
                "USR-ANALYST-01", "analyst", "analyst@supplygraph.io",
                "Senior Operations Analyst", UserRole.ANALYST, Set.of(wsId)
        ));

        // 12. Seed Governance Audit Sign-Off
        auditGovernanceService.logAction(
                UserId.of("USR-SCM-01"),
                wsId,
                AuditAction.DISRUPTION_CREATED,
                activeDisruption.id().value().toString(),
                "v0",
                "v1",
                "Activated disruption incident following Bavaria cleanroom flooding notice",
                "CONF-NONE"
        );

        // 13. Seed Data Provenance Records
        provenanceService.registerProvenance(
                wsId, "FAB-MUNICH", "SUPPLIER_FACILITY", ProvenanceType.REAL_IMPORTED,
                "SAP-SUPPLIER-MASTER-2026", "v1",
                Map.of("criticality", "TIER_1", "qualificationStatus", "CERTIFIED")
        );
        provenanceService.registerProvenance(
                wsId, "MCU-AUTO-32", "MATERIAL", ProvenanceType.REAL_IMPORTED,
                "PLM-TEAMCENTER-BOM-SYNC", "v1",
                Map.of("rohsCompliant", "true", "automotiveGrade", "AEC-Q100")
        );

        // 14. Seed Initial Research Benchmark Trial
        benchmarkRunner.runTrial(
                "Baseline Comparative Trial on Munich Fab Outage",
                BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING,
                42L,
                3
        );

        log.info("Demo initialization complete: Active disruption {} with cascading impact, recovery alternatives, monitoring rules, enterprise users, audit trail, and research benchmark.",
                activeDisruption.id().value());
    }
}
