package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.collaboration.RfqQuotation;
import com.supplygraph.domain.model.collaboration.RfqStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Route(value = "portal/rfq/submit", layout = MainLayout.class)
@PageTitle("Submit RFQ Quotation | SupplyGraph")
public class SupplierQuotationSubmissionView extends CollaborationViewBase {

    private final RfqCollaborationService rfqService;
    private final SupplierPortalService portalService;

    public SupplierQuotationSubmissionView(
            RfqCollaborationService rfqService,
            SupplierPortalService portalService
    ) {
        super("Supplier RFQ Quotation Submission", "External supplier tender bidding, commercial pricing matrix, lead time commitment, and validity binding.");
        this.rfqService = rfqService;
        this.portalService = portalService;

        WorkspaceId ws = currentWorkspaceId();
        List<RfqDocument> openRfqs = rfqService.listRfqs(ws).stream()
                .filter(r -> r.status() == RfqStatus.PUBLISHED)
                .toList();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        add(new H3("Submit Commercial Bid"));
        HorizontalLayout form1 = new HorizontalLayout();
        form1.setAlignItems(Alignment.END);

        ComboBox<RfqDocument> rfqBox = new ComboBox<>("Select Open RFQ Tender");
        rfqBox.setItems(openRfqs);
        rfqBox.setItemLabelGenerator(r -> r.title() + " (" + r.id().value() + ")");
        rfqBox.setWidth("380px");
        if (!openRfqs.isEmpty()) rfqBox.setValue(openRfqs.get(0));

        ComboBox<PartnerOrganization> supplierBox = new ComboBox<>("Bidding Supplier");
        supplierBox.setItems(partners);
        supplierBox.setItemLabelGenerator(p -> p.name() + " (" + p.id().value() + ")");
        if (!partners.isEmpty()) supplierBox.setValue(partners.get(0));

        form1.add(rfqBox, supplierBox);

        HorizontalLayout form2 = new HorizontalLayout();
        form2.setAlignItems(Alignment.END);

        BigDecimalField priceField = new BigDecimalField("Unit Price (USD)");
        priceField.setValue(BigDecimal.valueOf(18.50));

        IntegerField leadTimeField = new IntegerField("Lead Time (days)");
        leadTimeField.setValue(14);

        NumberField moqField = new NumberField("Minimum Order Qty (MOQ)");
        moqField.setValue(500.0);

        DatePicker validityPicker = new DatePicker("Price Validity Date", LocalDate.now().plusDays(45));

        Button submitBtn = new Button("Submit Binding Quotation", e -> {
            if (rfqBox.getValue() == null || supplierBox.getValue() == null || priceField.getValue() == null || leadTimeField.getValue() == null || moqField.getValue() == null || validityPicker.getValue() == null) {
                Notification.show("Please enter all commercial bid parameters", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            RfqQuotation quote = rfqService.submitQuotation(
                    ws,
                    rfqBox.getValue().id(),
                    supplierBox.getValue().id(),
                    priceField.getValue(),
                    leadTimeField.getValue(),
                    moqField.getValue(),
                    validityPicker.getValue()
            );
            Notification.show("Submitted quotation " + quote.id().value() + " for $" + quote.unitPriceUsd() + " / unit", 4000, Notification.Position.TOP_CENTER);
        });
        submitBtn.getElement().getThemeList().add("primary");

        form2.add(priceField, leadTimeField, moqField, validityPicker, submitBtn);

        add(form1, form2);
    }
}

