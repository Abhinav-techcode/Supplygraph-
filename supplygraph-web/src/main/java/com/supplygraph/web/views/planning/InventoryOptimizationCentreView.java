package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.MultiEchelonInventoryService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.InventoryPlan;
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

@Route(value = "planning/inventory", layout = MainLayout.class)
@PageTitle("Inventory Optimization Centre | ORBIX")
public class InventoryOptimizationCentreView extends PlanningViewBase {

    public InventoryOptimizationCentreView(MultiEchelonInventoryService inventoryService) {
        super("Multi-Echelon Inventory Optimization (MEIO) Centre", "Stochastic safety stock positioning, lead-time variability buffering, and multi-tier network stock allocation.");

        WorkspaceId ws = currentWorkspaceId();
        List<InventoryPlan> plans = inventoryService.listInventoryPlans(ws);

        HorizontalLayout navRow = new HorizontalLayout();
        navRow.add(new Button("Safety Stock Calculator", VaadinIcon.CALC.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/inventory/safety-stock"))));
        navRow.add(new Button("Reorder Planning Workspace", VaadinIcon.STORAGE.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/inventory/reorder"))));
        add(navRow);

        add(new H3("Network Inventory Plans"));
        Grid<InventoryPlan> grid = new Grid<>();
        grid.addColumn(p -> p.id().value()).setHeader("Plan ID").setAutoWidth(true);
        grid.addColumn(InventoryPlan::planName).setHeader("Plan Name").setAutoWidth(true);
        grid.addColumn(p -> p.targetServiceLevel().getLabel()).setHeader("Service Level").setAutoWidth(true);
        grid.addColumn(InventoryPlan::getPositionCount).setHeader("Network Positions").setAutoWidth(true);
        grid.addColumn(p -> p.getAtRiskPositionCount() + " positions").setHeader("Reorder Required").setAutoWidth(true);
        grid.addColumn(p -> "$" + String.format("%.2f", p.totalCarryingCost())).setHeader("Annual Carrying Cost").setAutoWidth(true);
        grid.addColumn(p -> p.calculatedAt().toString()).setHeader("Calculated At").setAutoWidth(true);

        grid.setItems(plans);
        grid.setSizeFull();

        if (plans.isEmpty()) {
            add(new Span("No inventory plans formulated yet. Launch the Safety Stock Planner to run stochastic calculations."));
        } else {
            add(grid);
        }
    }
}

