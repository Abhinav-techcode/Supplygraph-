package com.supplygraph.web.views.workspace;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/supplier", layout = MainLayout.class)
@PageTitle("Supplier Partner Workspace | ORBIX")
public class SupplierPartnerWorkspaceView extends WorkspaceViewBase {

    private final OrderCollaborationService orderService;
    private final RfqCollaborationService rfqService;
    private final PartnerDocumentExchangeService docService;
    private final SupplierPortalService portalService;

    public SupplierPartnerWorkspaceView(
            OrderCollaborationService orderService,
            RfqCollaborationService rfqService,
            PartnerDocumentExchangeService docService,
            SupplierPortalService portalService
    ) {
        super("Supplier Partner Portal Gateway", "External partner collaboration interface for purchase order fulfillment, quotation bidding, and compliance evidence delivery.", "Role :: External Supplier Partner");
        this.orderService = orderService;
        this.rfqService = rfqService;
        this.docService = docService;
        this.portalService = portalService;

        WorkspaceId ws = currentWorkspaceId();
        List<PoCollaborationRecord> pos = orderService.listPos(ws);
        List<RfqDocument> rfqs = rfqService.listRfqs(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        long openOrders = pos.stream().filter(p -> p.status() == PoStatus.ISSUED || p.status() == PoStatus.ACKNOWLEDGED).count();

        kpiRow.add(createKpiCard("Orders for Fulfillment", String.valueOf(openOrders), "Actionable PO commitments", "primary"));
        kpiRow.add(createKpiCard("Open RFQ Tenders", String.valueOf(rfqs.size()), "Bidding opportunities", "contrast"));
        kpiRow.add(createKpiCard("Dispatched ASNs", String.valueOf(orderService.listAsns(ws).size()), "Consignments in transit", "success"));
        kpiRow.add(createKpiCard("Uploaded Evidence", String.valueOf(docService.listDocuments(ws).size()), "Certificates of Analysis", "warning"));

        add(kpiRow);

        add(new H3("Actionable Purchase Orders"));
        Grid<PoCollaborationRecord> grid = new Grid<>();
        grid.addColumn(PoCollaborationRecord::poNumber).setHeader("PO #").setAutoWidth(true);
        grid.addColumn(p -> p.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(p -> p.orderQuantity() + " units").setHeader("Quantity").setAutoWidth(true);
        grid.addColumn(p -> "$" + p.totalAmountUsd()).setHeader("Value").setAutoWidth(true);
        grid.addColumn(p -> p.promisedDeliveryDate().toString()).setHeader("Promised Delivery").setAutoWidth(true);
        grid.addColumn(p -> p.status().name()).setHeader("Status").setAutoWidth(true);

        grid.setItems(pos);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("PO Acknowledgment Hub", VaadinIcon.CHECK_SQUARE_O.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/orders"))));
        actionRow.add(new Button("Submit RFQ Quote", VaadinIcon.MONEY_DEPOSIT.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/rfq/submit"))));
        actionRow.add(new Button("Dispatch Delivery ASN", VaadinIcon.TRUCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/deliveries"))));
        actionRow.add(new Button("Upload Quality CoA", VaadinIcon.UPLOAD.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/evidence"))));

        add(grid, actionRow);
    }
}

