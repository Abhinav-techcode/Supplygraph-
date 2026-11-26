package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Route(value = "collaboration/activity", layout = MainLayout.class)
@PageTitle("Collaboration Activity & Audit Timeline | ORBIX")
public class CollaborationActivityTimelineView extends CollaborationViewBase {

    public record ActivityEvent(String eventType, String entityId, String description, String theme, Instant timestamp) {}

    public CollaborationActivityTimelineView(
            OrderCollaborationService orderService,
            RfqCollaborationService rfqService,
            SupplierQualityCollaborationService qualityService,
            PartnerDocumentExchangeService docService
    ) {
        super("Collaboration Activity & Audit Timeline", "Chronological audit stream of commercial bids, delivery acknowledgments, quality investigations, and attestations.");

        WorkspaceId ws = currentWorkspaceId();
        List<ActivityEvent> events = new ArrayList<>();

        for (PoCollaborationRecord po : orderService.listPos(ws)) {
            events.add(new ActivityEvent("PURCHASE_ORDER", po.poNumber(), "PO " + po.status().name() + " for " + po.materialId().value(), "primary", po.lastUpdated()));
        }

        for (AdvanceShippingNotice asn : orderService.listAsns(ws)) {
            events.add(new ActivityEvent("SHIPMENT_ASN", asn.trackingNumber(), "ASN dispatched by " + asn.carrierName() + " (" + asn.shippedQuantity() + " units)", "success", asn.dispatchedAt()));
        }

        for (RfqDocument rfq : rfqService.listRfqs(ws)) {
            events.add(new ActivityEvent("RFQ_TENDER", rfq.id().value(), "RFQ '" + rfq.title() + "' status: " + rfq.status().name(), "contrast", rfq.createdAt()));
        }

        for (QualityIncident8D inc : qualityService.listIncidents(ws)) {
            events.add(new ActivityEvent("QUALITY_8D", inc.id().value(), "8D step: " + inc.currentStep().name() + " (" + inc.defectTitle() + ")", "error", inc.reportedAt()));
        }

        for (PartnerDocumentRecord doc : docService.listDocuments(ws)) {
            events.add(new ActivityEvent("DOCUMENT_VAULT", doc.id().value(), "Document " + doc.fileName() + (doc.verified() ? " [VERIFIED]" : " [UPLOADED]"), "warning", doc.uploadedAt()));
        }

        events.sort(Comparator.comparing(ActivityEvent::timestamp).reversed());

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Total Collaboration Events", String.valueOf(events.size()), "Audit trail entries", "primary"));
        kpiRow.add(createKpiCard("Shipment ASNs", String.valueOf(orderService.listAsns(ws).size()), "Dispatched consignments", "success"));
        kpiRow.add(createKpiCard("8D Corrective Actions", String.valueOf(qualityService.listIncidents(ws).size()), "Quality problem tickets", "error"));
        kpiRow.add(createKpiCard("Vaulted Documents", String.valueOf(docService.listDocuments(ws).size()), "Immutable evidence items", "warning"));

        add(kpiRow);

        add(new H3("Chronological Collaboration Event Log"));
        Grid<ActivityEvent> grid = new Grid<>();
        grid.addComponentColumn(e -> {
            Span badge = new Span(e.eventType());
            badge.getElement().getThemeList().add("badge small " + e.theme());
            return badge;
        }).setHeader("Category").setAutoWidth(true);
        grid.addColumn(ActivityEvent::entityId).setHeader("Entity ID / Reference").setAutoWidth(true);
        grid.addColumn(ActivityEvent::description).setHeader("Event Description").setAutoWidth(true);
        grid.addColumn(e -> e.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);

        grid.setItems(events);
        grid.setHeight("450px");

        add(grid);
    }
}
