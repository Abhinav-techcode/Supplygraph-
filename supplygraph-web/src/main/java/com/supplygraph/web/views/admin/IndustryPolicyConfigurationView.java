package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.IndustryPolicyLifecycleService;
import com.supplygraph.domain.model.platform.IndustryPolicyId;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;
import com.supplygraph.domain.model.platform.PolicyStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Industry Policy Configuration | ORBIX")
@Route(value = "admin/industry-policies", layout = MainLayout.class)
public class IndustryPolicyConfigurationView extends PlatformAdminViewBase {

    private final IndustryPolicyLifecycleService policyService;
    private final Grid<IndustryPolicyRecord> grid = new Grid<>();

    public IndustryPolicyConfigurationView(IndustryPolicyLifecycleService policyService) {
        super("Industry Policy Configuration", "Versioned industry compliance policies, risk thresholds, and material safety margins.");
        this.policyService = policyService;

        refreshGrid();

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setWidthFull();

        Button btnCreate = new Button("Draft New Policy Revision", VaadinIcon.PLUS.create(), e -> openDraftDialog());
        btnCreate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnHistory = new Button("Version History", VaadinIcon.TIME_BACKWARD.create(), e -> UI.getCurrent().navigate(ConfigurationVersionHistoryView.class));
        Button btnDiff = new Button("Policy Comparison", VaadinIcon.SPLIT.create(), e -> UI.getCurrent().navigate(ConfigurationComparisonView.class));

        actionsBar.add(btnCreate, btnHistory, btnDiff);
        add(actionsBar);

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.addColumn(p -> p.id().value()).setHeader("Policy ID").setAutoWidth(true);
        grid.addColumn(IndustryPolicyRecord::industryCode).setHeader("Industry Code");
        grid.addColumn(p -> "v" + p.versionNumber()).setHeader("Version");
        grid.addColumn(IndustryPolicyRecord::status).setHeader("Status");
        grid.addColumn(p -> p.policyJson().length() > 40 ? p.policyJson().substring(0, 40) + "..." : p.policyJson()).setHeader("Policy Payload");
        grid.addColumn(IndustryPolicyRecord::createdAt).setHeader("Created At");

        grid.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();
            if (p.status() == PolicyStatus.DRAFT) {
                Button valBtn = new Button("Validate", e -> {
                    policyService.validatePolicy(p.id());
                    refreshGrid();
                    Notification.show("Policy validated", 2000, Notification.Position.TOP_CENTER);
                });
                valBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                row.add(valBtn);
            }
            if (p.status() == PolicyStatus.VALIDATED || p.status() == PolicyStatus.DRAFT) {
                Button actBtn = new Button("Activate", e -> {
                    policyService.activatePolicy(p.id());
                    refreshGrid();
                    Notification.show("Policy activated", 2000, Notification.Position.TOP_CENTER);
                });
                actBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                row.add(actBtn);
            }
            if (p.status() == PolicyStatus.ACTIVE) {
                Button depBtn = new Button("Deprecate", e -> {
                    policyService.deprecatePolicy(p.id());
                    refreshGrid();
                    Notification.show("Policy marked deprecated", 2000, Notification.Position.TOP_CENTER);
                });
                depBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                row.add(depBtn);
            }
            return row;
        }).setHeader("Lifecycle Actions");

        grid.setHeight("450px");
    }

    private void refreshGrid() {
        List<IndustryPolicyRecord> list = policyService.findAll(currentWorkspaceId());
        grid.setItems(list);
    }

    private void openDraftDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Draft New Industry Policy");

        VerticalLayout layout = new VerticalLayout();
        ComboBox<String> cbIndustry = new ComboBox<>("Industry Pack");
        cbIndustry.setItems("EV_BATTERY", "SEMICONDUCTOR", "AEROSPACE", "PHARMACEUTICALS");
        cbIndustry.setValue("EV_BATTERY");

        TextArea taJson = new TextArea("Policy Configuration JSON");
        taJson.setValue("{\n  \"maxLeadTimeDays\": 45,\n  \"coldChainRequired\": false,\n  \"safetyStockBufferPct\": 15.0\n}");
        taJson.setWidthFull();
        taJson.setHeight("180px");

        layout.add(cbIndustry, taJson);
        dialog.add(layout);

        Button btnSave = new Button("Save Draft", e -> {
            try {
                policyService.createDraftPolicy(currentWorkspaceId(), cbIndustry.getValue(), taJson.getValue());
                refreshGrid();
                dialog.close();
                Notification.show("Policy revision draft created successfully", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Failed to create draft: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(btnCancel, btnSave);
        dialog.open();
    }
}
