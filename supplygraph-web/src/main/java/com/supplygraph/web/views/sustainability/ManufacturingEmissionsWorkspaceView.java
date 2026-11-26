package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.CarbonAccountingService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "sustainability/carbon/manufacturing", layout = MainLayout.class)
@PageTitle("Manufacturing Emissions Workspace | ORBIX")
public class ManufacturingEmissionsWorkspaceView extends SustainabilityViewBase {

    private final CarbonAccountingService carbonService;

    public ManufacturingEmissionsWorkspaceView(CarbonAccountingService carbonService) {
        super("Manufacturing Emissions Workspace", "Scope 1 stationary combustion and Scope 2 purchased electricity tracking across manufacturing plants and gigafactories.");
        this.carbonService = carbonService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout formsRow = new HorizontalLayout();
        formsRow.setWidthFull();

        Grid<CarbonEmissionRecord> grid = new Grid<>();

        // Scope 1 Section
        HorizontalLayout scope1Form = new HorizontalLayout();
        scope1Form.setAlignItems(Alignment.END);
        TextField facField1 = new TextField("Factory ID", "FAC-GIGA-01");
        ComboBox<String> fuelBox = new ComboBox<>("Fuel Type", "Natural Gas (m3)", "Diesel Fuel (L)", "Propane (kg)");
        fuelBox.setValue("Natural Gas (m3)");
        NumberField fuelQtyField = new NumberField("Fuel Quantity");
        fuelQtyField.setValue(12000.0);
        NumberField factorField1 = new NumberField("Factor (kg CO2e/unit)");
        factorField1.setValue(1.93);

        Button logScope1Btn = new Button("Record Scope 1", e -> {
            if (facField1.getValue() == null || fuelBox.getValue() == null || fuelQtyField.getValue() == null || factorField1.getValue() == null) {
                Notification.show("Please fill Scope 1 fields", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CarbonEmissionRecord r = carbonService.recordScope1Combustion(
                    ws,
                    new NodeId(facField1.getValue()),
                    fuelBox.getValue(),
                    fuelQtyField.getValue(),
                    fuelBox.getValue().contains("m3") ? "m3" : "L",
                    factorField1.getValue()
            );
            Notification.show("Recorded " + String.format("%.2f t", r.getTotalEmissionsTons()) + " Scope 1 for " + facField1.getValue(), 3000, Notification.Position.TOP_CENTER);
            refreshGrid(grid, ws);
        });
        logScope1Btn.getElement().getThemeList().add("error");
        scope1Form.add(facField1, fuelBox, fuelQtyField, factorField1, logScope1Btn);

        // Scope 2 Section
        HorizontalLayout scope2Form = new HorizontalLayout();
        scope2Form.setAlignItems(Alignment.END);
        TextField facField2 = new TextField("Factory ID", "FAC-GIGA-01");
        NumberField kwhField = new NumberField("Grid Electricity (kWh)");
        kwhField.setValue(150000.0);
        NumberField gridFactorField = new NumberField("Grid Factor (kg CO2/kWh)");
        gridFactorField.setValue(0.385);

        Button logScope2Btn = new Button("Record Scope 2", e -> {
            if (facField2.getValue() == null || kwhField.getValue() == null || gridFactorField.getValue() == null) {
                Notification.show("Please fill Scope 2 fields", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CarbonEmissionRecord r = carbonService.recordScope2Electricity(
                    ws,
                    new NodeId(facField2.getValue()),
                    kwhField.getValue(),
                    gridFactorField.getValue()
            );
            Notification.show("Recorded " + String.format("%.2f t", r.getTotalEmissionsTons()) + " Scope 2 for " + facField2.getValue(), 3000, Notification.Position.TOP_CENTER);
            refreshGrid(grid, ws);
        });
        logScope2Btn.getElement().getThemeList().add("warning");
        scope2Form.add(facField2, kwhField, gridFactorField, logScope2Btn);

        add(new H3("Direct Stationary Combustion (Scope 1)"), scope1Form);
        add(new H3("Purchased Electricity & Steam (Scope 2)"), scope2Form);

        add(new H3("Plant-Level Manufacturing Carbon Inventory"));
        grid.addColumn(r -> r.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> r.facilityOrLaneId().value()).setHeader("Facility").setAutoWidth(true);
        grid.addColumn(r -> r.scope().name()).setHeader("Scope").setAutoWidth(true);
        grid.addColumn(CarbonEmissionRecord::activityCategory).setHeader("Activity Source").setAutoWidth(true);
        grid.addColumn(r -> r.activityAmount() + " " + r.unitOfMeasure()).setHeader("Usage").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.3f t CO2e", r.getTotalEmissionsTons())).setHeader("Emissions").setAutoWidth(true);
        grid.addColumn(r -> r.recordedAt().toString()).setHeader("Logged At").setAutoWidth(true);

        refreshGrid(grid, ws);
        grid.setHeight("350px");
        add(grid);
    }

    private void refreshGrid(Grid<CarbonEmissionRecord> grid, WorkspaceId ws) {
        List<CarbonEmissionRecord> mfgRecords = carbonService.listRecords(ws).stream()
                .filter(r -> r.scope() == EmissionScope.SCOPE_1_DIRECT || r.scope() == EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY)
                .toList();
        grid.setItems(mfgRecords);
    }
}
