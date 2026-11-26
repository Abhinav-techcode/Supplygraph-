package com.supplygraph.web.views.workspace;

import com.supplygraph.application.explainability.ExplainableRecommendationService;
import com.supplygraph.application.pareto.MultiObjectiveParetoService;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.sustainability.SupplierSustainabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/executive", layout = MainLayout.class)
@PageTitle("Executive Workspace | ORBIX")
public class ExecutiveWorkspaceView extends WorkspaceViewBase {

    private final DisruptionApplicationService disruptionService;
    private final ExplainableRecommendationService recommendationService;
    private final MultiObjectiveParetoService paretoService;
    private final SupplierSustainabilityService esgService;
    private final com.supplygraph.application.intelligence.SupplyChainResilienceService resilienceService;
    private final com.supplygraph.application.service.ImpactAnalysisApplicationService impactService;

    public ExecutiveWorkspaceView(
            DisruptionApplicationService disruptionService,
            ExplainableRecommendationService recommendationService,
            MultiObjectiveParetoService paretoService,
            SupplierSustainabilityService esgService,
            com.supplygraph.application.intelligence.SupplyChainResilienceService resilienceService,
            com.supplygraph.application.service.ImpactAnalysisApplicationService impactService
    ) {
        super("Executive Decision Cockpit", "High-altitude enterprise risk index, revenue-at-risk exposure, systemic disruption monitoring, and ESG compliance.", "Role :: C-Level Executive / VP Supply Chain");
        this.disruptionService = disruptionService;
        this.recommendationService = recommendationService;
        this.paretoService = paretoService;
        this.esgService = esgService;
        this.resilienceService = resilienceService;
        this.impactService = impactService;

        WorkspaceId ws = currentWorkspaceId();
        List<Disruption> disruptions = disruptionService.listDisruptions(ws);
        List<SupplierEsgScorecard> esgs = esgService.listScorecards(ws);
        List<Disruption> active = disruptions.stream().filter(d -> d.status() == com.supplygraph.domain.model.disruption.DisruptionStatus.ACTIVE).toList();
        String resilience = resilienceService.getLatestAssessment(ws.value())
                .map(a -> String.format("%.1f / 100", a.overallResilienceIndex())).orElse("No assessment yet");
        java.math.BigDecimal exposure = java.math.BigDecimal.ZERO;
        int assessed = 0;
        for (Disruption d : active) {
            var a = impactService.getLatestAssessment(d.id());
            if (a.isPresent()) { exposure = exposure.add(a.get().totalEstimatedCost()); assessed++; }
        }
        String exposureText = assessed == 0 ? "No impact assessment yet" : String.format("$%,.0f", exposure);
        String esgText = esgs.isEmpty() ? "No scorecards yet" : String.format("%.1f / 100", esgs.stream().mapToDouble(SupplierEsgScorecard::compositeEsgScore).average().orElse(0));

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Supply Chain Resilience Index", resilience, "From the latest network resilience assessment", "success"));
        kpiRow.add(createKpiCard("Active Disruptions", String.valueOf(active.size()), "Disruptions in ACTIVE status", "error"));
        kpiRow.add(createKpiCard("Estimated Financial Exposure", exposureText, assessed + " of " + active.size() + " active disruption(s) assessed; assumes $85/unit baseline price", "warning"));
        kpiRow.add(createKpiCard("Supplier ESG Score (average)", esgText, esgs.size() + " supplier scorecard(s)", "primary"));

        add(kpiRow);

        add(new H3("Critical Disruption & Incident Portfolio"));
        Grid<Disruption> grid = new Grid<>();
        grid.addColumn(d -> d.id().value()).setHeader("Incident ID").setAutoWidth(true);
        grid.addColumn(Disruption::title).setHeader("Disruption Title").setAutoWidth(true);
        grid.addComponentColumn(d -> {
            Span badge = new Span(d.severity().name());
            badge.getElement().getThemeList().add("badge small " + (d.severity().name().contains("CRITICAL") ? "error" : "warning"));
            return badge;
        }).setHeader("Severity").setAutoWidth(true);
        grid.addColumn(d -> d.status().name()).setHeader("Lifecycle Stage").setAutoWidth(true);
        grid.addColumn(d -> d.createdAt().toString()).setHeader("Detected At").setAutoWidth(true);

        grid.setItems(disruptions);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Explainable Recommendations", VaadinIcon.LIGHTBULB.create(), e -> getUI().ifPresent(ui -> ui.navigate("recommendations-explainable"))));
        actionRow.add(new Button("Pareto Trade-Off Frontier", VaadinIcon.CHART_LINE.create(), e -> getUI().ifPresent(ui -> ui.navigate("pareto-comparison"))));
        actionRow.add(new Button("Sustainability Command", VaadinIcon.GLOBE.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability"))));
        actionRow.add(new Button("Tactical Planning", VaadinIcon.CALC_BOOK.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning"))));

        add(grid, actionRow);
    }
}
