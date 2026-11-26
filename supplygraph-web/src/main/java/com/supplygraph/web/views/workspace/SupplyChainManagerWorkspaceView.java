package com.supplygraph.web.views.workspace;

import com.supplygraph.application.planning.DemandPlanningService;
import com.supplygraph.application.planning.MultiEchelonInventoryService;
import com.supplygraph.application.planning.PlanningScenarioService;
import com.supplygraph.application.planning.ProductionPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/planner", layout = MainLayout.class)
@PageTitle("Supply Chain Manager Workspace | ORBIX")
public class SupplyChainManagerWorkspaceView extends WorkspaceViewBase {

    private final DemandPlanningService demandService;
    private final MultiEchelonInventoryService inventoryService;
    private final ProductionPlanningService productionService;
    private final PlanningScenarioService scenarioService;

    public SupplyChainManagerWorkspaceView(
            DemandPlanningService demandService,
            MultiEchelonInventoryService inventoryService,
            ProductionPlanningService productionService,
            PlanningScenarioService scenarioService
    ) {
        super("Supply Chain Planning Cockpit", "Integrated tactical planning, stochastic MEIO safety stock policies, and cross-echelon synchronization.", "Role :: Supply Chain Manager");
        this.demandService = demandService;
        this.inventoryService = inventoryService;
        this.productionService = productionService;
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();
        List<PlanningScenarioComparison> scenarios = scenarioService.listScenarios(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Demand Forecast Periods", String.valueOf(demandService.listForecasts(ws).size()), "Active monthly forecasts", "primary"));
        kpiRow.add(createKpiCard("MEIO Buffer Positions", String.valueOf(inventoryService.listInventoryPlans(ws).size()), "Multi-echelon safety stock", "success"));
        kpiRow.add(createKpiCard("Finite Schedules", String.valueOf(productionService.listSchedules(ws).size()), "Shop floor job runs", "warning"));
        kpiRow.add(createKpiCard("Customer Service Level", "No data", "No delivery-performance source connected", "contrast"));

        add(kpiRow);

        add(new H3("Tactical Planning Scenarios"));
        Grid<PlanningScenarioComparison> grid = new Grid<>();
        grid.addColumn(s -> s.scenarioId().value()).setHeader("Scenario ID").setAutoWidth(true);
        grid.addColumn(PlanningScenarioComparison::scenarioName).setHeader("Scenario Name").setAutoWidth(true);
        grid.addColumn(s -> s.scenarioType().getDescription()).setHeader("Type").setAutoWidth(true);
        grid.addColumn(s -> s.status().name()).setHeader("Approval Status").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.totalProjectedCost()).setHeader("Total Spend").setAutoWidth(true);
        grid.addColumn(s -> s.customerServiceFillRate() + "%").setHeader("Fill Rate").setAutoWidth(true);
        grid.addColumn(s -> s.averageLeadTimeDays() + " d").setHeader("Avg Lead Time").setAutoWidth(true);
        grid.addColumn(s -> s.carbonFootprintCo2Tons() + " t").setHeader("CO2e Footprint").setAutoWidth(true);

        grid.setItems(scenarios);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Demand Forecasting", VaadinIcon.CHART_LINE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/demand"))));
        actionRow.add(new Button("Safety Stock MEIO", VaadinIcon.STORAGE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/inventory/safety-stock"))));
        actionRow.add(new Button("Finite Scheduling", VaadinIcon.FACTORY.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/production"))));
        actionRow.add(new Button("Approval Handoff", VaadinIcon.CHECK_CIRCLE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/approval"))));

        add(grid, actionRow);
    }
}

