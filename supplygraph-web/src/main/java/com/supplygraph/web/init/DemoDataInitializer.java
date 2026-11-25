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
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.EchelonTier;
import com.supplygraph.domain.model.planning.ServiceLevel;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import com.supplygraph.domain.model.collaboration.*;
import com.supplygraph.domain.model.sustainability.*;
import com.supplygraph.domain.model.platform.*;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.application.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Initializes authoritative demo digital twin, disruption, continuous intelligence, enterprise security,
 * and provenance data for interactive exploration in SupplyGraph.
 */
@Component
@org.springframework.context.annotation.Profile("!prod | demo")   // not in production, unless the "demo" profile is switched on for a public demo
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
    private final com.supplygraph.application.integration.EnterpriseDataImportService importService;
    private final com.supplygraph.application.quality.DataQualityEngineService qualityService;
    private final com.supplygraph.application.intelligence.SupplierIntelligenceService supplierService;
    private final com.supplygraph.application.intelligence.MaterialIntelligenceService materialService;
    private final com.supplygraph.application.intelligence.SupplyChainResilienceService resilienceService;
    private final com.supplygraph.application.explainability.ExplainableRecommendationService explainableRecommendationService;
    private final com.supplygraph.application.provenance.DecisionProvenanceGraphService decisionProvenanceGraphService;
    private final com.supplygraph.application.whatif.WhatIfAnalysisService whatIfAnalysisService;
    private final com.supplygraph.application.pareto.MultiObjectiveParetoService multiObjectiveParetoService;
    private final com.supplygraph.application.sensitivity.SensitivityAnalysisService sensitivityAnalysisService;
    private final com.supplygraph.application.resilience.AdvancedResilienceAnalyticsService advancedResilienceAnalyticsService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.planning.DemandPlanningService demandPlanningService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.planning.MultiEchelonInventoryService multiEchelonInventoryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.planning.ProcurementPlanningService procurementPlanningService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.planning.ProductionPlanningService productionPlanningService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.planning.PlanningScenarioService planningScenarioService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.collaboration.SupplierPortalService supplierPortalService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.collaboration.OrderCollaborationService orderCollaborationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.collaboration.RfqCollaborationService rfqCollaborationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.collaboration.SupplierQualityCollaborationService supplierQualityCollaborationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.collaboration.PartnerDocumentExchangeService partnerDocumentExchangeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.sustainability.CarbonAccountingService carbonAccountingService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.sustainability.SupplierSustainabilityService supplierSustainabilityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.sustainability.ProductTraceabilityService productTraceabilityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.sustainability.CircularEconomyService circularEconomyService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.sustainability.EnvironmentalScenarioService environmentalScenarioService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.platform.IndustryPolicyLifecycleService industryPolicyService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.platform.WorkspaceAdministrationService workspaceAdminService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.platform.ApiDeveloperCentreService apiDeveloperCentreService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.platform.BackupDisasterRecoveryService backupDisasterRecoveryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.connector.ConnectorRegistryService connectorRegistryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.connector.ConnectorExecutionService connectorExecutionService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.port.out.ExecutionWorkflowRepositoryPort executionWorkflowRepo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.service.SupplyChainImportService networkImport;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.supplygraph.application.service.SyntheticNetworkGenerator networkGenerator;

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
            BenchmarkExperimentRunner benchmarkRunner,
            com.supplygraph.application.integration.EnterpriseDataImportService importService,
            com.supplygraph.application.quality.DataQualityEngineService qualityService,
            com.supplygraph.application.intelligence.SupplierIntelligenceService supplierService,
            com.supplygraph.application.intelligence.MaterialIntelligenceService materialService,
            com.supplygraph.application.intelligence.SupplyChainResilienceService resilienceService,
            com.supplygraph.application.explainability.ExplainableRecommendationService explainableRecommendationService,
            com.supplygraph.application.provenance.DecisionProvenanceGraphService decisionProvenanceGraphService,
            com.supplygraph.application.whatif.WhatIfAnalysisService whatIfAnalysisService,
            com.supplygraph.application.pareto.MultiObjectiveParetoService multiObjectiveParetoService,
            com.supplygraph.application.sensitivity.SensitivityAnalysisService sensitivityAnalysisService,
            com.supplygraph.application.resilience.AdvancedResilienceAnalyticsService advancedResilienceAnalyticsService
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
        this.importService = importService;
        this.qualityService = qualityService;
        this.supplierService = supplierService;
        this.materialService = materialService;
        this.resilienceService = resilienceService;
        this.explainableRecommendationService = explainableRecommendationService;
        this.decisionProvenanceGraphService = decisionProvenanceGraphService;
        this.whatIfAnalysisService = whatIfAnalysisService;
        this.multiObjectiveParetoService = multiObjectiveParetoService;
        this.sensitivityAnalysisService = sensitivityAnalysisService;
        this.advancedResilienceAnalyticsService = advancedResilienceAnalyticsService;
    }

    @Override
    public void run(String... args) {
        seedDemoData(false);
    }

    private void ensureUser(String id, String username, String email, String displayName, UserRole role, WorkspaceId wsId) {
        try {
            if (userProfileRepo.findByUsername(username).isPresent()) return;
            userProfileRepo.save(UserProfile.create(id, username, email, displayName, role, Set.of(wsId)));
        } catch (Exception ex) {
            log.warn("Non-fatal: user {} not seeded: {}", username, ex.getMessage());
        }
    }

    /** Idempotent: SIMULATED connectors (they never contact anything), one test and one synchronisation each. */
    private void seedIntegrationActivity(WorkspaceId wsId) {
        if (connectorRegistryService == null || connectorExecutionService == null) return;
        try {
            UserProfile admin = userProfileRepo.findByUsername("admin").orElse(null);
            if (admin == null) return;
            SecurityContext.runAs(admin, () -> {
                try {
                    if (connectorRegistryService.list(wsId).isEmpty()) {
                        record C(String name, ConnectorCategory cat) {}
                        for (C c : List.of(new C("SAP S/4HANA ERP (simulated)", ConnectorCategory.ERP),
                                new C("Manhattan SCALE WMS (simulated)", ConnectorCategory.WMS),
                                new C("Oracle Transportation Management (simulated)", ConnectorCategory.TMS),
                                new C("Siemens Teamcenter PLM (simulated)", ConnectorCategory.PLM))) {
                            var created = connectorRegistryService.register(wsId, c.name(), c.cat(), ConnectorMode.SANDBOX_SIMULATED, "sandbox");
                            var configured = connectorRegistryService.configure(created.id(), new ConnectorConfiguration(null, null, List.of(), 5000, 1_048_576, 3));
                            connectorRegistryService.activate(configured.id());
                        }
                        log.info("Demo: 4 simulated connectors registered");
                    }
                    if (connectorExecutionService.workspaceHistory(wsId, 1).isEmpty()) {
                        for (var c : connectorRegistryService.list(wsId)) {
                            connectorExecutionService.testConnectivity(c.id());
                            connectorExecutionService.synchronize(c.id());
                        }
                        log.info("Demo: simulated connector runs recorded");
                    }
                } catch (Exception ex) {
                    log.warn("Non-fatal: integration activity seeding notice: {}", ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Non-fatal: integration activity seeding notice: {}", ex.getMessage());
        }
    }

    /** Idempotent: about thirty live incidents spread over all ten supply chains, each on a real node of the twin, so the map and the recovery pages have plenty to show. */
    private void addExtraDisruptions(WorkspaceId wsId) {
        try {
            var twin = digitalTwinRepo.findByWorkspaceId(wsId).orElse(null);
            if (twin == null) return;
            var existing = disruptionService.listDisruptions(wsId).stream().map(Disruption::title).collect(java.util.stream.Collectors.toSet());
            String[] prefixes = {"ELEC-", "PHAR-", "AERO-", "FOOD-", "APPA-", "MEDI-", "SOLR-", "POLY-", "HEAV-", ""};
            String[] labels = {"Electronics", "Pharma", "Aerospace", "Food & beverage", "Apparel", "Medical devices", "Solar", "Chemicals", "Heavy machinery", "Automotive"};
            var all = new ArrayList<>(twin.getGraph().getAllNodes());
            all.sort(java.util.Comparator.comparing(n -> n.getId().value()));
            DisruptionCategory[] supCats = {DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionCategory.SUPPLIER_CAPACITY_REDUCTION, DisruptionCategory.RAW_MATERIAL_SHORTAGE};
            String[] supWhy = {"shut down after a labour dispute", "running at reduced capacity after an equipment failure", "short of raw material after a delayed shipment"};
            DisruptionCategory[] siteCats = {DisruptionCategory.MANUFACTURING_SITE_OUTAGE, DisruptionCategory.QUALITY_REJECTION, DisruptionCategory.INVENTORY_CONTAMINATION};
            String[] siteWhy = {"stopped by a power and cooling failure", "holding output after a quality rejection", "quarantining stock after a contamination finding"};
            DisruptionCategory[] hubCats = {DisruptionCategory.LOGISTICS_HUB_CLOSURE, DisruptionCategory.REGULATORY_RESTRICTION, DisruptionCategory.LEAD_TIME_INCREASE};
            String[] hubWhy = {"closed by severe weather", "restricted by a customs inspection", "slowed by a congestion backlog"};
            DisruptionSeverity[] sev = {DisruptionSeverity.CRITICAL, DisruptionSeverity.HIGH, DisruptionSeverity.MEDIUM};
            int made = 0;
            for (int g = 0; g < prefixes.length; g++) {
                final String pre = prefixes[g];
                final var group = all.stream().filter(n -> pre.isEmpty()
                        ? java.util.stream.Stream.of("ELEC-", "PHAR-", "AERO-", "FOOD-", "APPA-", "MEDI-", "SOLR-", "POLY-", "HEAV-").noneMatch(x -> n.getId().value().startsWith(x))
                        : n.getId().value().startsWith(pre)).toList();
                var sups = group.stream().filter(n -> n instanceof Supplier).toList();
                var sites = group.stream().filter(n -> n instanceof ManufacturingSite).toList();
                var hubs = group.stream().filter(n -> n instanceof LogisticsHub || n instanceof Warehouse).toList();
                java.util.List<com.supplygraph.domain.model.bom.MaterialId> mats = new ArrayList<>();
                for (var e : twin.getInventoryLedger().getAllBalances().entrySet()) {
                    String nid = e.getKey().nodeId().value();
                    boolean inGroup = pre.isEmpty()
                            ? java.util.stream.Stream.of("ELEC-", "PHAR-", "AERO-", "FOOD-", "APPA-", "MEDI-", "SOLR-", "POLY-", "HEAV-").noneMatch(nid::startsWith)
                            : nid.startsWith(pre);
                    if (inGroup && e.getKey().category() == BalanceCategory.ON_HAND_AVAILABLE && !mats.contains(e.getKey().materialId()) && mats.size() < 8) mats.add(e.getKey().materialId());
                }
                for (int k = 0; k < 3; k++) {
                    var pool = k == 0 ? sups : k == 1 ? sites : hubs;
                    if (pool.isEmpty()) continue;
                    int v = (g + k) % 3;
                    DisruptionCategory cat = k == 0 ? supCats[v] : k == 1 ? siteCats[v] : hubCats[v];
                    String why = k == 0 ? supWhy[v] : k == 1 ? siteWhy[v] : hubWhy[v];
                    int days = 4 + (g + k * 3) % 10;
                    DisruptionImpactParameters params = new DisruptionImpactParameters(BigDecimal.valueOf(40 + ((g * 7 + k * 13) % 55)), 86400L * days, days, BigDecimal.valueOf(2000 + 500 * g), "PIECES", Map.of());
                    // prefer a facility and material for which the twin can derive real recovery options
                    SupplyChainNode node = null;
                    Set<MaterialId> material = Set.of();
                    for (int t = 0; t < Math.min(pool.size(), 12) && node == null; t++) {
                        var cand = pool.get((g * 2 + k + t) % pool.size());
                        for (var m : mats) {
                            Disruption probe = Disruption.createDraft(wsId, "ALL", "DEMO_INCIDENT", "probe", "probe", cat, sev[(g + k) % 3], Set.of(cand.getId()), Set.of(), Set.of(m),
                                    Instant.now(), Instant.now().plus(Duration.ofDays(days)), params, "probe");
                            if (!com.supplygraph.application.service.RecoveryOptionDeriver.derive(probe, twin).isEmpty()) { node = cand; material = Set.of(m); break; }
                        }
                    }
                    if (node == null) node = pool.get((g * 2 + k) % pool.size());
                    String title = labels[g] + ": " + node.getName() + " " + why;
                    if (existing.contains(title) || title.length() > 240) continue;
                    try {
                        Disruption d = disruptionService.createDraft(wsId, "ALL", "DEMO_INCIDENT", title,
                                "Synthetic demo incident on " + node.getId().value() + " (" + node.getName() + ", " + node.getLocation() + "): " + why + "; expected to last about " + days + " days.",
                                cat, sev[(g + k) % 3], Set.of(node.getId()), Set.of(), material,
                                Instant.now().minus(Duration.ofHours(6 + g + k)), Instant.now().plus(Duration.ofDays(days)), params,
                                "Synthetic demo data.");
                        disruptionService.validateDisruption(d.id());
                        Disruption live = disruptionService.activateDisruption(d.id());
                        made++;
                        try { impactService.analyzeCascadingImpact(live.id(), wsId); } catch (RuntimeException ignored) { }
                        try { recoveryService.generateAlternativePlans(live.id(), wsId); } catch (RuntimeException ignored) { }
                    } catch (RuntimeException ex) {
                        log.warn("Demo incident '{}' skipped: {}", title, ex.getMessage());
                    }
                }
            }
            if (made > 0) log.info("Demo: {} extra live incidents added across the ten supply chains", made);
        } catch (RuntimeException ex) {
            log.warn("Non-fatal: extra incidents not added: {}", ex.getMessage());
        }
    }

    // 5c. Nine more industries beside the automotive chain, so the twin spans ten supply chains. Each is generated (real cities, invented companies and stock) and its ids carry a prefix so they never clash.
    private void addIndustryNetworks(WorkspaceId wsId) {
        if (networkImport != null && networkGenerator != null) {
            record Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry industry, String prefix, long seed) {}
            List<Extra> extras = List.of(
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.ELECTRONICS, "ELEC-", 11L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.PHARMACEUTICAL, "PHAR-", 12L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.AEROSPACE, "AERO-", 13L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.FOOD_BEVERAGE, "FOOD-", 14L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.APPAREL, "APPA-", 15L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.MEDICAL_DEVICES, "MEDI-", 16L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.SOLAR_ENERGY, "SOLR-", 17L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.CHEMICALS, "POLY-", 18L),
                    new Extra(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.HEAVY_MACHINERY, "HEAV-", 19L));
            var present = digitalTwinRepo.findByWorkspaceId(wsId).map(t -> t.getGraph().getAllNodes().stream().map(n -> n.getId().value()).toList()).orElse(List.of());
            for (Extra x : extras) {
                if (present.stream().anyMatch(id -> id.startsWith(x.prefix()))) continue;   // already added on an earlier start
                try {
                    var g = networkGenerator.generate(x.industry(), com.supplygraph.application.service.SyntheticNetworkGenerator.Region.GLOBAL,
                            com.supplygraph.application.service.SyntheticNetworkGenerator.Size.MEDIUM, x.seed(), x.prefix());
                    var r = networkImport.importCsv(wsId, g.facilities(), g.lanes(), g.stock(), g.bom(), false, true);
                    if (!r.errors().isEmpty()) log.warn("{} network was not added: {}", x.industry(), r.errors());
                    else log.info("{} network added: twin now v{}. {}", x.industry(), r.twinVersion(), g.summary());
                } catch (RuntimeException ex) {
                    log.warn("{} network skipped: {}", x.industry(), ex.getMessage());
                }
            }
        }
    }

    public synchronized void seedDemoData(boolean force) {
        WorkspaceId wsId = WorkspaceId.of(DEMO_WORKSPACE_ID);
        boolean alreadySeeded = !force && demandPlanningService != null && !demandPlanningService.listForecasts(wsId).isEmpty();
        if (alreadySeeded) {
            log.info("Demo workspace {} already fully seeded with planning and operations data", DEMO_WORKSPACE_ID);
            addIndustryNetworks(wsId);   // adds any industry networks that an earlier version of the seeder did not have
            addExtraDisruptions(wsId);
            seedIntegrationActivity(wsId);   // adds the simulated connectors and their runs when an earlier version had none
            return;
        }

        log.info("Seeding full enterprise digital twin, recovery decisions, planning, SRM, and sustainability data for {}", DEMO_WORKSPACE_ID);

        // 1. Workspace
        if (!workspaceRepo.existsById(DEMO_WORKSPACE_ID)) {
            workspaceRepo.save(new WorkspaceJpaEntity(
                    DEMO_WORKSPACE_ID,
                    "Automotive Powertrain & Electronics Global Network",
                    "AUTOMOTIVE",
                    Instant.now(),
                    Instant.now()
            ));
        }

        // 2. Canonical Digital Twin Graph (18 Multi-Tier Nodes across 6 Columns)
        // Column 0: Tier 3 Raw Materials & Mines
        Supplier mineAtacama = new Supplier(NodeId.of("MINE-ATACAMA"), "Atacama Lithium Brine Operation", "Antofagasta, CL", true, 3, Set.of("ISO14001"), 0.05);
        Supplier ingotDresden = new Supplier(NodeId.of("INGOT-DRESDEN"), "Dresden High-Purity Silicon Ingot", "Dresden, DE", true, 3, Set.of("ISO9001"), 0.08);
        Supplier miningNorilsk = new Supplier(NodeId.of("MINING-NORILSK"), "Norilsk Nickel & Cobalt Complex", "Norilsk, RU", true, 3, Set.of("ISO9001"), 0.22);

        // Column 1: Tier 2 Component & Material Fabs
        Supplier tier2Fab = new Supplier(NodeId.of("FAB-MUNICH"), "Munich Advanced Silicon Fab", "Munich, DE", true, 2, Set.of("ISO9001", "IATF16949"), 0.15);
        Supplier chemLyon = new Supplier(NodeId.of("CHEM-LYON"), "Lyon Cathode Precursor Synthesis", "Lyon, FR", true, 2, Set.of("ISO9001", "REACH"), 0.12);
        Supplier cellGothenburg = new Supplier(NodeId.of("CELL-GOTHENBURG"), "Gothenburg Battery Cell Gigafactory", "Gothenburg, SE", true, 2, Set.of("ISO9001", "IATF16949"), 0.09);
        Supplier fabHsinchu = new Supplier(NodeId.of("FAB-HSINCHU"), "Hsinchu Wafer Packaging & Test", "Hsinchu, TW", true, 2, Set.of("ISO9001", "IATF16949"), 0.11);

        // Column 2: Tier 1 Subsystems
        Supplier tier1Supplier = new Supplier(NodeId.of("SUP-KYOTO"), "Kyoto Microelectronics Corp", "Kyoto, JP", true, 1, Set.of("ISO9001", "IATF16949"), 0.10);
        Supplier harnessSibiu = new Supplier(NodeId.of("HARNESS-SIBIU"), "Sibiu High-Voltage Wiring Systems", "Sibiu, RO", true, 1, Set.of("IATF16949"), 0.07);
        Supplier moduleAustin = new Supplier(NodeId.of("MODULE-AUSTIN"), "Austin Power Electronics Inverters", "Austin, US", true, 1, Set.of("ISO9001", "IATF16949"), 0.06);
        Supplier brakesBrescia = new Supplier(NodeId.of("BRAKES-BRESCIA"), "Brescia Electro-Hydraulic Braking", "Brescia, IT", true, 1, Set.of("IATF16949"), 0.08);

        // Column 3: Logistics Hubs & Ports
        LogisticsHub hub = new LogisticsHub(NodeId.of("HUB-FRANKFURT"), "Frankfurt Global Logistics Hub", "Frankfurt, DE", true, true, 3500, Set.of("AIR", "RAIL", "ROAD"));
        LogisticsHub portRotterdam = new LogisticsHub(NodeId.of("PORT-ROTTERDAM"), "Rotterdam Maritime Deepwater Port", "Rotterdam, NL", true, true, 8000, Set.of("OCEAN", "RAIL", "ROAD", "INLAND_WATERWAY"));
        LogisticsHub hubChicago = new LogisticsHub(NodeId.of("HUB-CHICAGO"), "Chicago Midwest Rail & Freight Hub", "Chicago, US", true, true, 2800, Set.of("RAIL", "ROAD", "AIR"));

        // Column 4: Tier 0 Manufacturing Plants
        ManufacturingSite assemblyPlant = new ManufacturingSite(NodeId.of("PLANT-STUTTGART"), "Stuttgart Powertrain Assembly", "Stuttgart, DE", true, Set.of("LINE-POWERTRAIN-1", "LINE-POWERTRAIN-2"), "ISO-7", 650);
        ManufacturingSite assemblyBerlin = new ManufacturingSite(NodeId.of("ASSEMBLY-BERLIN"), "Berlin High-Voltage Integration", "Berlin, DE", true, Set.of("LINE-BATTERY-1", "LINE-PACK-2"), "ISO-8", 500);
        ManufacturingSite plantDetroit = new ManufacturingSite(NodeId.of("PLANT-DETROIT"), "Detroit Electric Truck Complex", "Detroit, US", true, Set.of("LINE-TRUCK-1"), "ISO-8", 400);

        // Column 5: Customer Delivery & Flagship OEM Terminal
        LogisticsHub oemWolfsburg = new LogisticsHub(NodeId.of("OEM-WOLFSBURG"), "Wolfsburg Flagship Assembly Terminal", "Wolfsburg, DE", true, true, 1200, Set.of("RAIL", "ROAD"));

        List<SupplyChainNode> allNodesList = List.of(
                mineAtacama, ingotDresden, miningNorilsk,
                tier2Fab, chemLyon, cellGothenburg, fabHsinchu,
                tier1Supplier, harnessSibiu, moduleAustin, brakesBrescia,
                hub, portRotterdam, hubChicago,
                assemblyPlant, assemblyBerlin, plantDetroit,
                oemWolfsburg
        );

        List<TransportationLane> allLanesList = List.of(
                new TransportationLane(EdgeId.of("L-1"), NodeId.of("MINE-ATACAMA"), NodeId.of("CHEM-LYON"), TransportMode.OCEAN, Duration.ofHours(336), BigDecimal.valueOf(0.42), true),
                new TransportationLane(EdgeId.of("L-2"), NodeId.of("MINING-NORILSK"), NodeId.of("CHEM-LYON"), TransportMode.RAIL, Duration.ofHours(72), BigDecimal.valueOf(0.68), true),
                new TransportationLane(EdgeId.of("L-3"), NodeId.of("CHEM-LYON"), NodeId.of("CELL-GOTHENBURG"), TransportMode.ROAD, Duration.ofHours(24), BigDecimal.valueOf(1.40), true),
                new TransportationLane(EdgeId.of("L-4"), NodeId.of("INGOT-DRESDEN"), NodeId.of("FAB-MUNICH"), TransportMode.ROAD, Duration.ofHours(5), BigDecimal.valueOf(0.80), true),
                new TransportationLane(EdgeId.of("LANE-MUNICH-STUTTGART"), NodeId.of("FAB-MUNICH"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(6), BigDecimal.valueOf(1.20), true),
                new TransportationLane(EdgeId.of("L-6"), NodeId.of("FAB-MUNICH"), NodeId.of("HUB-FRANKFURT"), TransportMode.RAIL, Duration.ofHours(4), BigDecimal.valueOf(0.65), true),
                new TransportationLane(EdgeId.of("L-7"), NodeId.of("FAB-HSINCHU"), NodeId.of("HUB-FRANKFURT"), TransportMode.AIR, Duration.ofHours(28), BigDecimal.valueOf(7.80), true),
                new TransportationLane(EdgeId.of("LANE-KYOTO-FRA"), NodeId.of("SUP-KYOTO"), NodeId.of("HUB-FRANKFURT"), TransportMode.AIR, Duration.ofHours(36), BigDecimal.valueOf(8.50), true),
                new TransportationLane(EdgeId.of("L-9"), NodeId.of("HARNESS-SIBIU"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(18), BigDecimal.valueOf(1.90), true),
                new TransportationLane(EdgeId.of("L-10"), NodeId.of("BRAKES-BRESCIA"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(7), BigDecimal.valueOf(0.95), true),
                new TransportationLane(EdgeId.of("L-11"), NodeId.of("CELL-GOTHENBURG"), NodeId.of("ASSEMBLY-BERLIN"), TransportMode.ROAD, Duration.ofHours(14), BigDecimal.valueOf(1.60), true),
                new TransportationLane(EdgeId.of("L-12"), NodeId.of("MODULE-AUSTIN"), NodeId.of("HUB-CHICAGO"), TransportMode.RAIL, Duration.ofHours(28), BigDecimal.valueOf(1.15), true),
                new TransportationLane(EdgeId.of("L-13"), NodeId.of("HUB-CHICAGO"), NodeId.of("PLANT-DETROIT"), TransportMode.ROAD, Duration.ofHours(6), BigDecimal.valueOf(0.70), true),
                new TransportationLane(EdgeId.of("LANE-FRA-STUTTGART"), NodeId.of("HUB-FRANKFURT"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(4), BigDecimal.valueOf(0.90), true),
                new TransportationLane(EdgeId.of("L-15"), NodeId.of("PORT-ROTTERDAM"), NodeId.of("HUB-FRANKFURT"), TransportMode.RAIL, Duration.ofHours(8), BigDecimal.valueOf(0.85), true),
                new TransportationLane(EdgeId.of("L-16"), NodeId.of("PORT-ROTTERDAM"), NodeId.of("ASSEMBLY-BERLIN"), TransportMode.ROAD, Duration.ofHours(9), BigDecimal.valueOf(1.30), true),
                new TransportationLane(EdgeId.of("L-17"), NodeId.of("PLANT-STUTTGART"), NodeId.of("OEM-WOLFSBURG"), TransportMode.RAIL, Duration.ofHours(8), BigDecimal.valueOf(1.05), true),
                new TransportationLane(EdgeId.of("L-18"), NodeId.of("ASSEMBLY-BERLIN"), NodeId.of("OEM-WOLFSBURG"), TransportMode.ROAD, Duration.ofHours(3), BigDecimal.valueOf(0.60), true)
        );

        SupplyChainGraph graph = new SupplyChainGraph(allNodesList, allLanesList);

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
        inventoryRepo.updateBalance(
                wsId,
                InventoryAccountId.of(NodeId.of("FAB-MUNICH"), MaterialId.of("MCU-AUTO-32"), BalanceCategory.ON_HAND_AVAILABLE),
                Quantity.of(BigDecimal.valueOf(4200), UnitOfMeasure.PIECE)
        );
        inventoryRepo.updateBalance(
                wsId,
                InventoryAccountId.of(NodeId.of("PORT-ROTTERDAM"), MaterialId.of("MCU-AUTO-32"), BalanceCategory.ON_HAND_AVAILABLE),
                Quantity.of(BigDecimal.valueOf(65000), UnitOfMeasure.PIECE)
        );
        inventoryRepo.updateBalance(
                wsId,
                InventoryAccountId.of(NodeId.of("CELL-GOTHENBURG"), MaterialId.of("MCU-AUTO-32"), BalanceCategory.ON_HAND_AVAILABLE),
                Quantity.of(BigDecimal.valueOf(32000), UnitOfMeasure.PIECE)
        );

        digitalTwinRepo.reload(wsId);   // balances above were written through the ledger port; rebuild the twin so stock is visible everywhere

        // 5b. Grow the sample into a large, realistic network: a generated 20-facility automotive chain (real city coordinates, estimated lanes and stock) joined to the canonical hubs
        List<String> generatedPlants = new ArrayList<>(), generatedSuppliers = new ArrayList<>(), generatedNodes = new ArrayList<>();
        if (networkImport != null && networkGenerator != null) {
            var gen = networkGenerator.generate(com.supplygraph.application.service.SyntheticNetworkGenerator.Industry.AUTOMOTIVE,
                    com.supplygraph.application.service.SyntheticNetworkGenerator.Region.GLOBAL, com.supplygraph.application.service.SyntheticNetworkGenerator.Size.LARGE, 2026L);
            StringBuilder links = new StringBuilder(gen.lanes());
            String[] anchors = {"HUB-FRANKFURT", "PORT-ROTTERDAM", "HUB-CHICAGO"};
            int hubIdx = 0;
            for (String line : gen.facilities().split("\\R")) {
                String id = line.split(",")[0];
                if (!id.equals("id")) generatedNodes.add(id);
                if (id.startsWith("HUB-")) links.append(id).append(',').append(anchors[hubIdx++ % anchors.length]).append(",RAIL,36,0.9\n");
                else if (id.startsWith("PLANT-")) generatedPlants.add(id);
                else if (id.startsWith("SUP-")) generatedSuppliers.add(id);
            }
            var merged = networkImport.importCsv(wsId, gen.facilities(), links.toString(), gen.stock(), gen.bom(), false, true);
            if (!merged.errors().isEmpty()) log.warn("Large network was not added: {}", merged.errors());
            else log.info("Large network added: twin now has v{} with generated facilities. {}", merged.twinVersion(), gen.summary());
        }

        addIndustryNetworks(wsId);

        // 6. Active Disruption Incident
        Disruption activeDisruption;
        List<Disruption> existingDisruptions = disruptionService.listDisruptions(wsId);
        if (existingDisruptions.isEmpty()) {
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
            activeDisruption = disruptionService.activateDisruption(disruption.id());

            // 7. Impact Assessment
            impactService.analyzeCascadingImpact(activeDisruption.id(), wsId);

            // 8. Generate Default Recovery Alternatives
            recoveryService.generateAlternativePlans(activeDisruption.id(), wsId);

            // 8b. Second Active Disruption (Rotterdam Maritime Port Strike)
            Disruption disruptionRotterdam = disruptionService.createDraft(
                    wsId, "AUTOMOTIVE", "LOGISTICS_PORT_CONGESTION",
                    "Rotterdam Maritime Deepwater Port Strike & Backlog",
                    "Stevedore labor dispute and terminal automation failure halting deepwater container vessel offloading and rail dispatch.",
                    DisruptionCategory.LOGISTICS_HUB_CLOSURE,
                    DisruptionSeverity.CRITICAL,
                    Set.of(NodeId.of("PORT-ROTTERDAM")),
                    Set.of(EdgeId.of("L-15")),
                    Set.of(MaterialId.of("MCU-AUTO-32")),
                    Instant.now(),
                    Instant.now().plus(Duration.ofDays(14)),
                    new DisruptionImpactParameters(
                            BigDecimal.valueOf(85),
                            86400L * 14,
                            14,
                            BigDecimal.valueOf(18000),
                            "PIECES",
                            Map.of("impactedBerths", "4")
                    ),
                    "Port of Rotterdam Authority declaration of commercial force majeure."
            );
            disruptionService.validateDisruption(disruptionRotterdam.id());
            Disruption activeDisruptionRotterdam = disruptionService.activateDisruption(disruptionRotterdam.id());
            impactService.analyzeCascadingImpact(activeDisruptionRotterdam.id(), wsId);
            recoveryService.generateAlternativePlans(activeDisruptionRotterdam.id(), wsId);

        } else {
            activeDisruption = existingDisruptions.get(0);
        }

        // 8c. Two more incidents on the generated part of the network, so the large map has several live situations
        boolean extraIncidentsExist = disruptionService.listDisruptions(wsId).stream().anyMatch(x -> x.title().equals("Assembly plant paint-shop fire"));
        if (!extraIncidentsExist && !generatedPlants.isEmpty() && !generatedSuppliers.isEmpty()) {
            Disruption plantFire = disruptionService.createDraft(wsId, "AUTOMOTIVE", "PLANT_FIRE", "Assembly plant paint-shop fire",
                    "A fire in the paint shop has stopped one assembly plant; inspection and repairs are expected to take about ten days.",
                    DisruptionCategory.MANUFACTURING_SITE_OUTAGE, DisruptionSeverity.HIGH, Set.of(NodeId.of(generatedPlants.get(0))), Set.of(), Set.of(MaterialId.of("BATTERY-CELL")),
                    Instant.now(), Instant.now().plus(Duration.ofDays(10)),
                    new DisruptionImpactParameters(BigDecimal.valueOf(100), 86400L * 10, 10, BigDecimal.valueOf(9000), "PIECES", Map.of()), "Site safety officer report.");
            disruptionService.validateDisruption(plantFire.id());
            Disruption livePlantFire = disruptionService.activateDisruption(plantFire.id());
            impactService.analyzeCascadingImpact(livePlantFire.id(), wsId);
            try {
                recoveryService.generateAlternativePlans(livePlantFire.id(), wsId);
            } catch (com.supplygraph.domain.error.BusinessRuleException ex) {
                log.info("No recovery option can be derived for '{}' yet: {}", livePlantFire.title(), ex.getMessage());   // honest: the incident is shown without invented plans
            }

            Disruption supplierStrike = disruptionService.createDraft(wsId, "AUTOMOTIVE", "SUPPLIER_STRIKE", "Component supplier labour strike",
                    "A strike at a first-tier component supplier has halted shipments of wiring and cells.",
                    DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.MEDIUM, Set.of(NodeId.of(generatedSuppliers.get(0))), Set.of(), Set.of(MaterialId.of("WIRING-HARNESS")),
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)),
                    new DisruptionImpactParameters(BigDecimal.valueOf(70), 86400L * 7, 7, BigDecimal.valueOf(4000), "PIECES", Map.of()), "Union notice of industrial action.");
            disruptionService.validateDisruption(supplierStrike.id());
            Disruption liveStrike = disruptionService.activateDisruption(supplierStrike.id());
            impactService.analyzeCascadingImpact(liveStrike.id(), wsId);
            try {
                recoveryService.generateAlternativePlans(liveStrike.id(), wsId);
            } catch (com.supplygraph.domain.error.BusinessRuleException ex) {
                log.info("No recovery option can be derived for '{}' yet: {}", liveStrike.title(), ex.getMessage());   // honest: the incident is shown without invented plans
            }
        }

        addExtraDisruptions(wsId);

        // 9. Seed Default Continuous Monitoring Rules
        try {
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
        } catch (Exception ex) {
            log.warn("Non-fatal: Monitoring rule seeding notice: {}", ex.getMessage());
        }

        // 10. Ingest Initial Seed Telemetry Events
        try {
            OperationalEvent seedEvent1 = OperationalEvent.createIngested(
                    EventType.SUPPLIER_DELIVERY_DELAYED,
                    EventSource.EXTERNAL_INTEGRATION,
                    Instant.now().minus(Duration.ofHours(2)),
                    "FAB-MUNICH",
                    "NODE",
                    wsId,
                    Map.of("delayHours", "36", "carrier", "DHL_FREIGHT", "route", "LANE-MUNICH-STUTTGART"),
                    EventProvenance.imported("ERP_SAP", "BATCH-SEED-01", "system_telemetry"),
                    "SEED-EVT-" + UUID.randomUUID().toString().substring(0, 8)
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
                    "SEED-EVT-" + UUID.randomUUID().toString().substring(0, 8)
            );
            eventIngestionService.ingestEvent(seedEvent2);
        } catch (Exception ex) {
            log.warn("Non-fatal: Telemetry event seeding notice: {}", ex.getMessage());
        }

        // 10b. Two weeks of generated operational history across the large network, so monitoring, trends and lists have realistic volume. Labelled as generated in its provenance.
        if (!generatedNodes.isEmpty()) {
            java.util.Random rnd = new java.util.Random(7);
            EventType[] kinds = {EventType.SUPPLIER_DELIVERY_DELAYED, EventType.SHIPMENT_STATUS_CHANGED, EventType.PRODUCTION_CAPACITY_CHANGED, EventType.DEMAND_CHANGED};
            int written = 0;
            for (int i = 0; i < 150; i++) {
                EventType kind = kinds[rnd.nextInt(kinds.length)];
                String node = generatedNodes.get(rnd.nextInt(generatedNodes.size()));
                Map<String, String> data = switch (kind) {
                    case SUPPLIER_DELIVERY_DELAYED -> Map.of("delayHours", String.valueOf(2 + rnd.nextInt(40)), "carrier", "GENERATED");
                    case SHIPMENT_STATUS_CHANGED -> Map.of("status", new String[]{"DEPARTED", "IN_TRANSIT", "AT_CUSTOMS", "ARRIVED"}[rnd.nextInt(4)]);
                    case PRODUCTION_CAPACITY_CHANGED -> Map.of("capacityPercent", String.valueOf(60 + rnd.nextInt(41)));
                    default -> Map.of("changePercent", String.valueOf(-15 + rnd.nextInt(31)));
                };
                try {
                    eventIngestionService.ingestEvent(OperationalEvent.createIngested(kind, EventSource.EXTERNAL_INTEGRATION, Instant.now().minus(Duration.ofMinutes(30L + rnd.nextInt(14 * 24 * 60))),
                            node, "NODE", wsId, data, EventProvenance.imported("GENERATED_DEMO_HISTORY", "HIST-" + i, "synthetic"), "SEED-HIST-" + i));
                    written++;
                } catch (RuntimeException ex) {
                    log.debug("Generated history event {} skipped: {}", i, ex.getMessage());
                }
            }
            log.info("Generated operational history: {} telemetry events over the last 14 days", written);
        }

        // 11. Seed Enterprise Users (RBAC): each user on its own, so one that already exists never blocks the others
        ensureUser("USR-ADMIN-01", "admin", "admin@supplygraph.io", "Enterprise System Administrator", UserRole.SYSTEM_ADMIN, wsId);
        ensureUser("USR-SCM-01", "sc_manager", "scm@supplygraph.io", "Lead Supply Chain Manager", UserRole.SUPPLY_CHAIN_MANAGER, wsId);
        ensureUser("USR-ANALYST-01", "analyst", "analyst@supplygraph.io", "Senior Operations Analyst", UserRole.ANALYST, wsId);

        // 12. Seed Governance Audit Sign-Off
        try {
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
        } catch (Exception ex) {
            log.warn("Non-fatal: Audit log seeding notice: {}", ex.getMessage());
        }

        // 13. Seed Data Provenance Records
        try {
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
        } catch (Exception ex) {
            log.warn("Non-fatal: Provenance seeding notice: {}", ex.getMessage());
        }

        // 14. Seed Initial Research Benchmark Trial
        try {
            benchmarkRunner.runTrial(
                    "Baseline Comparative Trial on Munich Fab Outage",
                    BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                    BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING,
                    42L,
                    3
            );
        } catch (Exception ex) {
            log.warn("Non-fatal: Benchmark trial seeding notice: {}", ex.getMessage());
        }

        // 15. Seed Phase 7 Enterprise Integration, Quality & Intelligence
        try {
            String initialSupplierCsv = """
                    supplier_id,name,location,lead_time
                    FAB-MUNICH,Munich Semiconductor Fab,DEU,60
                    CELL-GOTHENBURG,Gothenburg Battery Cell Gigafactory,SWE,45
                    CHEM-LYON,Lyon Specialty Chemical Precursors,FRA,30
                    HARNESS-SIBIU,Sibiu Automotive Wiring Systems,ROU,14
                    """;
            var job = importService.parseAndValidate(
                    initialSupplierCsv.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    com.supplygraph.domain.model.integration.ImportSourceType.CSV,
                    com.supplygraph.domain.model.integration.DataCategory.SUPPLIER,
                    "initial_supplier_master_2026.csv",
                    "WS-AUTO-01",
                    "USR-SCM-01"
            );
            importService.executeImport(job.id(), "WS-AUTO-01", "USR-SCM-01");

            // Also seed for DEMO_WORKSPACE_ID
            qualityService.evaluateWorkspaceQuality(DEMO_WORKSPACE_ID, twinState);
            supplierService.computeSupplierRiskProfiles(DEMO_WORKSPACE_ID, twinState);
            materialService.computeMaterialProfiles(DEMO_WORKSPACE_ID, twinState, BigDecimal.valueOf(30));
            resilienceService.assessNetworkResilience(DEMO_WORKSPACE_ID, twinState);

            // 16. Seed Phase 8 Explainable Recommendations, Provenance Graph, What-If, Pareto, Sensitivity & Advanced Resilience
            // The recommendation describes a plan that was really derived from the twin: nothing is invented for the sample.
            var candidateAlt = recoveryService.listPlansByDisruption(activeDisruption.id()).stream().findFirst().map(realPlan -> {
                var m = realPlan.metrics();
                return new com.supplygraph.domain.model.recovery.RecoveryPlanAlternative(
                        com.supplygraph.domain.model.recovery.PlanAlternativeId.of("ALT-" + realPlan.id().value()),
                        realPlan.planName(), realPlan.description(), m.totalCost(), m.recoveryDurationDays() * 24.0, m.serviceLevelImpactPercentage(),
                        realPlan.actions().stream().map(com.supplygraph.domain.model.plan.PlanAction::description).toList());
            }).orElse(null);
            if (candidateAlt != null) {
                explainableRecommendationService.generateRecommendation(activeDisruption.id().value(), "V" + recoveryService.listPlansByDisruption(activeDisruption.id()).get(0).sourceTwinVersion().value(), candidateAlt);
            }

            decisionProvenanceGraphService.buildAndSaveProvenanceGraph(DEMO_WORKSPACE_ID, activeDisruption.id().value(), "USR-SCM-01");

            var whatIfConfig = com.supplygraph.domain.model.whatif.WhatIfScenarioConfig.of(
                    com.supplygraph.domain.model.whatif.WhatIfScenarioId.of("WIF-AIR-01"),
                    "Air Freight Surge + Safety Stock Multiplier",
                    "Counterfactual simulation testing air freight expediting with 1.5x buffer capacity",
                    "WS-AUTO-01",
                    0.85,
                    1.50,
                    -3,
                    "AIR",
                    14,
                    0.5,
                    0.3,
                    0.2
            );
            whatIfAnalysisService.executeScenario(whatIfConfig);

            if (candidateAlt != null) {
                multiObjectiveParetoService.evaluateAlternatives(activeDisruption.id().value(), List.of(candidateAlt));
                try {
                    sensitivityAnalysisService.runSensitivitySuite(DEMO_WORKSPACE_ID, candidateAlt.id().value(), candidateAlt.expectedCost(), candidateAlt.timeToRecoverHours() / 24.0, candidateAlt.serviceLevelRestoration());
                } catch (com.supplygraph.domain.error.ValidationException ex) {
                    log.info("Sensitivity suite not run for the sample plan: {}", ex.getMessage());
                }
            }
            advancedResilienceAnalyticsService.evaluateWorkspaceResilience(DEMO_WORKSPACE_ID);
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 7/8 demo data seeding notice: {}", ex.getMessage());
        }

        // 17. Seed Phase 9 Governed Execution Workflows, Tasks & Approvals
        try {
            if (executionWorkflowRepo != null) {
                String wfId = "WF-REC-MUNICH-FLOOD-01";
                ExecutionTask t1 = ExecutionTask.create(wfId, 1, TaskType.CHANGE_TRANSPORT_MODE,
                        "Charter B77F Air Cargo flight MUC to STR (Lufthansa Cargo LH8420)",
                        Map.of("carrier", "LUFTHANSA_CARGO", "flight", "LH8420", "corridor", "MUNICH-STUTTGART"),
                        List.of(), Map.of("status", "CONFIRMED"), 3).markReady().startAttempt().complete("AWB-9982-LH-CARGO", false);

                ExecutionTask t2 = ExecutionTask.create(wfId, 2, TaskType.ALLOCATE_INVENTORY,
                        "Buffer release 1,500 MCU units from Frankfurt Distribution Hub",
                        Map.of("facility", "HUB-FRANKFURT", "material", "MCU-AUTO-32", "quantity", "1500"),
                        List.of(t1.id()), Map.of("balanceCategory", "ALLOCATED"), 3).markReady().startAttempt();

                ExecutionTask t3 = ExecutionTask.create(wfId, 3, TaskType.EXPEDITE_PURCHASE_ORDER,
                        "Transmit SAP ERP Purchase Order emergency delivery amendment",
                        Map.of("poNumber", "PO-2026-001", "revisedDate", "2026-11-20", "system", "SAP_S4HANA"),
                        List.of(t2.id()), Map.of("status", "AMENDED_CONFIRMED"), 3).markReady();

                ExecutionTask t4 = ExecutionTask.create(wfId, 4, TaskType.CHANGE_PRODUCTION_SEQUENCE,
                        "Reschedule Stuttgart Assembly Shift 2 sequence in MES",
                        Map.of("plantId", "PLANT-STUTTGART", "lineId", "LINE-POWERTRAIN-1"),
                        List.of(t3.id()), Map.of("scheduleStatus", "UPDATED"), 3);

                // The sample workflow must reference a plan that really exists, otherwise plan-versus-actual views cannot load it.
                String seededPlanId = recoveryService.listPlansByDisruption(activeDisruption.id()).stream().findFirst()
                        .map(pl -> pl.id().value()).orElse("PLAN-" + activeDisruption.id().value());
                ExecutionWorkflow wf = ExecutionWorkflow.draft(
                        wsId, seededPlanId, "USR-SCM-01", "USR-ADMIN-01", "v1",
                        List.of(t1, t2, t3, t4), "CORR-MUNICH-RECOVERY-01", 0
                );
                WorkflowApproval app1 = new WorkflowApproval(wfId, "USR-SCM-01", "SUPPLY_CHAIN_MANAGER",
                        WorkflowApproval.Scope.WORKFLOW, true, "Approved emergency air charter and buffer draw", Instant.now());
                WorkflowApproval app2 = new WorkflowApproval(wfId, "USR-ADMIN-01", "SYSTEM_ADMIN",
                        WorkflowApproval.Scope.SENSITIVE_TASKS, true, "Approved expedited financial allocation within policy", Instant.now());
                ExecutionWorkflow approvedWf = wf.validate()
                        .submitForApproval()
                        .addApproval(app1)
                        .addApproval(app2)
                        .markApproved(true)
                        .markReady(false)
                        .markExecuting();
                try {
                    executionWorkflowRepo.save(approvedWf);
                    log.info("Phase 9 Demo: Seeded Governed Execution Workflow {} with 4 tasks and dual approvals", wfId);
                } catch (RuntimeException already) {
                    log.info("Phase 9 Demo: workflow {} already exists and is kept as it is ({})", wfId, already.getMessage());
                }

                // A second workflow that is still waiting for approval, so the approvals queue has something real to act on.
                var otherPlan = recoveryService.listAllPlans(wsId).stream().filter(pl -> !pl.id().value().equals(seededPlanId)).findFirst();
                if (otherPlan.isPresent()) {
                    String pendingId = "WF-REC-PENDING-01";
                    ExecutionTask p1 = ExecutionTask.create(pendingId, 1, TaskType.ALLOCATE_INVENTORY,
                            "Release buffer stock from the best-placed hub to the affected plant",
                            Map.of("facility", "HUB-FRANKFURT", "material", "MCU-AUTO-32", "quantity", "800"),
                            List.of(), Map.of("balanceCategory", "ALLOCATED"), 3);
                    ExecutionTask p2 = ExecutionTask.create(pendingId, 2, TaskType.EXPEDITE_PURCHASE_ORDER,
                            "Ask the alternative supplier to expedite the open purchase order",
                            Map.of("poNumber", "PO-2026-002", "system", "SAP_S4HANA"),
                            List.of(p1.id()), Map.of("status", "AMENDED_CONFIRMED"), 3);
                    ExecutionWorkflow pending = ExecutionWorkflow.draft(wsId, otherPlan.get().id().value(), "USR-SCM-01", "USR-ADMIN-01", "v1",
                            List.of(p1, p2), "CORR-PENDING-RECOVERY-01", 0).validate().submitForApproval();
                    executionWorkflowRepo.save(pending);
                    log.info("Phase 9 Demo: Seeded workflow {} waiting for approval", pendingId);
                }
            }
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 9 demo execution seeding notice: {}", ex.getMessage());
        }

        // 18. Seed Phase 10 Enterprise Connectors (simulated), their synchronisation runs
        seedIntegrationActivity(wsId);

        // 19. Seed Phase 11 Supply Chain Planning
        try {
            if (demandPlanningService != null) {
                List<HistoricalDemandPoint> ecuHistory = List.of(
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(6), 1400.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(5), 1450.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(4), 1520.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(3), 1580.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(2), 1610.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 1650.0)
                );
                demandPlanningService.generateForecast(
                        wsId, MaterialId.of("ECU-POWERTRAIN-V8"), NodeId.of("PLANT-STUTTGART"),
                        ForecastMethod.TREND_ADJUSTED_HOLT_WINTERS, ecuHistory, 6, 0.3, 0.1
                );

                List<HistoricalDemandPoint> mcuHistory = List.of(
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(6), 3800.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(5), 4000.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(4), 4150.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(3), 4300.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(2), 4450.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 4600.0)
                );
                demandPlanningService.generateForecast(
                        wsId, MaterialId.of("MCU-AUTO-32"), NodeId.of("HUB-FRANKFURT"),
                        ForecastMethod.WEIGHTED_MOVING_AVERAGE, mcuHistory, 6, 0.25, 0.0
                );
            }

            if (multiEchelonInventoryService != null) {
                EchelonInventoryPosition pos1 = new EchelonInventoryPosition(
                        NodeId.of("PLANT-STUTTGART"), EchelonTier.PLANT_WAREHOUSE, MaterialId.of("MCU-AUTO-32"),
                        4500.0, 1500.0, 2400.0, 5200.0, 8000.0, 2.50
                );
                EchelonInventoryPosition pos2 = new EchelonInventoryPosition(
                        NodeId.of("HUB-FRANKFURT"), EchelonTier.REGIONAL_HUB, MaterialId.of("MCU-AUTO-32"),
                        1500.0, 5000.0, 1200.0, 3000.0, 6000.0, 1.80
                );
                multiEchelonInventoryService.generateInventoryPlan(
                        wsId, "Automotive Microcontroller Multi-Echelon Buffer Plan",
                        ServiceLevel.SL_99, List.of(pos1, pos2)
                );
            }

            if (procurementPlanningService != null) {
                NetRequirementItem req1 = new NetRequirementItem(
                        MaterialId.of("MCU-AUTO-32"), NodeId.of("PLANT-STUTTGART"),
                        10000.0, 4500.0, 1500.0, 4000.0, LocalDate.now().plusDays(14)
                );
                SupplierAllocationShare alloc1 = new SupplierAllocationShare(
                        NodeId.of("FAB-MUNICH"), MaterialId.of("MCU-AUTO-32"),
                        65.0, 2600.0, BigDecimal.valueOf(52.00), BigDecimal.valueOf(135200.00)
                );
                SupplierAllocationShare alloc2 = new SupplierAllocationShare(
                        NodeId.of("FAB-HSINCHU"), MaterialId.of("MCU-AUTO-32"),
                        35.0, 1400.0, BigDecimal.valueOf(54.00), BigDecimal.valueOf(75600.00)
                );
                procurementPlanningService.generateProcurementPlan(
                        wsId, "Q3 Strategic Semiconductor Dual-Sourcing Allocation",
                        List.of(req1), List.of(alloc1, alloc2)
                );
            }

            if (productionPlanningService != null) {
                ScheduledProductionJob job1 = new ScheduledProductionJob(
                        "JOB-POWERTRAIN-01", MaterialId.of("ECU-POWERTRAIN-V8"), NodeId.of("PLANT-STUTTGART"),
                        LocalDate.now(), 1, 350.0, 1.0, 6.5, 1
                );
                ScheduledProductionJob job2 = new ScheduledProductionJob(
                        "JOB-POWERTRAIN-02", MaterialId.of("ECU-POWERTRAIN-V8"), NodeId.of("PLANT-STUTTGART"),
                        LocalDate.now(), 2, 300.0, 0.5, 6.0, 2
                );
                productionPlanningService.generateFiniteSchedule(
                        wsId, NodeId.of("PLANT-STUTTGART"), "Stuttgart Main Powertrain Assembly Schedule",
                        List.of(job1, job2), 16.0
                );
            }

            if (planningScenarioService != null) {
                planningScenarioService.createScenario(
                        wsId, "Base Operating Plan Q3", PlanningScenarioType.BASE_PLAN,
                        BigDecimal.valueOf(4200000.00), 0.982, 8.5, 420.0, "USR-SCM-01"
                );
                planningScenarioService.createScenario(
                        wsId, "Resilience Dual-Sourcing Air Expedited Plan", PlanningScenarioType.ACCELERATED_DELIVERY,
                        BigDecimal.valueOf(4850000.00), 0.998, 5.2, 490.0, "USR-SCM-01"
                );
                planningScenarioService.createScenario(
                        wsId, "Low-Carbon Rail Modal Shift", PlanningScenarioType.SUPPLY_RESTRICTION,
                        BigDecimal.valueOf(4350000.00), 0.975, 9.8, 210.0, "USR-SCM-01"
                );
            }
            log.info("Phase 11 Demo: Seeded SC Planning (Demand Forecasts, Inventory Plans, Procurement Splits, Production Schedules, Scenarios)");
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 11 planning demo seeding notice: {}", ex.getMessage());
        }

        // 20. Seed Phase 12 Supplier Portal & SRM Collaboration
        try {
            if (supplierPortalService != null) {
                var p1 = supplierPortalService.onboardPartner(wsId, "Kyoto Microelectronics Corp", PartnerType.SUPPLIER, PartnerTier.TIER_1_DIRECT, "65-432-1098", "JP");
                var p2 = supplierPortalService.onboardPartner(wsId, "Munich Advanced Silicon Fab", PartnerType.SUPPLIER, PartnerTier.TIER_2_SUBTIER, "31-890-4521", "DE");
                var p3 = supplierPortalService.onboardPartner(wsId, "Lyon Cathode Precursor Synthesis", PartnerType.SUPPLIER, PartnerTier.TIER_2_SUBTIER, "42-109-8765", "FR");
                var p4 = supplierPortalService.onboardPartner(wsId, "Sibiu High-Voltage Wiring Systems", PartnerType.SUPPLIER, PartnerTier.TIER_1_DIRECT, "54-321-6789", "RO");
                var p5 = supplierPortalService.onboardPartner(wsId, "Brescia Electro-Hydraulic Braking", PartnerType.SUPPLIER, PartnerTier.TIER_1_DIRECT, "23-456-7890", "IT");

                if (orderCollaborationService != null) {
                    var po1 = orderCollaborationService.issuePo(wsId, "PO-2026-001", p1.id(), MaterialId.of("MCU-AUTO-32"), 4500.0, BigDecimal.valueOf(247500.00), LocalDate.now().plusDays(20));
                    orderCollaborationService.acknowledgePo(wsId, po1.id(), LocalDate.now().plusDays(20));

                    var po2 = orderCollaborationService.issuePo(wsId, "PO-2026-002", p2.id(), MaterialId.of("MCU-AUTO-32"), 10000.0, BigDecimal.valueOf(520000.00), LocalDate.now().plusDays(30));

                    var po3 = orderCollaborationService.issuePo(wsId, "PO-2026-003", p3.id(), MaterialId.of("CHEM-LYON"), 25000.0, BigDecimal.valueOf(680000.00), LocalDate.now().plusDays(15));
                    orderCollaborationService.dispatchAsn(wsId, po3.id(), "ASN-2026-FR-09", "CMA_CGM_CARRIER", 25000.0, LocalDate.now().plusDays(14));
                }

                if (rfqCollaborationService != null) {
                    var rfq = rfqCollaborationService.createRfq(wsId, "Emergency Wafer Sourcing Q3 - 32-Bit Microcontrollers", MaterialId.of("MCU-AUTO-32"), 5000.0, LocalDate.now().plusDays(10));
                    rfqCollaborationService.submitQuotation(wsId, rfq.id(), p1.id(), BigDecimal.valueOf(58.00), 14, 2000.0, LocalDate.now().plusDays(30));
                    rfqCollaborationService.submitQuotation(wsId, rfq.id(), p2.id(), BigDecimal.valueOf(52.50), 21, 2500.0, LocalDate.now().plusDays(30));
                }

                if (supplierQualityCollaborationService != null) {
                    var q1 = supplierQualityCollaborationService.openIncident(wsId, p2.id(), MaterialId.of("MCU-AUTO-32"), "Micro-void delamination in silicon photolithography packaging", IncidentSeverity.CRITICAL);
                    supplierQualityCollaborationService.advanceStep(wsId, q1.id(), EightDStep.D2_PROBLEM_DESCRIPTION);
                    supplierQualityCollaborationService.advanceStep(wsId, q1.id(), EightDStep.D3_CONTAINMENT_ACTIONS);
                    supplierQualityCollaborationService.advanceStep(wsId, q1.id(), EightDStep.D4_ROOT_CAUSE_ANALYSIS);

                    var q2 = supplierQualityCollaborationService.openIncident(wsId, p4.id(), MaterialId.of("HARNESS-SIBIU"), "High-voltage cable sheath abrasion tolerance deviation", IncidentSeverity.MEDIUM);
                    supplierQualityCollaborationService.advanceStep(wsId, q2.id(), EightDStep.D2_PROBLEM_DESCRIPTION);
                }

                if (partnerDocumentExchangeService != null) {
                    partnerDocumentExchangeService.uploadDocument(wsId, p1.id(), "IATF-16949-Kyoto-Certification-2026.pdf", DocumentCategory.QUALITY_CERTIFICATE, "sha256-8f4b2390a12e");
                    partnerDocumentExchangeService.uploadDocument(wsId, p2.id(), "ISO-14001-Munich-Environmental.pdf", DocumentCategory.QUALITY_CERTIFICATE, "sha256-5e1a90bc4432");
                    partnerDocumentExchangeService.uploadDocument(wsId, p3.id(), "REACH-RoHS-Declaration-Lyon.pdf", DocumentCategory.ESG_EVIDENCE, "sha256-11da90ee9821");
                }
                log.info("Phase 12 Demo: Seeded Supplier Portal & SRM (Partners, Purchase Orders, ASNs, RFQs, 8D Quality Reports, Documents)");
            }
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 12 SRM demo seeding notice: {}", ex.getMessage());
        }

        // 21. Seed Phase 13 Sustainability, ESG & Traceability
        try {
            if (carbonAccountingService != null) {
                carbonAccountingService.recordScope1Combustion(wsId, NodeId.of("FAB-MUNICH"), "NATURAL_GAS", 18500.0, "m³", 2.03);
                carbonAccountingService.recordScope1Combustion(wsId, NodeId.of("PLANT-STUTTGART"), "DIESEL_GENERATOR", 4200.0, "liters", 2.68);
                carbonAccountingService.recordScope2Electricity(wsId, NodeId.of("ASSEMBLY-BERLIN"), 125000.0, 0.38);
                carbonAccountingService.recordScope2Electricity(wsId, NodeId.of("PLANT-STUTTGART"), 98000.0, 0.38);
                carbonAccountingService.recordScope3Freight(wsId, NodeId.of("LANE-MUNICH-STUTTGART"), com.supplygraph.domain.model.planning.TransportMode.ROAD, 220.0, 45.0);
                carbonAccountingService.recordScope3Freight(wsId, NodeId.of("L-17"), com.supplygraph.domain.model.planning.TransportMode.RAIL, 650.0, 180.0);
                carbonAccountingService.recordScope3Freight(wsId, NodeId.of("L-1"), com.supplygraph.domain.model.planning.TransportMode.OCEAN, 11500.0, 850.0);
            }

            if (productTraceabilityService != null) {
                TraceabilityNode n1 = new TraceabilityNode(new TraceabilityNodeId("TR-01"), "LOT-ATACAMA-901", TraceabilityNodeType.RAW_MATERIAL_ORIGIN, MaterialId.of("MINE-ATACAMA"), NodeId.of("MINE-ATACAMA"), 15000.0, "KG", "SQM Lithium Operations", Instant.now().minus(Duration.ofDays(75)));
                TraceabilityNode n2 = new TraceabilityNode(new TraceabilityNodeId("TR-02"), "BATCH-LYON-402", TraceabilityNodeType.SUPPLIER_BATCH, MaterialId.of("CHEM-LYON"), NodeId.of("CHEM-LYON"), 14200.0, "KG", "Lyon Chemical Synthesis", Instant.now().minus(Duration.ofDays(45)));
                TraceabilityNode n3 = new TraceabilityNode(new TraceabilityNodeId("TR-03"), "CELL-LOT-GOTHENBURG-110", TraceabilityNodeType.WORK_ORDER_BATCH, MaterialId.of("CELL-GOTHENBURG"), NodeId.of("CELL-GOTHENBURG"), 12000.0, "CELLS", "Northvolt Gigafactory", Instant.now().minus(Duration.ofDays(25)));
                TraceabilityNode n4 = new TraceabilityNode(new TraceabilityNodeId("TR-04"), "PACK-MODULE-BERLIN-88", TraceabilityNodeType.WORK_ORDER_BATCH, MaterialId.of("ASSEMBLY-BERLIN"), NodeId.of("ASSEMBLY-BERLIN"), 400.0, "MODULES", "Berlin Integration Plant", Instant.now().minus(Duration.ofDays(10)));
                TraceabilityNode n5 = new TraceabilityNode(new TraceabilityNodeId("TR-05"), "ECU-POWERTRAIN-STUTTGART-V8", TraceabilityNodeType.FINISHED_PRODUCT_LOT, MaterialId.of("ECU-POWERTRAIN-V8"), NodeId.of("PLANT-STUTTGART"), 400.0, "UNITS", "Stuttgart Assembly Line 1", Instant.now().minus(Duration.ofDays(4)));
                TraceabilityNode n6 = new TraceabilityNode(new TraceabilityNodeId("TR-06"), "VIN-WBA-EV-2026-8891", TraceabilityNodeType.SHIPMENT_CONSIGNMENT, MaterialId.of("OEM-WOLFSBURG"), NodeId.of("OEM-WOLFSBURG"), 1.0, "VEHICLE", "Wolfsburg Flagship OEM", Instant.now().minus(Duration.ofDays(1)));

                productTraceabilityService.registerNode(wsId, n1);
                productTraceabilityService.registerNode(wsId, n2);
                productTraceabilityService.registerNode(wsId, n3);
                productTraceabilityService.registerNode(wsId, n4);
                productTraceabilityService.registerNode(wsId, n5);
                productTraceabilityService.registerNode(wsId, n6);

                productTraceabilityService.linkNodes(wsId, n1.id(), n2.id(), 14200.0, "Purified lithium carbonate precursor conversion");
                productTraceabilityService.linkNodes(wsId, n2.id(), n3.id(), 12000.0, "Cathode slurry coating & pouch cell fabrication");
                productTraceabilityService.linkNodes(wsId, n3.id(), n4.id(), 400.0, "High-voltage battery module pack integration");
                productTraceabilityService.linkNodes(wsId, n4.id(), n5.id(), 400.0, "ECU powertrain control harness pairing");
                productTraceabilityService.linkNodes(wsId, n5.id(), n6.id(), 1.0, "Final chassis mating & end-of-line dyno inspection");
            }

            if (circularEconomyService != null) {
                circularEconomyService.recordRecovery(wsId, MaterialId.of("CELL-4680"), CircularFlowType.BATTERY_CELL_RECYCLING, 12500.0, 91.5);
                circularEconomyService.recordRecovery(wsId, MaterialId.of("MCU-AUTO-32"), CircularFlowType.VIRGIN_MATERIAL_AVOIDANCE, 3200.0, 84.0);
                circularEconomyService.recordRecovery(wsId, MaterialId.of("ECU-POWERTRAIN-V8"), CircularFlowType.REMANUFACTURING, 850.0, 96.0);
            }

            if (environmentalScenarioService != null) {
                environmentalScenarioService.simulateScenario(wsId, "Net-Zero 2030 Science-Based Target Pathway", 85000.0, 24000.0, BigDecimal.valueOf(1280000.00));
                environmentalScenarioService.simulateScenario(wsId, "Modal Freight Shift (Road to Electric Rail)", 85000.0, 56000.0, BigDecimal.valueOf(420000.00));
            }

            if (supplierSustainabilityService != null) {
                supplierSustainabilityService.recordEsgAudit(wsId, NodeId.of("FAB-MUNICH"), 88.5, 92.0, 95.0, true, true, true);
                supplierSustainabilityService.recordEsgAudit(wsId, NodeId.of("MINE-ATACAMA"), 78.0, 84.0, 88.0, true, true, true);
                supplierSustainabilityService.recordEsgAudit(wsId, NodeId.of("CELL-GOTHENBURG"), 96.0, 94.0, 98.0, true, true, true);
            }
            log.info("Phase 13 Demo: Seeded Sustainability & ESG (Scope 1-3 Carbon, Cradle-to-Grave Traceability DAG, Circular Ledger, Environmental Scenarios)");
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 13 sustainability demo seeding notice: {}", ex.getMessage());
        }

        // 22. Seed Phase 14-15 Platform Administration, Quotas, Policies, API Keys & Backups
        try {
            if (workspaceAdminService != null) {
                workspaceAdminService.configureQuota(wsId, 2500, 100, 50);
            }

            if (industryPolicyService != null) {
                String autoPolicyJson = """
                        {
                          "standard": "IATF-16949-V2",
                          "safetyStockMinimumDays": 14,
                          "criticalComponentsDualSourced": true,
                          "bufferStockAllocationPolicy": "PRIORITY_OEM_FIRST",
                          "qualityEscalationThresholdHours": 4
                        }
                        """;
                var draft = industryPolicyService.createDraftPolicy(wsId, "AUTOMOTIVE", autoPolicyJson);
                var validated = industryPolicyService.validatePolicy(draft.id());
                industryPolicyService.activatePolicy(validated.id());

                String aeroPolicyJson = """
                        {
                          "standard": "AS9100-D",
                          "lotGenealogyMandatory": true,
                          "quarantineDefectRateThresholdPpm": 50,
                          "airworthinessCertificateRequired": true
                        }
                        """;
                var draftAero = industryPolicyService.createDraftPolicy(wsId, "AEROSPACE", aeroPolicyJson);
                industryPolicyService.validatePolicy(draftAero.id());
            }

            if (apiDeveloperCentreService != null) {
                apiDeveloperCentreService.generateApiKey(wsId, "Production ERP Telemetry Integration Key", List.of(ApiScope.READ_TWIN, ApiScope.MUTATE_PLAN));
                apiDeveloperCentreService.generateApiKey(wsId, "Executive Analytics & BI Feed Key", List.of(ApiScope.READ_TWIN, ApiScope.EXECUTE_SIMULATION));
            }

            if (backupDisasterRecoveryService != null) {
                var snap = backupDisasterRecoveryService.createBackupSnapshot(wsId, "BACKUP-AUTO-2026-11-18-DAILY-PROD");
                backupDisasterRecoveryService.verifySnapshot(snap.id());
            }
            log.info("Phase 14/15 Demo: Seeded Platform Administration (Quotas, Industry Policies, API Keys, Backup Disaster Recovery Snapshots)");
        } catch (Exception ex) {
            log.warn("Non-fatal: Phase 14/15 platform admin demo seeding notice: {}", ex.getMessage());
        }

        log.info("Demo initialization complete: Active disruption {} with cascading impact, recovery alternatives, monitoring rules, enterprise users, audit trail, research benchmark, and decision intelligence.",
                activeDisruption.id().value());
    }
}
