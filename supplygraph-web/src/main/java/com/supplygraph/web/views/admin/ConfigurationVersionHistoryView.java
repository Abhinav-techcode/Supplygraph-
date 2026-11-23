package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.IndustryPolicyLifecycleService;
import com.supplygraph.domain.model.platform.IndustryPolicyRecord;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Policy Configuration Version History | SupplyGraph")
@Route(value = "admin/industry-policies/history", layout = MainLayout.class)
public class ConfigurationVersionHistoryView extends PlatformAdminViewBase {

    private final IndustryPolicyLifecycleService policyService;
    private final Grid<IndustryPolicyRecord> historyGrid = new Grid<>();
    private final ComboBox<String> cbIndustry = new ComboBox<>("Industry Pack");

    public ConfigurationVersionHistoryView(IndustryPolicyLifecycleService policyService) {
        super("Configuration Version History", "Audited immutable revision log of industry pack policies with deterministic rollback capability.");
        this.policyService = policyService;

        // Offer the industries this workspace really has policies for; fall back to the known packs when there are none yet.
        java.util.List<String> industries = policyService.findAll(currentWorkspaceId()).stream().map(IndustryPolicyRecord::industryCode).distinct().sorted().toList();
        if (industries.isEmpty()) industries = java.util.List.of("AUTOMOTIVE", "EV_BATTERY", "SEMICONDUCTOR", "PHARMACEUTICALS");
        cbIndustry.setItems(industries);
        cbIndustry.setValue(industries.getFirst());
        cbIndustry.addValueChangeListener(e -> loadHistory(e.getValue()));

        HorizontalLayout filterBar = new HorizontalLayout(cbIndustry);
        filterBar.setWidthFull();
        filterBar.setAlignItems(Alignment.BASELINE);
        add(filterBar);

        configureGrid();
        add(historyGrid);

        loadHistory(cbIndustry.getValue());
    }

    private void configureGrid() {
        historyGrid.addColumn(p -> "v" + p.versionNumber()).setHeader("Revision").setAutoWidth(true);
        historyGrid.addColumn(p -> p.id().value()).setHeader("Policy ID");
        historyGrid.addColumn(IndustryPolicyRecord::status).setHeader("Status");
        historyGrid.addColumn(p -> p.policyJson().length() > 60 ? p.policyJson().substring(0, 60) + "..." : p.policyJson()).setHeader("Payload Content");
        historyGrid.addColumn(IndustryPolicyRecord::createdAt).setHeader("Committed At");

        historyGrid.addComponentColumn(p -> {
            Button btnRollback = new Button("Rollback to this", e -> {
                try {
                    IndustryPolicyRecord rolled = policyService.rollbackPolicy(p.id());
                    loadHistory(cbIndustry.getValue());
                    Notification.show("Successfully rolled back to policy v" + p.versionNumber() + " (created as v" + rolled.versionNumber() + ")", 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Rollback failed: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            btnRollback.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            return btnRollback;
        }).setHeader("Deterministic Rollback");

        historyGrid.setHeight("450px");
    }

    private void loadHistory(String industryCode) {
        if (industryCode == null) return;
        List<IndustryPolicyRecord> history = policyService.getPolicyHistory(currentWorkspaceId(), industryCode);
        historyGrid.setItems(history);
    }
}
