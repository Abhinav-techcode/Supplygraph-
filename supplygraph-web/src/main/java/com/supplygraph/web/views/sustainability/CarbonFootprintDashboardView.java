package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.CarbonAccountingService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;

@Route(value = "sustainability/carbon", layout = MainLayout.class)
@PageTitle("Carbon Footprint Dashboard | SupplyGraph")
public class CarbonFootprintDashboardView extends SustainabilityViewBase {

    private final CarbonAccountingService carbonService;

    public CarbonFootprintDashboardView(CarbonAccountingService carbonService) {
        super("Carbon Footprint Dashboard", "GHG Protocol Scope 1, 2, and 3 emissions accounting, activity data ingestion, and emission factor application.");
        this.carbonService = carbonService;

        WorkspaceId ws = currentWorkspaceId();
        Map<EmissionScope, Double> scopeEmissions = carbonService.getEmissionsByScope(ws);

        double s1 = scopeEmissions.getOrDefault(EmissionScope.SCOPE_1_DIRECT, 0.0);
        double s2 = scopeEmissions.getOrDefault(EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY, 0.0);
        double s3Freight = scopeEmissions.getOrDefault(EmissionScope.SCOPE_3_UPSTREAM_FREIGHT, 0.0);
        double s3Purchased = scopeEmissions.getOrDefault(EmissionScope.SCOPE_3_PURCHASED_GOODS, 0.0);
        double s3Total = s3Freight + s3Purchased;
        double total = s1 + s2 + s3Total;

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Scope 1 (Direct)", String.format("%.2f t", s1), total > 0 ? String.format("%.1f%% of total", (s1 / total) * 100) : "0%", "error"));
        kpiRow.add(createKpiCard("Scope 2 (Electricity)", String.format("%.2f t", s2), total > 0 ? String.format("%.1f%% of total", (s2 / total) * 100) : "0%", "warning"));
        kpiRow.add(createKpiCard("Scope 3 (Upstream)", String.format("%.2f t", s3Total), total > 0 ? String.format("%.1f%% of total", (s3Total / total) * 100) : "0%", "primary"));
        kpiRow.add(createKpiCard("Total Inventory", String.format("%.2f t CO2e", total), "Complete carbon accounting", "contrast"));

        add(kpiRow);

        add(new H3("Carbon Emissions Inventory"));

        List<CarbonEmissionRecord> allRecords = carbonService.listRecords(ws);

        Grid<CarbonEmissionRecord> grid = new Grid<>();
        grid.addColumn(r -> r.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(r -> r.facilityOrLaneId().value()).setHeader("Facility / Lane").setAutoWidth(true);
        grid.addComponentColumn(r -> {
            Span badge = new Span(r.scope().name());
            badge.getElement().getThemeList().add("badge small " + (r.scope() == EmissionScope.SCOPE_1_DIRECT ? "error" : r.scope() == EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY ? "warning" : "primary"));
            return badge;
        }).setHeader("Emission Scope").setAutoWidth(true);
        grid.addColumn(CarbonEmissionRecord::activityCategory).setHeader("Activity").setAutoWidth(true);
        grid.addColumn(r -> r.activityAmount() + " " + r.unitOfMeasure()).setHeader("Activity Amount").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.4f kg CO2e / unit", r.emissionFactorKgCo2e())).setHeader("Factor").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.3f t CO2e", r.getTotalEmissionsTons())).setHeader("Total Emissions (t)").setAutoWidth(true);
        grid.addColumn(r -> r.recordedAt().toString()).setHeader("Timestamp").setAutoWidth(true);

        grid.setItems(allRecords);
        grid.setHeight("400px");

        ComboBox<String> filterBox = new ComboBox<>("Filter by Scope");
        filterBox.setItems("ALL", "SCOPE_1_DIRECT", "SCOPE_2_INDIRECT_ELECTRICITY", "SCOPE_3_UPSTREAM_FREIGHT", "SCOPE_3_PURCHASED_GOODS");
        filterBox.setValue("ALL");
        filterBox.addValueChangeListener(e -> {
            String val = e.getValue();
            if (val == null || "ALL".equals(val)) {
                grid.setItems(allRecords);
            } else {
                grid.setItems(allRecords.stream().filter(r -> r.scope().name().equals(val)).toList());
            }
        });

        add(filterBox, grid);
    }
}
