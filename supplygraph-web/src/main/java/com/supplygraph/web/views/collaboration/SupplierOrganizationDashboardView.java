package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.collaboration.PartnerTier;
import com.supplygraph.domain.model.collaboration.PartnerType;
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

@Route(value = "portal/organization", layout = MainLayout.class)
@PageTitle("Partner Organization Dashboard | ORBIX")
public class SupplierOrganizationDashboardView extends CollaborationViewBase {

    private final SupplierPortalService portalService;

    public SupplierOrganizationDashboardView(SupplierPortalService portalService) {
        super("Partner Organization & Identity Dashboard", "Ecosystem partner directory, tiered onboarding, DUNS registry, and operational tenant profiles.");
        this.portalService = portalService;

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        long directOem = partners.stream().filter(p -> p.tier() == PartnerTier.TIER_1_DIRECT).count();
        long subtiers = partners.stream().filter(p -> p.tier() == PartnerTier.TIER_2_SUBTIER || p.tier() == PartnerTier.TIER_3_RAW).count();
        long logistics = partners.stream().filter(p -> p.type() == PartnerType.LOGISTICS_CARRIER).count();

        kpiRow.add(createKpiCard("Registered Partners", String.valueOf(partners.size()), "Total connected organizations", "primary"));
        kpiRow.add(createKpiCard("Tier-1 Direct OEM", String.valueOf(directOem), "Direct production suppliers", "success"));
        kpiRow.add(createKpiCard("Tier-2/3 Upstream", String.valueOf(subtiers), "Sub-assembly & raw materials", "contrast"));
        kpiRow.add(createKpiCard("Logistics & 3PLs", String.valueOf(logistics), "Carriers & forwarders", "warning"));

        add(kpiRow);

        add(new H3("Onboard Ecosystem Partner Organization"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField nameField = new TextField("Organization Name", "Samsung SDI Europe");
        ComboBox<PartnerType> typeBox = new ComboBox<>("Partner Type");
        typeBox.setItems(PartnerType.values());
        typeBox.setValue(PartnerType.SUPPLIER);

        ComboBox<PartnerTier> tierBox = new ComboBox<>("Partner Tier");
        tierBox.setItems(PartnerTier.values());
        tierBox.setValue(PartnerTier.TIER_1_DIRECT);

        TextField dunsField = new TextField("DUNS Number", "08-112-9988");
        TextField countryField = new TextField("Country", "HU");
        countryField.setWidth("80px");

        Grid<PartnerOrganization> grid = new Grid<>();

        Button onboardBtn = new Button("Onboard Partner", e -> {
            if (nameField.getValue() == null || typeBox.getValue() == null || tierBox.getValue() == null || countryField.getValue() == null) {
                Notification.show("Please fill all required partner fields", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            PartnerOrganization p = portalService.onboardPartner(
                    ws,
                    nameField.getValue(),
                    typeBox.getValue(),
                    tierBox.getValue(),
                    dunsField.getValue(),
                    countryField.getValue()
            );
            Notification.show("Onboarded partner: " + p.name() + " (" + p.id().value() + ")", 3000, Notification.Position.TOP_CENTER);
            grid.setItems(portalService.listPartners(ws));
        });
        onboardBtn.getElement().getThemeList().add("primary");

        form.add(nameField, typeBox, tierBox, dunsField, countryField, onboardBtn);
        add(form);

        add(new H3("Partner Organizations Directory"));
        grid.addColumn(p -> p.id().value()).setHeader("Partner ID").setAutoWidth(true);
        grid.addColumn(PartnerOrganization::name).setHeader("Legal Entity Name").setAutoWidth(true);
        grid.addColumn(p -> p.type().name()).setHeader("Type").setAutoWidth(true);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.tier().name());
            badge.getElement().getThemeList().add("badge small " + (p.tier() == PartnerTier.TIER_1_DIRECT ? "success" : "contrast"));
            return badge;
        }).setHeader("Tier").setAutoWidth(true);
        grid.addColumn(PartnerOrganization::dunsNumber).setHeader("DUNS").setAutoWidth(true);
        grid.addColumn(PartnerOrganization::countryCode).setHeader("Country").setAutoWidth(true);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.active() ? "ACTIVE" : "INACTIVE");
            badge.getElement().getThemeList().add("badge small " + (p.active() ? "success" : "error"));
            return badge;
        }).setHeader("Status").setAutoWidth(true);
        grid.addColumn(p -> p.onboardedAt().toString()).setHeader("Onboarded").setAutoWidth(true);

        grid.setItems(partners);
        grid.setHeight("350px");

        add(grid);
    }
}

