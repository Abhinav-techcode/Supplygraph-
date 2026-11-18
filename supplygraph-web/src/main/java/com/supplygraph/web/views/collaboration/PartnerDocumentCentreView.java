package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.DocumentCategory;
import com.supplygraph.domain.model.collaboration.PartnerDocumentId;
import com.supplygraph.domain.model.collaboration.PartnerDocumentRecord;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
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

@Route(value = "collaboration/documents", layout = MainLayout.class)
@PageTitle("Partner Document Exchange & Vault | SupplyGraph")
public class PartnerDocumentCentreView extends CollaborationViewBase {

    private final PartnerDocumentExchangeService docService;

    public PartnerDocumentCentreView(PartnerDocumentExchangeService docService) {
        super("Partner Document Exchange & Immutable Vault", "Secure cryptographic exchange of material certificates, technical CAD specifications, and ESG declarations.");
        this.docService = docService;

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerDocumentRecord> docs = docService.listDocuments(ws);

        add(new H3("Upload Partner Document to Vault"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField partnerField = new TextField("Partner ID", "PARTNER-LG-ENERGY");
        TextField fileField = new TextField("File Name", "Certificate-of-Analysis-Lot991.pdf");
        fileField.setWidth("300px");
        ComboBox<DocumentCategory> catBox = new ComboBox<>("Document Category");
        catBox.setItems(DocumentCategory.values());
        catBox.setValue(DocumentCategory.QUALITY_CERTIFICATE);
        TextField hashField = new TextField("SHA-256 Checksum", "sha256:5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8");
        hashField.setWidth("250px");

        Grid<PartnerDocumentRecord> grid = new Grid<>();

        Button uploadBtn = new Button("Upload Document", e -> {
            if (partnerField.getValue() == null || fileField.getValue() == null || catBox.getValue() == null || hashField.getValue() == null) {
                Notification.show("Please enter all required document attributes", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            PartnerDocumentRecord record = docService.uploadDocument(
                    ws,
                    new PartnerOrganizationId(partnerField.getValue()),
                    fileField.getValue(),
                    catBox.getValue(),
                    hashField.getValue()
            );
            Notification.show("Vaulted document: " + record.fileName() + " (" + record.id().value() + ")", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(docService.listDocuments(ws));
        });
        uploadBtn.getElement().getThemeList().add("primary");

        form.add(partnerField, fileField, catBox, hashField, uploadBtn);
        add(form);

        add(new H3("Vault Document Ledger"));
        grid.addColumn(d -> d.id().value()).setHeader("Document ID").setAutoWidth(true);
        grid.addColumn(d -> d.partnerId().value()).setHeader("Partner").setAutoWidth(true);
        grid.addColumn(PartnerDocumentRecord::fileName).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(d -> d.category().name()).setHeader("Category").setAutoWidth(true);
        grid.addColumn(PartnerDocumentRecord::cryptographicHash).setHeader("SHA-256 Hash").setAutoWidth(true);
        grid.addComponentColumn(d -> {
            Span badge = new Span(d.verified() ? "VERIFIED" : "UNVERIFIED");
            badge.getElement().getThemeList().add("badge small " + (d.verified() ? "success" : "warning"));
            return badge;
        }).setHeader("Verification").setAutoWidth(true);
        grid.addColumn(d -> d.uploadedAt().toString()).setHeader("Uploaded At").setAutoWidth(true);

        Button verifyBtn = new Button("Verify Selected Document", e -> {
            PartnerDocumentRecord selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("Please select a document to verify", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            PartnerDocumentRecord verified = docService.verifyDocument(ws, selected.id());
            Notification.show("Verified document: " + verified.fileName(), 3000, Notification.Position.TOP_CENTER);
            grid.setItems(docService.listDocuments(ws));
        });
        verifyBtn.getElement().getThemeList().add("success");

        grid.setItems(docs);
        grid.setHeight("350px");

        add(grid, verifyBtn);
    }
}

