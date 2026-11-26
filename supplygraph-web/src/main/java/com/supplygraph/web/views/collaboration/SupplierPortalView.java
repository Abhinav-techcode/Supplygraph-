package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.RfqDocument;
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

@Route(value = "portal", layout = MainLayout.class)
@PageTitle("Supplier Portal Command Centre | ORBIX")
public class SupplierPortalView extends CollaborationViewBase {

    private final SupplierPortalService portalService;
    private final RfqCollaborationService rfqService;
    private final OrderCollaborationService orderService;
    private final SupplierQualityCollaborationService qualityService;
    private final PartnerDocumentExchangeService docService;

    public SupplierPortalView(
            SupplierPortalService portalService,
            RfqCollaborationService rfqService,
            OrderCollaborationService orderService,
            SupplierQualityCollaborationService qualityService,
            PartnerDocumentExchangeService docService
    ) {
        super("Supplier & Partner Collaboration Portal", "Unified external collaboration gateway for multi-tier suppliers, contract manufacturers, and logistics partners.");
        this.portalService = portalService;
        this.rfqService = rfqService;
        this.orderService = orderService;
        this.qualityService = qualityService;
        this.docService = docService;

        WorkspaceId ws = currentWorkspaceId();

        List<PoCollaborationRecord> pos = orderService.listPos(ws);
        long openOrders = pos.stream().filter(p -> p.status() == PoStatus.ISSUED || p.status() == PoStatus.ACKNOWLEDGED).count();
        List<RfqDocument> rfqs = rfqService.listRfqs(ws);
        List<QualityIncident8D> incidents = qualityService.listIncidents(ws);
        long openIncidents = incidents.stream().filter(i -> !i.closed()).count();
        int docCount = docService.listDocuments(ws).size();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Open Purchase Orders", String.valueOf(openOrders), "Awaiting fulfillment", "primary"));
        kpiRow.add(createKpiCard("Active RFQs", String.valueOf(rfqs.size()), "Bidding opportunities", "contrast"));
        kpiRow.add(createKpiCard("Active 8D Quality Issues", String.valueOf(openIncidents), "Corrective actions underway", "error"));
        kpiRow.add(createKpiCard("Verified Documents", String.valueOf(docCount), "Compliance & CoA vault", "success"));

        add(kpiRow);

        add(new H3("Recent Purchase Order Collaboration Requests"));
        Grid<PoCollaborationRecord> poGrid = new Grid<>();
        poGrid.addColumn(PoCollaborationRecord::poNumber).setHeader("PO #").setAutoWidth(true);
        poGrid.addColumn(p -> p.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        poGrid.addColumn(p -> p.materialId().value()).setHeader("Material").setAutoWidth(true);
        poGrid.addColumn(p -> p.orderQuantity() + " units").setHeader("Order Qty").setAutoWidth(true);
        poGrid.addColumn(p -> "$" + p.totalAmountUsd()).setHeader("Amount").setAutoWidth(true);
        poGrid.addColumn(p -> p.promisedDeliveryDate().toString()).setHeader("Promised Date").setAutoWidth(true);
        poGrid.addComponentColumn(p -> {
            Span badge = new Span(p.status().name());
            badge.getElement().getThemeList().add("badge small " + (p.status() == PoStatus.SHIPPED ? "success" : p.status() == PoStatus.ISSUED ? "warning" : "contrast"));
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        poGrid.setItems(pos);
        poGrid.setHeight("250px");
        add(poGrid);

        HorizontalLayout navRow = new HorizontalLayout();
        navRow.add(new Button("Purchase Orders", VaadinIcon.CART.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/orders"))));
        navRow.add(new Button("Submit RFQ Quote", VaadinIcon.MONEY_DEPOSIT.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/rfq/submit"))));
        navRow.add(new Button("Deliveries & ASNs", VaadinIcon.TRUCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/deliveries"))));
        navRow.add(new Button("Quality 8D Hub", VaadinIcon.DIPLOMA.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/quality"))));
        navRow.add(new Button("Document Vault", VaadinIcon.FILE_TEXT.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/documents"))));
        navRow.add(new Button("Organization Profile", VaadinIcon.BUILDING.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/organization"))));

        add(navRow);
    }
}

