package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.PoStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@Route(value = "portal/deliveries", layout = MainLayout.class)
@PageTitle("Delivery Confirmation & ASNs | SupplyGraph")
public class DeliveryConfirmationCentreView extends CollaborationViewBase {

    private final OrderCollaborationService orderService;

    public DeliveryConfirmationCentreView(OrderCollaborationService orderService) {
        super("Delivery Confirmation & ASN Dispatch Centre", "Outbound shipping manifestation, barcode tracking, carrier telemetry, and receiving dock pre-notification.");
        this.orderService = orderService;

        WorkspaceId ws = currentWorkspaceId();
        List<PoCollaborationRecord> openPos = orderService.listPos(ws).stream()
                .filter(p -> p.status() == PoStatus.ISSUED || p.status() == PoStatus.ACKNOWLEDGED)
                .toList();
        List<AdvanceShippingNotice> asns = orderService.listAsns(ws);

        add(new H3("Dispatch Advance Shipping Notice (ASN)"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        ComboBox<PoCollaborationRecord> poBox = new ComboBox<>("Linked Purchase Order");
        poBox.setItems(openPos);
        poBox.setItemLabelGenerator(p -> p.poNumber() + " (" + p.materialId().value() + " - " + p.orderQuantity() + " units)");
        poBox.setWidth("350px");
        if (!openPos.isEmpty()) poBox.setValue(openPos.get(0));

        TextField carrierField = new TextField("Carrier Name", "Schenker Logistics");
        TextField trackingField = new TextField("Tracking / Waybill #", "DB-TRACK-99220011");
        NumberField shippedQtyField = new NumberField("Shipped Qty");
        shippedQtyField.setValue(5000.0);
        DatePicker etaPicker = new DatePicker("Estimated Arrival (ETA)", LocalDate.now().plusDays(4));

        Grid<AdvanceShippingNotice> grid = new Grid<>();

        Button dispatchBtn = new Button("Dispatch ASN", e -> {
            if (poBox.getValue() == null || carrierField.getValue() == null || trackingField.getValue() == null || shippedQtyField.getValue() == null || etaPicker.getValue() == null) {
                Notification.show("Please enter all shipping parameters", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            AdvanceShippingNotice asn = orderService.dispatchAsn(
                    ws,
                    poBox.getValue().id(),
                    trackingField.getValue(),
                    carrierField.getValue(),
                    shippedQtyField.getValue(),
                    etaPicker.getValue()
            );
            Notification.show("Dispatched ASN " + asn.id().value() + " with tracking " + asn.trackingNumber(), 4000, Notification.Position.TOP_CENTER);
            grid.setItems(orderService.listAsns(ws));
        });
        dispatchBtn.getElement().getThemeList().add("primary");

        form.add(poBox, carrierField, trackingField, shippedQtyField, etaPicker, dispatchBtn);
        add(form);

        add(new H3("Dispatched Advance Shipping Notices"));
        grid.addColumn(a -> a.id().value()).setHeader("ASN ID").setAutoWidth(true);
        grid.addColumn(a -> a.poId().value()).setHeader("PO ID").setAutoWidth(true);
        grid.addColumn(AdvanceShippingNotice::carrierName).setHeader("Logistics Carrier").setAutoWidth(true);
        grid.addColumn(AdvanceShippingNotice::trackingNumber).setHeader("Waybill / Tracking #").setAutoWidth(true);
        grid.addColumn(a -> a.shippedQuantity() + " units").setHeader("Shipped Volume").setAutoWidth(true);
        grid.addColumn(a -> a.estimatedArrivalDate().toString()).setHeader("Estimated Arrival (ETA)").setAutoWidth(true);
        grid.addColumn(a -> a.dispatchedAt().toString()).setHeader("Dispatched Timestamp").setAutoWidth(true);

        grid.setItems(asns);
        grid.setHeight("350px");

        add(grid);
    }
}

