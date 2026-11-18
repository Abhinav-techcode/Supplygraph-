package com.supplygraph.web.views.workspace;

import com.supplygraph.application.pareto.MultiObjectiveParetoService;
import com.supplygraph.application.sensitivity.SensitivityAnalysisService;
import com.supplygraph.application.service.SimulationApplicationService;
import com.supplygraph.application.whatif.WhatIfAnalysisService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/analyst", layout = MainLayout.class)
@PageTitle("Supply Chain Analyst Workspace | SupplyGraph")
public class AnalystWorkspaceView extends WorkspaceViewBase {

    private final WhatIfAnalysisService whatIfService;
    private final SensitivityAnalysisService sensitivityService;
    private final MultiObjectiveParetoService paretoService;
    private final SimulationApplicationService simulationService;

    public AnalystWorkspaceView(
            WhatIfAnalysisService whatIfService,
            SensitivityAnalysisService sensitivityService,
            MultiObjectiveParetoService paretoService,
            SimulationApplicationService simulationService
    ) {
        super("Decision Intelligence & Analytics Sandbox", "Monte Carlo risk distributions, discrete-event simulation, multi-objective Pareto optimization, and What-If hypothesis testing.", "Role :: Supply Chain Data Scientist / Analyst");
        this.whatIfService = whatIfService;
        this.sensitivityService = sensitivityService;
        this.paretoService = paretoService;
        this.simulationService = simulationService;

        WorkspaceId ws = currentWorkspaceId();
        List<WhatIfComparisonResult> scenarios = whatIfService.findAll();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Monte Carlo Trials Run", "No data", "See Monte Carlo runs for actual trial counts", "primary"));
        kpiRow.add(createKpiCard("Pareto Optimal Front", "No data", "Computed on the Pareto page", "success"));
        kpiRow.add(createKpiCard("Sensitivity Robustness", "No data", "Computed on the Sensitivity page", "warning"));
        kpiRow.add(createKpiCard("What-If Hypotheses", String.valueOf(scenarios.size()), "Simulated policy branches", "contrast"));

        add(kpiRow);

        add(new H3("What-If Simulation Scenarios"));
        Grid<WhatIfComparisonResult> grid = new Grid<>();
        grid.addColumn(s -> s.scenarioId().value()).setHeader("Scenario ID").setAutoWidth(true);
        grid.addColumn(WhatIfComparisonResult::scenarioName).setHeader("Hypothesis Name").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.scenarioCostUsd()).setHeader("Scenario Cost").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f%%", s.costVariancePct())).setHeader("Cost Delta %").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.2f days", s.recoveryTimeDeltaDays())).setHeader("TTR Delta").setAutoWidth(true);
        grid.addColumn(s -> s.evaluatedAt().toString()).setHeader("Executed At").setAutoWidth(true);

        grid.setItems(scenarios);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("What-If Sandbox", VaadinIcon.SLIDERS.create(), e -> getUI().ifPresent(ui -> ui.navigate("what-if-workspace"))));
        actionRow.add(new Button("Pareto Optimization", VaadinIcon.CHART_LINE.create(), e -> getUI().ifPresent(ui -> ui.navigate("pareto-comparison"))));
        actionRow.add(new Button("Sensitivity Studio", VaadinIcon.DASHBOARD.create(), e -> getUI().ifPresent(ui -> ui.navigate("sensitivity-analysis"))));
        actionRow.add(new Button("Decision Provenance", VaadinIcon.SITEMAP.create(), e -> getUI().ifPresent(ui -> ui.navigate("decision-provenance"))));

        add(grid, actionRow);
    }
}

