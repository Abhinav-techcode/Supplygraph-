package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.SupplierSustainabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "sustainability/esg", layout = MainLayout.class)
@PageTitle("Supplier Sustainability & ESG | SupplyGraph")
public class SupplierSustainabilityDashboardView extends SustainabilityViewBase {

    private final SupplierSustainabilityService esgService;

    public SupplierSustainabilityDashboardView(SupplierSustainabilityService esgService) {
        super("Supplier Sustainability & ESG Audit Centre", "Comprehensive ESG evaluation across multi-tier suppliers, EUDR deforestation diligence, and CBAM carbon border compliance.");
        this.esgService = esgService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        List<SupplierEsgScorecard> allCards = esgService.listScorecards(ws);
        long cbamCount = allCards.stream().filter(SupplierEsgScorecard::cbamCompliant).count();
        long eudrCount = allCards.stream().filter(SupplierEsgScorecard::eudrCompliant).count();
        long conflictCount = allCards.stream().filter(SupplierEsgScorecard::conflictMineralsCertified).count();

        kpiRow.add(createKpiCard("Audited Suppliers", String.valueOf(allCards.size()), "Registered ESG scorecards", "contrast"));
        kpiRow.add(createKpiCard("CBAM Compliant", cbamCount + " / " + allCards.size(), "EU Carbon Border Adjustment", "primary"));
        kpiRow.add(createKpiCard("EUDR Deforestation-Free", eudrCount + " / " + allCards.size(), "EU Deforestation Regulation", "success"));
        kpiRow.add(createKpiCard("Conflict-Free Certified", conflictCount + " / " + allCards.size(), "3TG Responsible Minerals", "warning"));

        add(kpiRow);

        add(new H3("Record Supplier ESG Audit"));
        HorizontalLayout auditForm = new HorizontalLayout();
        auditForm.setAlignItems(Alignment.END);

        TextField supplierIdField = new TextField("Supplier ID", "SUP-ANODE-CATL");
        NumberField envField = new NumberField("Env Score (0-100)");
        envField.setValue(85.0);
        NumberField socField = new NumberField("Soc Score (0-100)");
        socField.setValue(80.0);
        NumberField govField = new NumberField("Gov Score (0-100)");
        govField.setValue(90.0);

        Checkbox cbamBox = new Checkbox("CBAM Compliant", true);
        Checkbox eudrBox = new Checkbox("EUDR Compliant", true);
        Checkbox conflictBox = new Checkbox("Conflict Minerals Certified", true);

        Grid<SupplierEsgScorecard> grid = new Grid<>();

        Button saveAuditBtn = new Button("Submit Audit Scorecard", e -> {
            if (supplierIdField.getValue() == null || envField.getValue() == null || socField.getValue() == null || govField.getValue() == null) {
                Notification.show("Please enter all required audit metrics", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            SupplierEsgScorecard card = esgService.recordEsgAudit(
                    ws,
                    new NodeId(supplierIdField.getValue()),
                    envField.getValue(),
                    socField.getValue(),
                    govField.getValue(),
                    cbamBox.getValue(),
                    conflictBox.getValue(),
                    eudrBox.getValue()
            );
            Notification.show("Recorded ESG Scorecard for " + supplierIdField.getValue() + " (Composite: " + String.format("%.1f", card.compositeEsgScore()) + ")", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(esgService.listScorecards(ws));
        });
        saveAuditBtn.getElement().getThemeList().add("primary");

        auditForm.add(supplierIdField, envField, socField, govField, cbamBox, eudrBox, conflictBox, saveAuditBtn);
        add(auditForm);

        add(new H3("Supplier ESG Performance Ledger"));
        grid.addColumn(s -> s.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(s -> s.supplierId().value()).setHeader("Supplier ID").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f", s.environmentalScore())).setHeader("Environmental (40%)").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f", s.socialScore())).setHeader("Social (30%)").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f", s.governanceScore())).setHeader("Governance (30%)").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span badge = new Span(String.format("%.1f", s.compositeEsgScore()));
            if (s.compositeEsgScore() >= 80.0) {
                badge.getElement().getThemeList().add("badge success");
            } else if (s.compositeEsgScore() >= 60.0) {
                badge.getElement().getThemeList().add("badge warning");
            } else {
                badge.getElement().getThemeList().add("badge error");
            }
            return badge;
        }).setHeader("Composite ESG").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.cbamCompliant() ? "COMPLIANT" : "NON-COMPLIANT");
            badge.getElement().getThemeList().add("badge small " + (s.cbamCompliant() ? "success" : "error"));
            return badge;
        }).setHeader("CBAM").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.eudrCompliant() ? "COMPLIANT" : "NON-COMPLIANT");
            badge.getElement().getThemeList().add("badge small " + (s.eudrCompliant() ? "success" : "error"));
            return badge;
        }).setHeader("EUDR").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.conflictMineralsCertified() ? "CERTIFIED" : "UNCERTIFIED");
            badge.getElement().getThemeList().add("badge small " + (s.conflictMineralsCertified() ? "success" : "contrast"));
            return badge;
        }).setHeader("Conflict 3TG").setAutoWidth(true);

        grid.setItems(allCards);
        grid.setHeight("350px");
        add(grid);
    }
}
