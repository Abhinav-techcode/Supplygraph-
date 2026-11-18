package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@Route(value = "collaboration/rfq", layout = MainLayout.class)
@PageTitle("RFQ Management Workspace | SupplyGraph")
public class RfqManagementWorkspaceView extends CollaborationViewBase {

    private final RfqCollaborationService rfqService;

    public RfqManagementWorkspaceView(RfqCollaborationService rfqService) {
        super("RFQ Management Workspace", "Strategic sourcing requests for quotations, vendor package dissemination, and tender lifecycle governance.");
        this.rfqService = rfqService;

        WorkspaceId ws = currentWorkspaceId();
        List<RfqDocument> rfqs = rfqService.listRfqs(ws);

        add(new H3("Publish New Request For Quotation (RFQ)"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField titleField = new TextField("RFQ Title", "2026 Solid-State Electrolyte Bulk Tender");
        TextField matField = new TextField("Material ID", "MAT-SOLID-ELECTROLYTE");
        NumberField qtyField = new NumberField("Required Volume");
        qtyField.setValue(15000.0);
        DatePicker deadlinePicker = new DatePicker("Response Deadline", LocalDate.now().plusDays(25));

        Grid<RfqDocument> grid = new Grid<>();

        Button createBtn = new Button("Publish RFQ", e -> {
            if (titleField.getValue() == null || matField.getValue() == null || qtyField.getValue() == null || deadlinePicker.getValue() == null) {
                Notification.show("Please complete all RFQ fields", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            RfqDocument doc = rfqService.createRfq(
                    ws,
                    titleField.getValue(),
                    new MaterialId(matField.getValue()),
                    qtyField.getValue(),
                    deadlinePicker.getValue()
            );
            Notification.show("Published RFQ: " + doc.title() + " (" + doc.id().value() + ")", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(rfqService.listRfqs(ws));
        });
        createBtn.getElement().getThemeList().add("primary");

        form.add(titleField, matField, qtyField, deadlinePicker, createBtn);
        add(form);

        add(new H3("Active & Historical RFQs"));
        grid.addColumn(r -> r.id().value()).setHeader("RFQ ID").setAutoWidth(true);
        grid.addColumn(RfqDocument::title).setHeader("Title").setAutoWidth(true);
        grid.addColumn(r -> r.targetMaterialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(r -> r.targetQuantity() + " units").setHeader("Target Qty").setAutoWidth(true);
        grid.addColumn(r -> r.submissionDeadline().toString()).setHeader("Deadline").setAutoWidth(true);
        grid.addComponentColumn(r -> {
            Span badge = new Span(r.status().name());
            badge.getElement().getThemeList().add("badge small " + (r.status() == RfqStatus.AWARDED ? "success" : r.status() == RfqStatus.PUBLISHED ? "primary" : "contrast"));
            return badge;
        }).setHeader("Status").setAutoWidth(true);
        grid.addColumn(r -> r.quotations().size() + " quotes").setHeader("Bids Received").setAutoWidth(true);
        grid.addColumn(r -> r.createdAt().toString()).setHeader("Created At").setAutoWidth(true);

        grid.setItems(rfqs);
        grid.setHeight("400px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Compare Quotations", e -> getUI().ifPresent(ui -> ui.navigate("collaboration/rfq/compare"))));
        add(grid, actionRow);
    }
}

