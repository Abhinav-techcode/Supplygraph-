package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.CarbonAccountingService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.TransportMode;
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

@Route(value = "sustainability/carbon/transport", layout = MainLayout.class)
@PageTitle("Transportation Emissions Explorer | SupplyGraph")
public class TransportationEmissionsExplorerView extends SustainabilityViewBase {

    private final CarbonAccountingService carbonService;

    public TransportationEmissionsExplorerView(CarbonAccountingService carbonService) {
        super("Transportation Emissions Explorer", "Scope 3 Category 4 upstream freight carbon intensity analysis across transport modes and intermodal corridors.");
        this.carbonService = carbonService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Air Freight Intensity", "602.0 g/t-km", "Highest carbon velocity", "error"));
        kpiRow.add(createKpiCard("Road Freight Intensity", "62.0 g/t-km", "Standard trucking corridor", "warning"));
        kpiRow.add(createKpiCard("Rail Freight Intensity", "22.0 g/t-km", "Eco-friendly continental", "primary"));
        kpiRow.add(createKpiCard("Ocean Freight Intensity", "8.4 g/t-km", "Lowest carbon maritime", "success"));

        add(kpiRow);

        add(new H3("Calculate & Ingest Freight Emissions"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField laneField = new TextField("Transport Lane ID", "LANE-EUR-TRANSIT-01");
        ComboBox<TransportMode> modeBox = new ComboBox<>("Transport Mode");
        modeBox.setItems(TransportMode.values());
        modeBox.setValue(TransportMode.ROAD);

        NumberField distanceField = new NumberField("Distance (km)");
        distanceField.setValue(850.0);

        NumberField weightField = new NumberField("Cargo Weight (tons)");
        weightField.setValue(22.0);

        Grid<CarbonEmissionRecord> grid = new Grid<>();

        Button calcBtn = new Button("Record Freight Shipment", e -> {
            if (laneField.getValue() == null || modeBox.getValue() == null || distanceField.getValue() == null || weightField.getValue() == null) {
                Notification.show("Please enter all freight parameters", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CarbonEmissionRecord record = carbonService.recordScope3Freight(
                    ws,
                    new NodeId(laneField.getValue()),
                    modeBox.getValue(),
                    distanceField.getValue(),
                    weightField.getValue()
            );
            Notification.show("Logged " + String.format("%.3f t CO2e", record.getTotalEmissionsTons()) + " on " + laneField.getValue(), 3000, Notification.Position.TOP_CENTER);
            refreshGrid(grid, ws);
        });
        calcBtn.getElement().getThemeList().add("primary");

        form.add(laneField, modeBox, distanceField, weightField, calcBtn);
        add(form);

        add(new H3("Logged Upstream Freight Records"));
        grid.addColumn(r -> r.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> r.facilityOrLaneId().value()).setHeader("Lane ID").setAutoWidth(true);
        grid.addColumn(CarbonEmissionRecord::activityCategory).setHeader("Mode & Route").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f ton-km", r.activityAmount())).setHeader("Activity Volume").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.4f kg/t-km", r.emissionFactorKgCo2e())).setHeader("Factor").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.3f t CO2e", r.getTotalEmissionsTons())).setHeader("Emissions").setAutoWidth(true);
        grid.addColumn(r -> r.recordedAt().toString()).setHeader("Logged At").setAutoWidth(true);

        refreshGrid(grid, ws);
        grid.setHeight("350px");

        add(grid);
    }

    private void refreshGrid(Grid<CarbonEmissionRecord> grid, WorkspaceId ws) {
        List<CarbonEmissionRecord> freightRecords = carbonService.listRecords(ws).stream()
                .filter(r -> r.scope() == EmissionScope.SCOPE_3_UPSTREAM_FREIGHT)
                .toList();
        grid.setItems(freightRecords);
    }
}
