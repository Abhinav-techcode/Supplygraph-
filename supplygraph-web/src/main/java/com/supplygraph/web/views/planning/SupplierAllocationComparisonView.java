package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.ProcurementPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.NetRequirementItem;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.domain.model.planning.SupplierAllocationShare;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route(value = "planning/procurement/allocation", layout = MainLayout.class)
@PageTitle("Supplier Allocation Comparison | ORBIX")
public class SupplierAllocationComparisonView extends PlanningViewBase {

    private final ProcurementPlanningService procurementService;
    private final Grid<SupplierAllocationShare> grid = new Grid<>();
    private final List<SupplierAllocationShare> currentAllocations = new ArrayList<>();

    public SupplierAllocationComparisonView(ProcurementPlanningService procurementService) {
        super("Supplier Allocation Comparison", "Multi-sourcing volume split analysis, risk-diversification trade-offs, and procurement commitment formulation.");
        this.procurementService = procurementService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout setupForm = new HorizontalLayout();
        setupForm.setWidthFull();
        setupForm.setAlignItems(Alignment.BASELINE);

        TextField materialField = new TextField("Component / Material");
        materialField.setValue("MAT-ANODE-GRAPHITE");

        NumberField totalVolumeField = new NumberField("Gross Required Volume (kg)");
        totalVolumeField.setValue(50000.0);

        NumberField primarySplitField = new NumberField("Primary Supplier Split (%)");
        primarySplitField.setValue(70.0);
        primarySplitField.setMin(0.0);
        primarySplitField.setMax(100.0);

        Button evaluateBtn = new Button("Calculate Allocation Split", e -> {
            currentAllocations.clear();
            double totalVol = totalVolumeField.getValue();
            double primaryPct = primarySplitField.getValue();
            double secondaryPct = 100.0 - primaryPct;

            double primaryVol = totalVol * (primaryPct / 100.0);
            double secondaryVol = totalVol * (secondaryPct / 100.0);

            BigDecimal primaryUnitCost = new BigDecimal("14.50");
            BigDecimal secondaryUnitCost = new BigDecimal("15.20");

            currentAllocations.add(new SupplierAllocationShare(
                    new NodeId("SUPPLIER-ALPHA-MINING"),
                    new MaterialId(materialField.getValue()),
                    primaryPct,
                    primaryVol,
                    primaryUnitCost,
                    primaryUnitCost.multiply(BigDecimal.valueOf(primaryVol)).setScale(2, RoundingMode.HALF_UP)
            ));

            if (secondaryPct > 0) {
                currentAllocations.add(new SupplierAllocationShare(
                        new NodeId("SUPPLIER-BETA-REFINERY"),
                        new MaterialId(materialField.getValue()),
                        secondaryPct,
                        secondaryVol,
                        secondaryUnitCost,
                        secondaryUnitCost.multiply(BigDecimal.valueOf(secondaryVol)).setScale(2, RoundingMode.HALF_UP)
                ));
            }

            grid.setItems(new ArrayList<>(currentAllocations));
            Notification.show("Evaluated " + currentAllocations.size() + " supplier allocation splits", 2500, Notification.Position.TOP_END);
        });
        evaluateBtn.getElement().getThemeList().add("primary");

        setupForm.add(materialField, totalVolumeField, primarySplitField, evaluateBtn);
        add(setupForm);

        add(new H3("Volume Allocation & Projected Spend"));
        grid.addColumn(s -> s.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        grid.addColumn(s -> s.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(s -> s.allocationPercentage() + "%").setHeader("Allocation %").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.0f units", s.allocatedQuantity())).setHeader("Allocated Quantity").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.unitCost()).setHeader("Unit Price").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.totalCost()).setHeader("Total Spend").setAutoWidth(true);
        grid.setHeight("250px");
        add(grid);

        Button commitBtn = new Button("Commit Strategic Procurement Plan", e -> {
            if (currentAllocations.isEmpty()) {
                Notification.show("Please calculate an allocation split first.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                NetRequirementItem netReq = procurementService.calculateNetRequirement(
                        new MaterialId(materialField.getValue()),
                        new NodeId("PLANT-BERLIN"),
                        totalVolumeField.getValue(),
                        5000.0,
                        0.0,
                        LocalDate.now().plusWeeks(3)
                );

                ProcurementPlan plan = procurementService.generateProcurementPlan(
                        ws, "Anode Sourcing Q1", List.of(netReq), currentAllocations
                );
                Notification.show("Committed Procurement Plan: " + plan.id().value(), 3000, Notification.Position.TOP_END);
            } catch (Exception ex) {
                Notification.show("Commit failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        commitBtn.getElement().getThemeList().add("success");
        add(commitBtn);
    }
}

