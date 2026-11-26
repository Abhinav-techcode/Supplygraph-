package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.DocumentCategory;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "portal/evidence", layout = MainLayout.class)
@PageTitle("Supplier Evidence & Assurance Centre | ORBIX")
public class SupplierEvidenceCentreView extends CollaborationViewBase {

    private final SupplierPortalService portalService;
    private final PartnerDocumentExchangeService docService;

    public SupplierEvidenceCentreView(
            SupplierPortalService portalService,
            PartnerDocumentExchangeService docService
    ) {
        super("Supplier Evidence & Assurance Centre", "Self-service submission of quality certificates, conflict mineral disclosures, and CBAM/EUDR audit evidence.");
        this.portalService = portalService;
        this.docService = docService;

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        add(new H3("Submit Evidence Document"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        ComboBox<PartnerOrganization> partnerBox = new ComboBox<>("Submitting Partner");
        partnerBox.setItems(partners);
        partnerBox.setItemLabelGenerator(p -> p.name() + " (" + p.id().value() + ")");
        if (!partners.isEmpty()) partnerBox.setValue(partners.get(0));

        TextField fileNameField = new TextField("Document Name", "EUDR-Geoloc-Audit-2026.pdf");
        ComboBox<DocumentCategory> catBox = new ComboBox<>("Document Category");
        catBox.setItems(DocumentCategory.values());
        catBox.setValue(DocumentCategory.ESG_EVIDENCE);

        TextField hashField = new TextField("SHA-256 Checksum", "sha256:7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069");
        hashField.setWidth("250px");

        Grid<PartnerDocumentRecord> grid = new Grid<>();

        Button submitBtn = new Button("Upload & Attest Evidence", e -> {
            if (partnerBox.getValue() == null || fileNameField.getValue() == null || catBox.getValue() == null || hashField.getValue() == null) {
                Notification.show("Please enter all evidence attributes", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            PartnerDocumentRecord doc = docService.uploadDocument(
                    ws,
                    partnerBox.getValue().id(),
                    fileNameField.getValue(),
                    catBox.getValue(),
                    hashField.getValue()
            );
            Notification.show("Evidence submitted: " + doc.fileName() + " (" + doc.id().value() + ")", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(docService.listDocuments(ws));
        });
        submitBtn.getElement().getThemeList().add("primary");

        form.add(partnerBox, fileNameField, catBox, hashField, submitBtn);
        add(form);

        add(new H3("Supplier Submitted Evidence Records"));
        grid.addColumn(d -> d.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(d -> d.partnerId().value()).setHeader("Partner ID").setAutoWidth(true);
        grid.addColumn(PartnerDocumentRecord::fileName).setHeader("Document Title").setAutoWidth(true);
        grid.addColumn(d -> d.category().name()).setHeader("Category").setAutoWidth(true);
        grid.addColumn(PartnerDocumentRecord::cryptographicHash).setHeader("SHA-256 Hash").setAutoWidth(true);
        grid.addComponentColumn(d -> {
            Span badge = new Span(d.verified() ? "VERIFIED" : "PENDING AUDIT");
            badge.getElement().getThemeList().add("badge small " + (d.verified() ? "success" : "warning"));
            return badge;
        }).setHeader("Assurance").setAutoWidth(true);
        grid.addColumn(d -> d.uploadedAt().toString()).setHeader("Uploaded At").setAutoWidth(true);

        grid.setItems(docService.listDocuments(ws));
        grid.setHeight("350px");

        add(grid);
    }
}

