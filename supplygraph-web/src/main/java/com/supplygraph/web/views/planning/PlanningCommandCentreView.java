package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.DemandPlanningService;
import com.supplygraph.application.planning.MultiEchelonInventoryService;
import com.supplygraph.application.planning.PlanningScenarioService;
import com.supplygraph.application.planning.ProcurementPlanningService;
import com.supplygraph.application.planning.ProductionPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning", layout = MainLayout.class)
@PageTitle("Planning Command Centre | SupplyGraph")
public class PlanningCommandCentreView extends PlanningViewBase {

    private final DemandPlanningService demandService;
    private final MultiEchelonInventoryService inventoryService;
    private final ProcurementPlanningService procurementService;
    private final ProductionPlanningService productionService;
    private final PlanningScenarioService scenarioService;

    public PlanningCommandCentreView(
            DemandPlanningService demandService,
            MultiEchelonInventoryService inventoryService,
            ProcurementPlanningService procurementService,
            ProductionPlanningService productionService,
            PlanningScenarioService scenarioService
    ) {
        super("Planning Command Centre", "Integrated end-to-end operational planning, MEIO safety stocks, procurement explosion, and finite capacity scheduling.");
        this.demandService = demandService;
        this.inventoryService = inventoryService;
        this.procurementService = procurementService;
        this.productionService = productionService;
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Demand Forecasts", String.valueOf(demandService.listForecasts(ws).size()), "Active demand periods", "primary"));
        kpiRow.add(createKpiCard("Inventory Plans", String.valueOf(inventoryService.listInventoryPlans(ws).size()), "MEIO safety positions", "success"));
        kpiRow.add(createKpiCard("Procurement Plans", String.valueOf(procurementService.listProcurementPlans(ws).size()), "Sourcing allocations", "contrast"));
        kpiRow.add(createKpiCard("Production Schedules", String.valueOf(productionService.listSchedules(ws).size()), "Finite capacity runs", "warning"));

        add(kpiRow);

        add(new H3("Active Tactical Planning Scenarios"));
        Grid<PlanningScenarioComparison> grid = new Grid<>();
        grid.addColumn(s -> s.scenarioId().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(PlanningScenarioComparison::scenarioName).setHeader("Scenario Name").setAutoWidth(true);
        grid.addColumn(s -> s.scenarioType().getDescription()).setHeader("Type").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.status().name());
            if (s.status() == PlanningPlanStatus.APPROVED) {
                badge.getElement().getThemeList().add("badge success");
            } else if (s.status() == PlanningPlanStatus.PENDING_APPROVAL) {
                badge.getElement().getThemeList().add("badge warning");
            } else {
                badge.getElement().getThemeList().add("badge contrast");
            }
            return badge;
        }).setHeader("Status").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.totalProjectedCost()).setHeader("Projected Cost").setAutoWidth(true);
        grid.addColumn(s -> s.customerServiceFillRate() + "%").setHeader("Fill Rate").setAutoWidth(true);
        grid.addColumn(s -> s.averageLeadTimeDays() + " days").setHeader("Lead Time").setAutoWidth(true);
        grid.addColumn(s -> s.carbonFootprintCo2Tons() + " t CO2e").setHeader("Emissions").setAutoWidth(true);

        List<PlanningScenarioComparison> scenarios = scenarioService.listScenarios(ws);
        grid.setItems(scenarios);
        grid.setHeight("350px");

        HorizontalLayout actions = new HorizontalLayout();
        actions.add(new Button("Demand Planning", VaadinIcon.CHART_LINE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/demand"))));
        actions.add(new Button("Inventory Optimization", VaadinIcon.STORAGE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/inventory"))));
        actions.add(new Button("Procurement Planning", VaadinIcon.CART.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/procurement"))));
        actions.add(new Button("Production Schedules", VaadinIcon.FACTORY.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/production"))));
        actions.add(new Button("Scenario Designer", VaadinIcon.SLIDERS.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/scenarios"))));

        add(grid, actions);
    }

    private VerticalLayout createKpiCard(String title, String value, String description, String theme) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames("p-m", "border", "rounded-m");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        Span t = new Span(title);
        t.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        H3 v = new H3(value);
        v.getStyle().set("margin", "var(--lumo-space-xs) 0");

        Span d = new Span(description);
        d.getElement().getThemeList().add("badge " + theme + " small");

        card.add(t, v, d);
        return card;
    }
}

