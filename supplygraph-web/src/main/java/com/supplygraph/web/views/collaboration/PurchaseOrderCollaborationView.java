package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@Route(value = "portal/orders", layout = MainLayout.class)
@PageTitle("Purchase Order Collaboration | SupplyGraph")
public class PurchaseOrderCollaborationView extends CollaborationViewBase {

    private final OrderCollaborationService orderService;

    public PurchaseOrderCollaborationView(OrderCollaborationService orderService) {
        super("Purchase Order Collaboration Hub", "Real-time purchase order delivery promise confirmation, line-item scheduling, and fulfillment tracking.");
        this.orderService = orderService;

        WorkspaceId ws = currentWorkspaceId();
        List<PoCollaborationRecord> pos = orderService.listPos(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        long issuedCount = pos.stream().filter(p -> p.status() == PoStatus.ISSUED).count();
        long ackCount = pos.stream().filter(p -> p.status() == PoStatus.ACKNOWLEDGED).count();
        long shippedCount = pos.stream().filter(p -> p.status() == PoStatus.SHIPPED).count();

        kpiRow.add(createKpiCard("Awaiting Acknowledgment", String.valueOf(issuedCount), "Action required by supplier", "warning"));
        kpiRow.add(createKpiCard("Confirmed in Production", String.valueOf(ackCount), "Supplier promise locked", "primary"));
        kpiRow.add(createKpiCard("In Transit / Shipped", String.valueOf(shippedCount), "ASN linked shipments", "success"));
        kpiRow.add(createKpiCard("Total Active Orders", String.valueOf(pos.size()), "Total collaboration portfolio", "contrast"));

        add(kpiRow);

        add(new H3("Purchase Orders"));
        Grid<PoCollaborationRecord> grid = new Grid<>();
        grid.addColumn(PoCollaborationRecord::poNumber).setHeader("PO #").setAutoWidth(true);
        grid.addColumn(p -> p.supplierId().value()).setHeader("Supplier ID").setAutoWidth(true);
        grid.addColumn(p -> p.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(p -> p.orderQuantity() + " units").setHeader("Order Qty").setAutoWidth(true);
        grid.addColumn(p -> "$" + p.totalAmountUsd()).setHeader("Value").setAutoWidth(true);
        grid.addColumn(p -> p.promisedDeliveryDate().toString()).setHeader("Delivery Promise").setAutoWidth(true);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.status().name());
            badge.getElement().getThemeList().add("badge small " + switch (p.status()) {
                case ISSUED -> "warning";
                case ACKNOWLEDGED -> "primary";
                case SHIPPED, DELIVERED -> "success";
                case EXCEPTION -> "error";
                default -> "contrast";
            });
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        DatePicker promisePicker = new DatePicker("Confirmed Delivery Date", LocalDate.now().plusDays(10));
        Button ackBtn = new Button("Acknowledge Selected PO", e -> {
            PoCollaborationRecord selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("Please select a purchase order from the table", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            if (promisePicker.getValue() == null) {
                Notification.show("Please select confirmed delivery date", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            orderService.acknowledgePo(ws, selected.id(), promisePicker.getValue());
            Notification.show("Acknowledged PO " + selected.poNumber() + " with promise date: " + promisePicker.getValue(), 3000, Notification.Position.TOP_CENTER);
            grid.setItems(orderService.listPos(ws));
        });
        ackBtn.getElement().getThemeList().add("primary");

        HorizontalLayout actionRow = new HorizontalLayout(promisePicker, ackBtn);
        actionRow.setAlignItems(Alignment.END);

        grid.setItems(pos);
        grid.setHeight("400px");

        add(grid, actionRow);
    }
}

