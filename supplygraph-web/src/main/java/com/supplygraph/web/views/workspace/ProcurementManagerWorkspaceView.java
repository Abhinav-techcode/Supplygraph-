package com.supplygraph.web.views.workspace;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.application.planning.ProcurementPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
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

@Route(value = "workspace/procurement", layout = MainLayout.class)
@PageTitle("Procurement Manager Workspace | ORBIX")
public class ProcurementManagerWorkspaceView extends WorkspaceViewBase {

    private final ProcurementPlanningService procurementService;
    private final RfqCollaborationService rfqService;
    private final SupplierPortalService portalService;
    private final OrderCollaborationService orderService;

    public ProcurementManagerWorkspaceView(
            ProcurementPlanningService procurementService,
            RfqCollaborationService rfqService,
            SupplierPortalService portalService,
            OrderCollaborationService orderService
    ) {
        super("Procurement Sourcing Cockpit", "Strategic direct-material sourcing, RFQ tender awards, dual-sourcing allocation splits, and supplier PO collaboration.", "Role :: Procurement Manager");
        this.procurementService = procurementService;
        this.rfqService = rfqService;
        this.portalService = portalService;
        this.orderService = orderService;

        WorkspaceId ws = currentWorkspaceId();
        List<RfqDocument> rfqs = rfqService.listRfqs(ws);
        List<PoCollaborationRecord> pos = orderService.listPos(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Active RFQs", String.valueOf(rfqs.size()), "Direct material tenders", "primary"));
        kpiRow.add(createKpiCard("Connected Suppliers", String.valueOf(portalService.listPartners(ws).size()), "Onboarded vendor base", "success"));
        kpiRow.add(createKpiCard("PO Commitments", String.valueOf(pos.size()), "Open order collaboration", "warning"));
        kpiRow.add(createKpiCard("Dual-Sourcing Split", "No data", "No sourcing-split source connected", "contrast"));

        add(kpiRow);

        add(new H3("Direct Material Purchase Orders"));
        Grid<PoCollaborationRecord> grid = new Grid<>();
        grid.addColumn(PoCollaborationRecord::poNumber).setHeader("PO #").setAutoWidth(true);
        grid.addColumn(p -> p.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        grid.addColumn(p -> p.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(p -> p.orderQuantity() + " units").setHeader("Order Qty").setAutoWidth(true);
        grid.addColumn(p -> "$" + p.totalAmountUsd()).setHeader("Spend Amount").setAutoWidth(true);
        grid.addColumn(p -> p.promisedDeliveryDate().toString()).setHeader("Delivery Promise").setAutoWidth(true);
        grid.addColumn(p -> p.status().name()).setHeader("Status").setAutoWidth(true);

        grid.setItems(pos);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("RFQ Management", VaadinIcon.FILE_TEXT.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/rfq"))));
        actionRow.add(new Button("Compare Quotes", VaadinIcon.DASHBOARD.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/rfq/compare"))));
        actionRow.add(new Button("Supplier Directory", VaadinIcon.BUILDING.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/organization"))));
        actionRow.add(new Button("Procurement Explosion", VaadinIcon.CART.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/procurement"))));

        add(grid, actionRow);
    }
}

