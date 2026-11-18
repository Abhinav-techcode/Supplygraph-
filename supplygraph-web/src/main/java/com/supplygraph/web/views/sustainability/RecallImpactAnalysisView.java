package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.RecallImpactResult;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "sustainability/traceability/recall", layout = MainLayout.class)
@PageTitle("Recall Impact Analysis | SupplyGraph")
public class RecallImpactAnalysisView extends SustainabilityViewBase {

    private final ProductTraceabilityService traceabilityService;

    public RecallImpactAnalysisView(ProductTraceabilityService traceabilityService) {
        super("Recall Impact Analysis & Blast Radius", "Forward DAG blast-radius evaluation to isolate contaminated materials, compromised lots, and outbound customer shipments.");
        this.traceabilityService = traceabilityService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout searchRow = new HorizontalLayout();
        searchRow.setAlignItems(Alignment.END);

        TextField batchField = new TextField("Tainted Batch Number");
        batchField.setPlaceholder("e.g. RAW-LI-2026-001");
        batchField.setWidth("350px");

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        VerticalLayout affectedVolCard = createKpiCard("Affected Volume", "0 units", "Total inventory compromised", "error");
        VerticalLayout affectedLocsCard = createKpiCard("Affected Facilities", "0 sites", "Warehouses & factories at risk", "warning");
        VerticalLayout shipmentsCard = createKpiCard("Outbound Consignments", "0 shipments", "Customer shipments impacted", "primary");

        kpiRow.add(affectedVolCard, affectedLocsCard, shipmentsCard);

        Grid<TraceabilityNode> affectedGrid = new Grid<>();
        affectedGrid.addColumn(n -> n.id().value()).setHeader("Trace ID").setAutoWidth(true);
        affectedGrid.addColumn(TraceabilityNode::batchNumber).setHeader("Compromised Batch").setAutoWidth(true);
        affectedGrid.addComponentColumn(n -> {
            Span badge = new Span(n.type().name());
            badge.getElement().getThemeList().add("badge small error");
            return badge;
        }).setHeader("Stage").setAutoWidth(true);
        affectedGrid.addColumn(n -> n.materialId().value()).setHeader("Material").setAutoWidth(true);
        affectedGrid.addColumn(n -> n.locationId().value()).setHeader("Location").setAutoWidth(true);
        affectedGrid.addColumn(n -> n.quantity() + " units").setHeader("Compromised Qty").setAutoWidth(true);
        affectedGrid.addColumn(n -> n.recordedAt().toString()).setHeader("Production Date").setAutoWidth(true);
        affectedGrid.setHeight("350px");

        Button analyzeBtn = new Button("Execute Blast-Radius Analysis", e -> {
            String batch = batchField.getValue();
            if (batch == null || batch.isBlank()) {
                Notification.show("Please enter a tainted batch number", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            try {
                RecallImpactResult result = traceabilityService.executeRecallImpactAnalysis(ws, batch.trim());
                affectedGrid.setItems(result.affectedDownstreamNodes());

                kpiRow.removeAll();
                kpiRow.add(createKpiCard("Affected Volume", String.format("%.1f units", result.totalCompromisedVolume()), "Total inventory compromised", "error"));
                kpiRow.add(createKpiCard("Affected Facilities", result.impactedFacilityLocations().size() + " sites", "Warehouses & factories at risk", "warning"));
                kpiRow.add(createKpiCard("Outbound Consignments", result.impactedCustomerShipments().size() + " shipments", "Customer shipments impacted", "primary"));

                Notification.show("Blast radius evaluated: " + result.affectedDownstreamNodes().size() + " downstream batches contaminated!", 4000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification.show("Analysis failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
            }
        });
        analyzeBtn.getElement().getThemeList().add("error");

        searchRow.add(batchField, analyzeBtn);

        add(searchRow, kpiRow, new H3("Downstream Contaminated Lots & Deliveries"), affectedGrid);
    }
}
