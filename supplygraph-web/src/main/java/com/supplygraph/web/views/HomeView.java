package com.supplygraph.web.views;

import com.supplygraph.application.service.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Platform Foundation & Executive Disruption Overview View.
 * Displays real-time KPIs, active disruptions, and quick navigation into Phase 4 workspaces.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Overview")
public class HomeView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final ImpactAnalysisApplicationService impactService;
    private final SimulationApplicationService simulationService;
    private final MonteCarloApplicationService monteCarloService;
    private final OptimizationApplicationService optimizationService;
    private final RecoveryPlanningApplicationService recoveryService;

    public HomeView() {
        this(null, null, null, null, null, null);
    }

    @Autowired
    public HomeView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) ImpactAnalysisApplicationService impactService,
            @Autowired(required = false) SimulationApplicationService simulationService,
            @Autowired(required = false) MonteCarloApplicationService monteCarloService,
            @Autowired(required = false) OptimizationApplicationService optimizationService,
            @Autowired(required = false) RecoveryPlanningApplicationService recoveryService
    ) {
        this.disruptionService = disruptionService;
        this.impactService = impactService;
        this.simulationService = simulationService;
        this.monteCarloService = monteCarloService;
        this.optimizationService = optimizationService;
        this.recoveryService = recoveryService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildKpiRow();
        buildQuickActions();
        buildRecentDisruptionsSection();
        buildArchitectureSection();
    }

    private void buildHeader() {
        H2 header = new H2("Supply Chain Intelligence & Conflict-Aware Recovery Decision Centre");
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph intro = new Paragraph(
                "SupplyGraph models complex multi-tier supply chains, detects cascading operational disruptions, "
                        + "runs discrete-event & Monte Carlo risk simulations, optimizes alternative allocations with Google OR-Tools, "
                        + "and enforces ConcurGuard semantic conflict arbitration before human executive sign-off."
        );
        intro.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(header, intro);
    }

    private void buildKpiRow() {
        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);

        long activeDisruptions = disruptionService != null ? disruptionService.countActiveDisruptions(wsId) : 1;
        long impactAssessments = impactService != null ? impactService.listImpactAssessments(wsId).size() : 1;
        long simulationRuns = simulationService != null ? simulationService.listSimulationRuns(wsId).size() : 0;
        long monteCarloRuns = monteCarloService != null ? monteCarloService.listAnalysisRuns(wsId).size() : 0;
        long optimizationRuns = optimizationService != null ? optimizationService.listOptimizationRuns(wsId).size() : 0;
        long pendingApprovals = recoveryService != null ? recoveryService.countPendingApprovals(wsId) : 3;

        HorizontalLayout kpiLayout = new HorizontalLayout();
        kpiLayout.setWidthFull();
        kpiLayout.setSpacing(true);

        kpiLayout.add(
                createKpiCard("Active Disruptions", String.valueOf(activeDisruptions), "var(--lumo-error-color)", VaadinIcon.WARNING),
                createKpiCard("Impact Assessments", String.valueOf(impactAssessments), "var(--lumo-primary-color)", VaadinIcon.SEARCH),
                createKpiCard("Simulation Runs", String.valueOf(simulationRuns), "var(--lumo-success-color)", VaadinIcon.PLAY_CIRCLE),
                createKpiCard("Monte Carlo Runs", String.valueOf(monteCarloRuns), "var(--lumo-primary-text-color)", VaadinIcon.CHART_LINE),
                createKpiCard("OR-Tools Optimizations", String.valueOf(optimizationRuns), "var(--lumo-success-color)", VaadinIcon.SLIDERS),
                createKpiCard("Pending Approvals", String.valueOf(pendingApprovals), "var(--lumo-warning-color)", VaadinIcon.CHECK_CIRCLE)
        );

        add(kpiLayout);
    }

    private VerticalLayout createKpiCard(String label, String value, String color, VaadinIcon icon) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.04)");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span titleSpan = new Span(label);
        titleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "600");

        var iconComp = icon.create();
        iconComp.setColor(color);
        top.add(titleSpan, iconComp);

        Span valSpan = new Span(value);
        valSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", color);

        card.add(top, valSpan);
        return card;
    }

    private void buildQuickActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "var(--lumo-space-m)");

        Button btnDisruptions = new Button("Manage Disruptions", VaadinIcon.WARNING.create(),
                e -> UI.getCurrent().navigate(DisruptionsView.class));
        btnDisruptions.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnImpact = new Button("Cascading Impact", VaadinIcon.SEARCH.create(),
                e -> UI.getCurrent().navigate(ImpactAssessmentView.class));

        Button btnSim = new Button("Simulation Workspace", VaadinIcon.PLAY_CIRCLE.create(),
                e -> UI.getCurrent().navigate(SimulationWorkspaceView.class));

        Button btnMonteCarlo = new Button("Monte Carlo Analysis", VaadinIcon.CHART_LINE.create(),
                e -> UI.getCurrent().navigate(MonteCarloWorkspaceView.class));

        Button btnOptimization = new Button("OR-Tools Optimizer", VaadinIcon.SLIDERS.create(),
                e -> UI.getCurrent().navigate(OptimizationWorkspaceView.class));

        Button btnDecision = new Button("Recovery Decision Centre", VaadinIcon.CHECK_CIRCLE.create(),
                e -> UI.getCurrent().navigate(RecoveryDecisionCentreView.class));
        btnDecision.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        actions.add(btnDisruptions, btnImpact, btnSim, btnMonteCarlo, btnOptimization, btnDecision);
        add(actions);
    }

    private void buildRecentDisruptionsSection() {
        VerticalLayout section = new VerticalLayout();
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-m)");

        H3 title = new H3("Active Disruption Incidents");
        title.getStyle().set("margin-top", "0");

        WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
        List<Disruption> disruptions = disruptionService != null
                ? disruptionService.listDisruptions(wsId)
                : List.of();

        Grid<Disruption> grid = new Grid<>(Disruption.class, false);
        grid.addColumn(Disruption::title).setHeader("Title").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(d -> d.category().name()).setHeader("Category").setAutoWidth(true);
        grid.addComponentColumn(d -> {
            Span badge = new Span(d.severity().name());
            String theme = switch (d.severity()) {
                case CRITICAL -> "badge error";
                case HIGH -> "badge warning";
                default -> "badge";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Severity").setAutoWidth(true);

        grid.addComponentColumn(d -> {
            Span badge = new Span(d.status().name());
            String theme = d.status().name().equals("ACTIVE") ? "badge error" : "badge success";
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addColumn(d -> d.startTime().toString()).setHeader("Reported At").setAutoWidth(true);

        grid.setItems(disruptions);
        grid.setAllRowsVisible(true);

        section.add(title, grid);
        add(section);
    }

    private void buildArchitectureSection() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-m)");

        H3 title = new H3("Phase 4 Operational Capabilities");
        title.getStyle().set("margin-top", "0");

        Paragraph modules = new Paragraph(
                "• Discrete-Event Simulation Engine: Deterministic, event-priority chronological event dispatching.\n"
                        + "• Monte Carlo Uncertainty Engine: Seeded pseudo-random variate generation across Normal, Triangular, and Uniform distributions.\n"
                        + "• Google OR-Tools Optimizer: Mixed-Integer Programming (MPSolver GLOP/SCIP) with deterministic pure-Java relaxation fallback.\n"
                        + "• ConcurGuard Semantic Arbitration: Multi-agent resource conflict detection, exclusivity checks, and stale-state protection.\n"
                        + "• Recovery Decision Centre: Human-in-the-loop executive governance and immutable audited approvals."
        );
        modules.getStyle().set("white-space", "pre-line").set("color", "var(--lumo-secondary-text-color)");

        card.add(title, modules);
        add(card);
    }
}
