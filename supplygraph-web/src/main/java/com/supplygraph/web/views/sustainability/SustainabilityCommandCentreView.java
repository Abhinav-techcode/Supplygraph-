package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.CarbonAccountingService;
import com.supplygraph.application.sustainability.CircularEconomyService;
import com.supplygraph.application.sustainability.EnvironmentalScenarioService;
import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.application.sustainability.SupplierSustainabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.CarbonEmissionRecord;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
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
import java.util.Map;

@Route(value = "sustainability", layout = MainLayout.class)
@PageTitle("Sustainability Command Centre | SupplyGraph")
public class SustainabilityCommandCentreView extends SustainabilityViewBase {

    private final CarbonAccountingService carbonService;
    private final ProductTraceabilityService traceabilityService;
    private final SupplierSustainabilityService esgService;
    private final CircularEconomyService circularService;
    private final EnvironmentalScenarioService scenarioService;

    public SustainabilityCommandCentreView(
            CarbonAccountingService carbonService,
            ProductTraceabilityService traceabilityService,
            SupplierSustainabilityService esgService,
            CircularEconomyService circularService,
            EnvironmentalScenarioService scenarioService
    ) {
        super("Sustainability & Carbon Command Centre", "Enterprise GHG Scope 1/2/3 intelligence, multi-tier product traceability DAGs, circular recycling, and EUDR/CBAM compliance.");
        this.carbonService = carbonService;
        this.traceabilityService = traceabilityService;
        this.esgService = esgService;
        this.circularService = circularService;
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();

        Map<EmissionScope, Double> scopeEmissions = carbonService.getEmissionsByScope(ws);
        double totalEmissions = scopeEmissions.values().stream().mapToDouble(Double::doubleValue).sum();
        double avoidedVirginKg = circularService.getTotalVirginMaterialAvoidedKg(ws);
        List<SupplierEsgScorecard> scorecards = esgService.listScorecards(ws);
        double avgEsg = scorecards.isEmpty() ? 0.0 : scorecards.stream().mapToDouble(SupplierEsgScorecard::compositeEsgScore).average().orElse(0.0);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Total GHG Footprint", String.format("%.2f t", totalEmissions), "Scope 1 + 2 + 3 CO2e", "warning"));
        kpiRow.add(createKpiCard("Avoided Virgin Material", String.format("%.1f kg", avoidedVirginKg), "Circular closed-loop recovery", "success"));
        kpiRow.add(createKpiCard("Supplier ESG Health", String.format("%.1f / 100", avgEsg), "Multi-tier supplier audits", "primary"));
        kpiRow.add(createKpiCard("Genealogy Nodes", String.valueOf(traceabilityService.getOrCreateDag(ws).getAllNodes().size()), "Tracked batch/lot artifacts", "contrast"));

        add(kpiRow);

        add(new H3("Recent Carbon Emission Ledger Entries"));
        Grid<CarbonEmissionRecord> carbonGrid = new Grid<>();
        carbonGrid.addColumn(r -> r.id().value()).setHeader("ID").setAutoWidth(true);
        carbonGrid.addColumn(r -> r.facilityOrLaneId().value()).setHeader("Facility / Lane").setAutoWidth(true);
        carbonGrid.addComponentColumn(r -> {
            Span badge = new Span(r.scope().name());
            badge.getElement().getThemeList().add("badge small " + (r.scope() == EmissionScope.SCOPE_1_DIRECT ? "error" : r.scope() == EmissionScope.SCOPE_2_INDIRECT_ELECTRICITY ? "warning" : "primary"));
            return badge;
        }).setHeader("Scope").setAutoWidth(true);
        carbonGrid.addColumn(CarbonEmissionRecord::activityCategory).setHeader("Activity").setAutoWidth(true);
        carbonGrid.addColumn(r -> r.activityAmount() + " " + r.unitOfMeasure()).setHeader("Activity Volume").setAutoWidth(true);
        carbonGrid.addColumn(r -> String.format("%.3f t CO2e", r.getTotalEmissionsTons())).setHeader("Emissions").setAutoWidth(true);

        List<CarbonEmissionRecord> recentCarbon = carbonService.listRecords(ws);
        carbonGrid.setItems(recentCarbon);
        carbonGrid.setHeight("250px");

        add(carbonGrid);

        add(new H3("Recent Circular Economy Recovery Entries"));
        Grid<CircularLedgerEntry> circularGrid = new Grid<>();
        circularGrid.addColumn(e -> e.id().value()).setHeader("ID").setAutoWidth(true);
        circularGrid.addColumn(e -> e.materialId().value()).setHeader("Material").setAutoWidth(true);
        circularGrid.addColumn(e -> e.flowType().name()).setHeader("Flow Type").setAutoWidth(true);
        circularGrid.addColumn(e -> e.intakeQuantity() + " kg").setHeader("Intake Qty").setAutoWidth(true);
        circularGrid.addColumn(e -> e.recoveryYieldPercentage() + "%").setHeader("Yield").setAutoWidth(true);
        circularGrid.addColumn(e -> String.format("%.1f kg", e.recoveredQuantity())).setHeader("Recovered Qty").setAutoWidth(true);
        circularGrid.addColumn(e -> String.format("%.1f kg", e.virginMaterialAvoidedKg())).setHeader("Virgin Material Avoided").setAutoWidth(true);

        List<CircularLedgerEntry> circularEntries = circularService.listLedgerEntries(ws);
        circularGrid.setItems(circularEntries);
        circularGrid.setHeight("220px");

        add(circularGrid);

        HorizontalLayout navButtons = new HorizontalLayout();
        navButtons.add(new Button("Carbon Footprint", VaadinIcon.GLOBE.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/carbon"))));
        navButtons.add(new Button("Supplier ESG", VaadinIcon.USERS.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/esg"))));
        navButtons.add(new Button("Product Genealogy", VaadinIcon.CONNECT_O.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/traceability/genealogy"))));
        navButtons.add(new Button("Recall Impact", VaadinIcon.WARNING.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/traceability/recall"))));
        navButtons.add(new Button("Circular Economy", VaadinIcon.RECYCLE.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/circular"))));
        navButtons.add(new Button("Abatement Scenarios", VaadinIcon.SLIDERS.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/scenarios"))));

        add(navButtons);
    }
}
