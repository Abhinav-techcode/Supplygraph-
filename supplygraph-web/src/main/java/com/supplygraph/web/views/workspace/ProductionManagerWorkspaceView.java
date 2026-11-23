package com.supplygraph.web.views.workspace;

import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.application.planning.ProductionPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/production", layout = MainLayout.class)
@PageTitle("Production Manager Workspace | SupplyGraph")
public class ProductionManagerWorkspaceView extends WorkspaceViewBase {

    private final ProductionPlanningService productionService;
    private final SupplierQualityCollaborationService qualityService;

    public ProductionManagerWorkspaceView(
            ProductionPlanningService productionService,
            SupplierQualityCollaborationService qualityService
    ) {
        super("Production Operations & Scheduling Cockpit", "Shop floor finite capacity scheduling, machine changeover minimization, and assembly line yield.", "Role :: Plant Production Manager");
        this.productionService = productionService;
        this.qualityService = qualityService;

        WorkspaceId ws = currentWorkspaceId();
        List<FiniteProductionSchedule> schedules = productionService.listSchedules(ws);
        List<QualityIncident8D> incidents = qualityService.listIncidents(ws);

        double avgUtil = schedules.stream().mapToDouble(FiniteProductionSchedule::averageCapacityUtilization).average().orElse(Double.NaN);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Finite Machine Runs", String.valueOf(schedules.size()), "Scheduled job campaigns", "primary"));
        kpiRow.add(createKpiCard("Avg Capacity Utilization", Double.isNaN(avgUtil) ? "No schedules yet" : String.format("%.1f%%", avgUtil), "Scheduled machine capacity", "success"));
        kpiRow.add(createKpiCard("Line Yield Efficiency", "No data", "No production-yield source connected", "warning"));
        kpiRow.add(createKpiCard("Quality Holds", String.valueOf(incidents.stream().filter(i -> !i.closed()).count()), "Active 8D shop floor alerts", "error"));

        add(kpiRow);

        add(new H3("Finite Production Schedules"));
        Grid<FiniteProductionSchedule> grid = new Grid<>();
        grid.addColumn(s -> s.id().value()).setHeader("Schedule ID").setAutoWidth(true);
        grid.addColumn(FiniteProductionSchedule::scheduleName).setHeader("Schedule Name").setAutoWidth(true);
        grid.addColumn(s -> s.plantId().value()).setHeader("Plant / Gigafactory").setAutoWidth(true);
        grid.addColumn(s -> s.scheduledJobs().size() + " jobs").setHeader("Jobs").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f%%", s.averageCapacityUtilization())).setHeader("Capacity Utilization").setAutoWidth(true);
        grid.addColumn(s -> s.generatedAt().toString()).setHeader("Scheduled At").setAutoWidth(true);

        grid.setItems(schedules);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Finite Scheduling Studio", VaadinIcon.CALENDAR_CLOCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/production"))));
        actionRow.add(new Button("Machine Capacity Explorer", VaadinIcon.DASHBOARD.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/production/capacity"))));
        actionRow.add(new Button("Quality 8D Holds", VaadinIcon.WARNING.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/quality"))));

        add(grid, actionRow);
    }
}
