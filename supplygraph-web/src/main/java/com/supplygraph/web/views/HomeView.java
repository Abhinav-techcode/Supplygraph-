package com.supplygraph.web.views;

import com.supplygraph.application.service.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.web.components.DigitalTwinNetworkGraphComponent;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SupplyGraph Executive Control Tower & Overview Dashboard.
 * Provides high-level visibility across the digital twin, active disruptions,
 * simulation models, mathematical optimization runs, and governed execution workflows.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Executive Control Tower")
public class HomeView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final ImpactAnalysisApplicationService impactService;
    private final SimulationApplicationService simulationService;
    private final MonteCarloApplicationService monteCarloService;
    private final OptimizationApplicationService optimizationService;
    private final RecoveryPlanningApplicationService recoveryService;
    private final com.supplygraph.application.graph.NetworkGraphViewService graphService;
    private final DemoDataInitializer demoDataInitializer;

    public HomeView(
            DisruptionApplicationService disruptionService,
            ImpactAnalysisApplicationService impactService,
            SimulationApplicationService simulationService,
            MonteCarloApplicationService monteCarloService,
            OptimizationApplicationService optimizationService,
            RecoveryPlanningApplicationService recoveryService,
            com.supplygraph.application.graph.NetworkGraphViewService graphService
    ) {
        this(disruptionService, impactService, simulationService, monteCarloService, optimizationService, recoveryService, graphService, null);
    }

    @Autowired
    public HomeView(
            DisruptionApplicationService disruptionService,
            ImpactAnalysisApplicationService impactService,
            SimulationApplicationService simulationService,
            MonteCarloApplicationService monteCarloService,
            OptimizationApplicationService optimizationService,
            RecoveryPlanningApplicationService recoveryService,
            com.supplygraph.application.graph.NetworkGraphViewService graphService,
            @Autowired(required = false) DemoDataInitializer demoDataInitializer
    ) {
        this.graphService = graphService;
        this.disruptionService = disruptionService;
        this.impactService = impactService;
        this.simulationService = simulationService;
        this.monteCarloService = monteCarloService;
        this.optimizationService = optimizationService;
        this.recoveryService = recoveryService;
        this.demoDataInitializer = demoDataInitializer;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1440px");
        getStyle().set("margin", "0 auto");
        addClassName("sg-home");

        buildHeader();
        buildDataPanel();
        buildKpiStrip();
        buildMapAndIncidents();
        buildExploreTiles();
    }

    private com.supplygraph.adapter.ai.IncidentInvestigationService investigations;
    private com.supplygraph.application.agent.AgentLearningService learning;
    private com.supplygraph.application.port.out.AgentInvestigationRepositoryPort investigationRepo;
    private MonteCarloApplicationService monteCarloSvc;
    private com.supplygraph.web.status.SystemStatusService statusService;
    private boolean intelligenceAdded;

    @Autowired(required = false)
    public void setStatusService(com.supplygraph.web.status.SystemStatusService statusService) { this.statusService = statusService; }

    /** Optional collaborators (absent in slim test contexts); the agent and simulation panels appear only when they are available. */
    @Autowired(required = false)
    public void setIntelligence(com.supplygraph.adapter.ai.IncidentInvestigationService investigations, com.supplygraph.application.agent.AgentLearningService learning,
                                com.supplygraph.application.port.out.AgentInvestigationRepositoryPort investigationRepo) {
        this.investigations = investigations;
        this.learning = learning;
        this.investigationRepo = investigationRepo;
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent e) {
        super.onAttach(e);
        if (intelligenceAdded) return;
        intelligenceAdded = true;
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        Div row = new Div();
        row.addClassName("sg-home-row");
        if (investigations != null && learning != null && investigationRepo != null) {
            Div a = card();
            a.getStyle().set("flex", "1 1 520px").set("min-width", "0");
            a.add(new com.supplygraph.web.components.AgentActivityPanel(wsId, disruptionService, investigations, learning, investigationRepo));
            row.add(a);
        }
        Div sim = card();
        sim.getStyle().set("flex", "1 1 520px").set("min-width", "0");
        sim.add(new com.supplygraph.web.components.SimulationPanel(wsId, disruptionService, simulationService, monteCarloService));
        row.add(sim);
        // keep the Explore tiles last
        int tilesIndex = getComponentCount() - 2;
        addComponentAtIndex(Math.max(0, tilesIndex), row);
        if (statusService != null) add(new com.supplygraph.web.status.SystemStatusPanel(statusService.current(), true));
    }

    private void buildDataPanel() {
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        var net = graphService.build(wsId);
        Div panel = card();
        panel.addClassNames("sg-data-panel");
        if (net.isEmpty()) panel.addClassName("sg-data-panel-empty");
        Span step = new Span("STEP 1");
        step.getElement().getThemeList().add("badge primary small");
        H4 t = new H4(net.isEmpty() ? "Add your supply chain to begin" : "Your data");
        t.getStyle().set("margin", "0");
        Paragraph p = new Paragraph(net.isEmpty()
                ? "SupplyGraph has no network yet. Everything else (map, impact, agents, simulation) is built from it. Load the sample supply chain to explore, or import your own facilities, lanes and stock."
                : "The twin holds " + net.nodes().size() + " facilities and " + net.lanes().size() + " lanes (version " + net.twinVersion() + "). Replace it any time with the sample or your own data.");
        p.getStyle().set("margin", "4px 0 8px").set("color", "var(--lumo-secondary-text-color)");
        Button sample = new Button("Load sample supply chain", VaadinIcon.DATABASE.create(), e -> {
            if (demoDataInitializer == null) { Notification.show("The sample loader is not available in this environment.", 3000, Notification.Position.MIDDLE); return; }
            try {
                demoDataInitializer.seedDemoData(true);
                Notification.show("Sample supply chain loaded.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().getPage().reload();
            } catch (Exception ex) {
                Notification.show("Could not load: " + ex.getMessage(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        sample.addThemeVariants(net.isEmpty() ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_TERTIARY);
        sample.addClassName("sg-load-sample");
        Button imp = new Button("Import my own data (CSV)", VaadinIcon.UPLOAD.create(), e -> UI.getCurrent().navigate(SupplyChainImportView.class));
        imp.addClassName("sg-import-own");
        imp.addThemeVariants(net.isEmpty() ? ButtonVariant.LUMO_CONTRAST : ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout actions = new HorizontalLayout(sample, imp);
        panel.add(step, t, p, actions);
        add(panel);
    }

    private static Div card() {
        Div c = new Div();
        c.addClassName("sg-card");
        return c;
    }

    private void buildHeader() {
        Div band = new Div();
        band.addClassName("sg-hero");
        Div text = new Div();
        H2 title = new H2("Control tower");
        title.getStyle().set("margin", "0");
        Paragraph sub = new Paragraph("Your supply chain as a living digital twin: see what is disrupted, what it reaches, and recover with governed, auditable decisions.");
        sub.getStyle().set("margin", "4px 0 0").set("color", "var(--lumo-secondary-text-color)");
        text.add(title, sub);

        Button guided = new Button("Start guided recovery", VaadinIcon.PLAY.create(), e -> UI.getCurrent().navigate(IncidentWorkspaceView.class));
        guided.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        guided.addClassName("sg-cta-guided");
        HorizontalLayout actions = new HorizontalLayout(guided);
        if (demoDataInitializer != null) {
            Button seed = new Button("Reload sample network", VaadinIcon.REFRESH.create(), e -> {
                try {
                    demoDataInitializer.seedDemoData(true);
                    Notification.show("Sample network reloaded.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().getPage().reload();
                } catch (Exception ex) {
                    Notification.show("Could not reload: " + ex.getMessage(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            seed.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            actions.add(seed);
        }
        band.add(text, actions);
        add(band);
    }

    private void buildKpiStrip() {
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        var net = graphService.build(wsId);
        Div strip = new Div();
        strip.addClassName("sg-kpi-grid");
        strip.add(kpi("Active incidents", String.valueOf(disruptionService.countActiveDisruptions(wsId)), net.disruptedNodeCount() + " facilities disrupted", "var(--lumo-error-color)", VaadinIcon.WARNING),
                kpi("Network", net.isEmpty() ? "No data" : net.nodes().size() + " facilities", net.isEmpty() ? "Load a supply chain" : net.lanes().size() + " lanes · twin v" + net.twinVersion(), "var(--lumo-primary-color)", VaadinIcon.SITEMAP),
                kpi("Awaiting approval", String.valueOf(recoveryService.countPendingApprovals(wsId)), "recovery plans", "var(--lumo-warning-text-color)", VaadinIcon.CHECK_CIRCLE),
                kpi("Impact analyses", String.valueOf(impactService.listImpactAssessments(wsId).size()), "computed from the twin", "var(--lumo-success-color)", VaadinIcon.SEARCH));
        add(strip);
    }

    private Div kpi(String label, String value, String note, String color, VaadinIcon icon) {
        Div c = card();
        c.addClassName("sg-kpi");
        var ic = icon.create();
        ic.setSize("20px");
        ic.getStyle().set("color", color);
        Span l = new Span(label);
        l.addClassName("sg-kpi-label");
        Div top = new Div(ic, l);
        top.addClassName("sg-kpi-top");
        Span v = new Span(value);
        v.addClassName("sg-kpi-value");
        Span n = new Span(note);
        n.addClassName("sg-kpi-note");
        c.add(top, v, n);
        return c;
    }

    private void buildMapAndIncidents() {
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        Div row = new Div();
        row.addClassName("sg-home-row");

        Div mapCard = card();
        mapCard.addClassName("sg-map-card");
        mapCard.getStyle().set("flex", "1 1 720px").set("min-width", "0");
        H3 mt = new H3("Network map");
        mt.getStyle().set("margin", "0 0 8px");
        mapCard.add(mt, new DigitalTwinNetworkGraphComponent(graphService.build(wsId)));

        Div inc = card();
        inc.addClassName("sg-incidents");
        inc.getStyle().set("flex", "0 1 340px").set("min-width", "260px");
        H3 it = new H3("Needs attention");
        it.getStyle().set("margin", "0 0 8px");
        inc.add(it);
        List<Disruption> list = disruptionService.listDisruptions(wsId);
        if (list.isEmpty()) {
            inc.add(new Paragraph("No incidents registered. The network is running normally."));
        }
        for (Disruption d : list) {
            Div item = new Div();
            item.addClassName("sg-incident");
            Span sev = new Span(d.severity().name());
            sev.getElement().getThemeList().add(d.severity().name().equals("CRITICAL") ? "badge error small" : "badge warning small");
            Span st = new Span(d.status().name());
            st.getElement().getThemeList().add("badge contrast small");
            Div tags = new Div(sev, st);
            tags.getStyle().set("display", "flex").set("gap", "6px").set("margin-bottom", "4px");
            Span name = new Span(d.title());
            name.getStyle().set("font-weight", "600").set("display", "block");
            Span where = new Span("Affects " + (d.affectedNodeIds().isEmpty() ? "no declared facility" : d.affectedNodeIds().stream().map(NodeId::value).collect(Collectors.joining(", "))));
            where.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)").set("display", "block");
            Button act = new Button("Recover", VaadinIcon.ARROW_RIGHT.create(), e -> UI.getCurrent().navigate(IncidentWorkspaceView.class));
            act.setIconAfterText(true);
            act.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            act.getStyle().set("margin-top", "8px");
            item.add(tags, name, where, act);
            inc.add(item);
        }
        row.add(mapCard, inc);
        add(row);
    }

    private record Tile(String title, String text, String route, VaadinIcon icon) {}

    private void buildExploreTiles() {
        H3 title = new H3("Explore");
        title.getStyle().set("margin", "8px 0 0");
        Div grid = new Div();
        grid.addClassName("sg-tile-grid");
        List<Tile> tiles = List.of(
                new Tile("Network", "Facilities, lanes, stock and the twin's history.", "digital-twin", VaadinIcon.SITEMAP),
                new Tile("Incidents", "Register disruptions and see what they reach.", "disruptions", VaadinIcon.WARNING),
                new Tile("Decisions", "Agent proposals, recovery plans and approvals.", "decision-centre", VaadinIcon.LIGHTBULB),
                new Tile("Execution", "Governed workflows, approvals and outcomes.", "execution-centre", VaadinIcon.PLAY),
                new Tile("Suppliers", "Supplier risk, orders, RFQs and quality.", "supplier-intelligence", VaadinIcon.TRUCK),
                new Tile("Planning", "Demand, inventory buffers and production runs.", "planning/demand", VaadinIcon.CALENDAR),
                new Tile("Sustainability", "Carbon, ESG and traceability.", "sustainability/traceability/genealogy", VaadinIcon.GLOBE),
                new Tile("Analytics lab", "Simulation, Monte Carlo and optimization.", "simulation", VaadinIcon.CHART_LINE),
                new Tile("System monitor", "Is the website healthy? Live checks, speed and errors.", "monitoring", VaadinIcon.DASHBOARD));
        for (Tile t : tiles) {
            Div c = card();
            c.addClassName("sg-tile");
            var ic = t.icon().create();
            ic.setSize("22px");
            ic.getStyle().set("color", "var(--lumo-primary-color)");
            Span name = new Span(t.title());
            name.getStyle().set("font-weight", "700").set("display", "block").set("margin-top", "6px");
            Span text = new Span(t.text());
            text.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            c.add(ic, name, text);
            c.getElement().addEventListener("click", e -> UI.getCurrent().navigate(t.route()));
            grid.add(c);
        }
        add(title, grid);
    }
}
