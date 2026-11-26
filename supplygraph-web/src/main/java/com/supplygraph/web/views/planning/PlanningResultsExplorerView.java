package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.PlanningScenarioService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning/scenarios/results", layout = MainLayout.class)
@PageTitle("Planning Results Explorer | ORBIX")
public class PlanningResultsExplorerView extends PlanningViewBase {

    public PlanningResultsExplorerView(PlanningScenarioService scenarioService) {
        super("Planning Results Explorer", "Multi-scenario trade-off evaluation matrix comparing cost, fill rate, lead time, and carbon emissions.");

        WorkspaceId ws = currentWorkspaceId();
        List<PlanningScenarioComparison> scenarios = scenarioService.listScenarios(ws);

        Grid<PlanningScenarioComparison> grid = new Grid<>();
        grid.addColumn(s -> s.scenarioId().value()).setHeader("Scenario ID").setAutoWidth(true);
        grid.addColumn(PlanningScenarioComparison::scenarioName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(s -> s.scenarioType().getDescription()).setHeader("Type").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span b = new Span(s.status().name());
            b.getElement().getThemeList().add("badge " + (s.status() == PlanningPlanStatus.APPROVED ? "success" : "contrast") + " small");
            return b;
        }).setHeader("Status").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.totalProjectedCost()).setHeader("Total Spend").setAutoWidth(true);
        grid.addColumn(s -> s.customerServiceFillRate() + "%").setHeader("Service Level").setAutoWidth(true);
        grid.addColumn(s -> s.averageLeadTimeDays() + " days").setHeader("Avg Lead Time").setAutoWidth(true);
        grid.addColumn(s -> s.carbonFootprintCo2Tons() + " t CO2e").setHeader("Emissions").setAutoWidth(true);
        grid.addColumn(s -> s.evaluatedAt().toString()).setHeader("Evaluated At").setAutoWidth(true);

        grid.setItems(scenarios);
        grid.setSizeFull();

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.add(new Button("Executive Approval Handoff", VaadinIcon.CHECK_CIRCLE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/approval"))));

        add(grid, toolbar);
    }
}

