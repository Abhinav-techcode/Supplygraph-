package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.CircularEconomyService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.sustainability.CircularFlowType;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
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

@Route(value = "sustainability/circular", layout = MainLayout.class)
@PageTitle("Circular Supply Chain & Recycling | ORBIX")
public class CircularSupplyChainDashboardView extends SustainabilityViewBase {

    private final CircularEconomyService circularService;

    public CircularSupplyChainDashboardView(CircularEconomyService circularService) {
        super("Circular Supply Chain & Material Recovery", "Closed-loop circularity ledger tracking secondary material recovery yields, battery recycling, and virgin mining avoidance.");
        this.circularService = circularService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        double totalVirginAvoided = circularService.getTotalVirginMaterialAvoidedKg(ws);
        List<CircularLedgerEntry> entries = circularService.listLedgerEntries(ws);
        double totalIntake = entries.stream().mapToDouble(CircularLedgerEntry::intakeQuantity).sum();
        double totalRecovered = entries.stream().mapToDouble(CircularLedgerEntry::recoveredQuantity).sum();
        double avgYield = totalIntake > 0 ? (totalRecovered / totalIntake) * 100.0 : 0.0;

        kpiRow.add(createKpiCard("Virgin Material Avoided", String.format("%.1f kg", totalVirginAvoided), "Secondary raw material offset", "success"));
        kpiRow.add(createKpiCard("Circular Feedstock Intake", String.format("%.1f kg", totalIntake), "End-of-life and scrap volume", "contrast"));
        kpiRow.add(createKpiCard("Recovered Secondary Output", String.format("%.1f kg", totalRecovered), "Re-integrated into BOM", "primary"));
        kpiRow.add(createKpiCard("Average Recovery Yield", String.format("%.1f%%", avgYield), "Refining & extraction efficiency", "warning"));

        add(kpiRow);

        add(new H3("Record Circular Recovery Run"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField matField = new TextField("Material ID", "MAT-BLACK-MASS-LI");
        ComboBox<CircularFlowType> typeBox = new ComboBox<>("Recovery Flow Type");
        typeBox.setItems(CircularFlowType.values());
        typeBox.setValue(CircularFlowType.BATTERY_CELL_RECYCLING);

        NumberField intakeField = new NumberField("Intake Feedstock (kg)");
        intakeField.setValue(5000.0);

        NumberField yieldField = new NumberField("Recovery Yield (%)");
        yieldField.setValue(94.5);

        Grid<CircularLedgerEntry> grid = new Grid<>();

        Button recordBtn = new Button("Record Circular Recovery", e -> {
            if (matField.getValue() == null || typeBox.getValue() == null || intakeField.getValue() == null || yieldField.getValue() == null) {
                Notification.show("Please enter all recovery details", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            CircularLedgerEntry entry = circularService.recordRecovery(
                    ws,
                    new MaterialId(matField.getValue()),
                    typeBox.getValue(),
                    intakeField.getValue(),
                    yieldField.getValue()
            );
            Notification.show("Logged recovery run: " + String.format("%.1f kg", entry.recoveredQuantity()) + " recovered!", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(circularService.listLedgerEntries(ws));
        });
        recordBtn.getElement().getThemeList().add("success");

        form.add(matField, typeBox, intakeField, yieldField, recordBtn);
        add(form);

        add(new H3("Circular Economy Ledger"));
        grid.addColumn(e -> e.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(e -> e.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(e -> e.flowType().name()).setHeader("Flow Type").setAutoWidth(true);
        grid.addColumn(e -> e.intakeQuantity() + " kg").setHeader("Intake Feedstock").setAutoWidth(true);
        grid.addColumn(e -> e.recoveryYieldPercentage() + "%").setHeader("Recovery Yield").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f kg", e.recoveredQuantity())).setHeader("Recovered Material").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f kg", e.virginMaterialAvoidedKg())).setHeader("Virgin Extraction Avoided").setAutoWidth(true);
        grid.addColumn(e -> e.recordedAt().toString()).setHeader("Logged Timestamp").setAutoWidth(true);

        grid.setItems(entries);
        grid.setHeight("350px");

        add(grid);
    }
}
