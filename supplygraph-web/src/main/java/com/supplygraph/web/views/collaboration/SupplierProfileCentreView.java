package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "portal/profile", layout = MainLayout.class)
@PageTitle("Supplier Profile & Capabilities | ORBIX")
public class SupplierProfileCentreView extends CollaborationViewBase {

    private final SupplierPortalService portalService;

    public SupplierProfileCentreView(SupplierPortalService portalService) {
        super("Supplier Profile & Capabilities Centre", "Self-service supplier profile configuration, ISO/IATF quality accreditations, and production facility metadata.");
        this.portalService = portalService;

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        HorizontalLayout selectorRow = new HorizontalLayout();
        selectorRow.setAlignItems(Alignment.END);

        ComboBox<PartnerOrganization> partnerBox = new ComboBox<>("Select Partner Profile");
        partnerBox.setItems(partners);
        partnerBox.setItemLabelGenerator(p -> p.name() + " (" + p.id().value() + ")");
        partnerBox.setWidth("350px");

        if (!partners.isEmpty()) {
            partnerBox.setValue(partners.get(0));
        }

        FormLayout profileForm = new FormLayout();

        TextField nameField = new TextField("Legal Organization Name");
        TextField typeField = new TextField("Partner Classification");
        TextField tierField = new TextField("Supplier Tier");
        TextField dunsField = new TextField("DUNS Number");
        TextField countryField = new TextField("Country Jurisdiction");
        TextField statusField = new TextField("Registration Status");
        TextField certsField = new TextField("Accreditations", "IATF 16949, ISO 9001, ISO 14001, VDA 6.3");
        TextField capacityField = new TextField("Annual Capacity", "15,000,000 units / year");

        profileForm.add(nameField, typeField, tierField, dunsField, countryField, statusField, certsField, capacityField);
        profileForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 2));

        Runnable updateFields = () -> {
            PartnerOrganization p = partnerBox.getValue();
            if (p != null) {
                nameField.setValue(p.name());
                typeField.setValue(p.type().name());
                tierField.setValue(p.tier().name());
                dunsField.setValue(p.dunsNumber() != null ? p.dunsNumber() : "N/A");
                countryField.setValue(p.countryCode());
                statusField.setValue(p.active() ? "Active & Verified" : "Suspended");
            }
        };

        partnerBox.addValueChangeListener(e -> updateFields.run());
        updateFields.run();

        selectorRow.add(partnerBox);
        add(selectorRow, new H3("Partner Operational Profile"), profileForm);
    }
}

