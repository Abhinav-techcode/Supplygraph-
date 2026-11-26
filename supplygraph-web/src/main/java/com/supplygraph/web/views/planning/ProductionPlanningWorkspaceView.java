package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.ProductionPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
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

@Route(value = "planning/production", layout = MainLayout.class)
@PageTitle("Production Planning Workspace | ORBIX")
public class ProductionPlanningWorkspaceView extends PlanningViewBase {

    public ProductionPlanningWorkspaceView(ProductionPlanningService productionService) {
        super("Production Planning Workspace", "Finite capacity shop floor scheduling, machine availability calendars, and changeover optimization.");

        WorkspaceId ws = currentWorkspaceId();
        List<FiniteProductionSchedule> schedules = productionService.listSchedules(ws);

        HorizontalLayout navRow = new HorizontalLayout();
        navRow.add(new Button("Capacity Utilization Explorer", VaadinIcon.DASHBOARD.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/production/capacity"))));
        add(navRow);

        add(new H3("Finite Production Schedules"));
        Grid<FiniteProductionSchedule> grid = new Grid<>();
        grid.addColumn(s -> s.id().value()).setHeader("Schedule ID").setAutoWidth(true);
        grid.addColumn(FiniteProductionSchedule::scheduleName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(s -> s.plantId().value()).setHeader("Plant").setAutoWidth(true);
        grid.addColumn(s -> s.getJobCount() + " jobs").setHeader("Scheduled Jobs").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f%%", s.averageCapacityUtilization() * 100)).setHeader("Capacity Utilization").setAutoWidth(true);
        grid.addColumn(s -> s.generatedAt().toString()).setHeader("Scheduled At").setAutoWidth(true);

        grid.setItems(schedules);
        grid.setSizeFull();

        if (schedules.isEmpty()) {
            add(new Span("No production schedules formulated yet. Launch the Capacity Utilization Explorer to sequence shift jobs."));
        } else {
            add(grid);
        }
    }
}

