package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.ProductionPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.FiniteProductionSchedule;
import com.supplygraph.domain.model.planning.ScheduledProductionJob;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@Route(value = "planning/production/capacity", layout = MainLayout.class)
@PageTitle("Capacity Utilization Explorer | SupplyGraph")
public class CapacityUtilizationExplorerView extends PlanningViewBase {

    public CapacityUtilizationExplorerView(ProductionPlanningService productionService) {
        super("Capacity Utilization Explorer", "Work center finite capacity constraints, shift hours, and changeover scheduling.");

        WorkspaceId ws = currentWorkspaceId();
        NodeId plantId = new NodeId("PLANT-BERLIN");

        List<ScheduledProductionJob> sampleJobs = List.of(
                new ScheduledProductionJob("JOB-101", new MaterialId("MAT-BP-4680"), plantId, LocalDate.now(), 1, 400.0, 1.0, 6.0, 1),
                new ScheduledProductionJob("JOB-102", new MaterialId("MAT-BP-2170"), plantId, LocalDate.now(), 1, 150.0, 0.5, 2.0, 2),
                new ScheduledProductionJob("JOB-103", new MaterialId("MAT-BP-4680"), plantId, LocalDate.now(), 2, 500.0, 0.5, 7.0, 1)
        );

        Grid<ScheduledProductionJob> grid = new Grid<>();
        grid.addColumn(ScheduledProductionJob::jobId).setHeader("Job ID").setAutoWidth(true);
        grid.addColumn(j -> j.productMaterialId().value()).setHeader("Product").setAutoWidth(true);
        grid.addColumn(j -> "Shift " + j.shiftNumber()).setHeader("Shift").setAutoWidth(true);
        grid.addColumn(j -> j.sequenceNumber()).setHeader("Sequence").setAutoWidth(true);
        grid.addColumn(j -> j.plannedQuantity() + " units").setHeader("Planned Output").setAutoWidth(true);
        grid.addColumn(j -> j.changeoverHours() + " hrs").setHeader("Changeover").setAutoWidth(true);
        grid.addColumn(j -> j.runHours() + " hrs").setHeader("Run Duration").setAutoWidth(true);
        grid.addColumn(j -> j.getTotalOccupiedHours() + " hrs").setHeader("Total Occupancy").setAutoWidth(true);

        grid.setItems(sampleJobs);
        grid.setHeight("300px");

        add(new H3("Work Center Line 1 Scheduled Jobs"), grid);

        Button commitBtn = new Button("Commit Finite Schedule", e -> {
            try {
                FiniteProductionSchedule schedule = productionService.generateFiniteSchedule(
                        ws, plantId, "Line 1 Finite Shift Run", sampleJobs, 24.0
                );
                Notification.show("Saved Schedule: " + schedule.id().value() + " (Util: " + String.format("%.1f%%", schedule.averageCapacityUtilization() * 100) + ")", 3000, Notification.Position.TOP_END);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        commitBtn.getElement().getThemeList().add("primary");
        add(commitBtn);
    }
}

