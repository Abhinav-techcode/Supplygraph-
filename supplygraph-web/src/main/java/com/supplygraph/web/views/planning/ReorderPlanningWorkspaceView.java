package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.MultiEchelonInventoryService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.EchelonInventoryPosition;
import com.supplygraph.domain.model.planning.EchelonTier;
import com.supplygraph.domain.model.planning.InventoryPlan;
import com.supplygraph.domain.model.planning.ServiceLevel;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "planning/inventory/reorder", layout = MainLayout.class)
@PageTitle("Reorder Planning Workspace | SupplyGraph")
public class ReorderPlanningWorkspaceView extends PlanningViewBase {

    private final MultiEchelonInventoryService inventoryService;
    private final Grid<EchelonInventoryPosition> grid = new Grid<>();

    public ReorderPlanningWorkspaceView(MultiEchelonInventoryService inventoryService) {
        super("Network Reorder Planning Workspace", "Real-time multi-echelon replenishment monitoring and threshold breach remediation.");
        this.inventoryService = inventoryService;

        WorkspaceId ws = currentWorkspaceId();

        List<EchelonInventoryPosition> samplePositions = List.of(
                new EchelonInventoryPosition(new NodeId("DC-ROTTERDAM"), EchelonTier.CENTRAL_DC, new MaterialId("MAT-BP-4680"), 120.0, 50.0, 80.0, 180.0, 500.0, 4.50),
                new EchelonInventoryPosition(new NodeId("HUB-LEIPZIG"), EchelonTier.REGIONAL_HUB, new MaterialId("MAT-BP-4680"), 35.0, 10.0, 40.0, 60.0, 200.0, 5.00),
                new EchelonInventoryPosition(new NodeId("PLANT-BERLIN"), EchelonTier.PLANT_WAREHOUSE, new MaterialId("MAT-BP-4680"), 450.0, 100.0, 150.0, 300.0, 1000.0, 3.20)
        );

        grid.addColumn(p -> p.locationId().value()).setHeader("Node / Location").setAutoWidth(true);
        grid.addColumn(p -> p.tier().getDescription()).setHeader("Echelon Tier").setAutoWidth(true);
        grid.addColumn(p -> p.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(EchelonInventoryPosition::onHandQuantity).setHeader("On Hand").setAutoWidth(true);
        grid.addColumn(EchelonInventoryPosition::inTransitQuantity).setHeader("In Transit").setAutoWidth(true);
        grid.addColumn(EchelonInventoryPosition::getInventoryPosition).setHeader("Total Position").setAutoWidth(true);
        grid.addColumn(EchelonInventoryPosition::reorderPoint).setHeader("Reorder Point (ROP)").setAutoWidth(true);
        grid.addColumn(EchelonInventoryPosition::safetyStock).setHeader("Safety Stock").setAutoWidth(true);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.isReorderRequired() ? "REORDER TRIGGERED" : "HEALTHY BUFFER");
            badge.getElement().getThemeList().add("badge " + (p.isReorderRequired() ? "error" : "success") + " small");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.setItems(samplePositions);
        grid.setHeight("350px");

        HorizontalLayout toolbar = new HorizontalLayout();
        Button createPlanBtn = new Button("Commit Network Stock Plan", e -> {
            try {
                InventoryPlan plan = inventoryService.generateInventoryPlan(
                        ws, "Auto Replenishment " + java.time.LocalDate.now(), ServiceLevel.SL_95, samplePositions
                );
                Notification.show("Committed Inventory Plan: " + plan.id().value(), 3000, Notification.Position.TOP_END);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        createPlanBtn.getElement().getThemeList().add("primary");
        toolbar.add(createPlanBtn);

        add(grid, toolbar);
    }
}

