package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.WorkspaceAdministrationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaRecord;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Workspace Quotas & Tenant Configuration | SupplyGraph")
@Route(value = "admin/workspaces", layout = MainLayout.class)
public class WorkspaceConfigurationCentreView extends PlatformAdminViewBase {

    public WorkspaceConfigurationCentreView(WorkspaceAdministrationService workspaceService) {
        super("Workspace Configuration Centre", "Configure multi-tenant workspace quotas, resource upper bounds, and operational availability.");

        WorkspaceId wsId = currentWorkspaceId();
        WorkspaceQuotaRecord quota = workspaceService.getOrCreateQuota(wsId);

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.add(
                createKpiCard("Assigned Workspace", wsId.value(), "Active Tenant Context", "primary"),
                createKpiCard("Max Network Nodes", String.valueOf(quota.maxNodes()), "Graph Size Constraint", null),
                createKpiCard("Max Active Disruptions", String.valueOf(quota.maxActiveDisruptions()), "Concurrent Incident Limit", null),
                createKpiCard("Tenant Status", quota.enabled() ? "ENABLED" : "SUSPENDED", "Operational State", quota.enabled() ? "success" : "error")
        );
        add(kpis);

        H3 formTitle = new H3("Modify Workspace Resource Quotas");
        formTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        add(formTitle);

        FormLayout form = new FormLayout();
        TextField tfWorkspace = new TextField("Target Workspace ID");
        tfWorkspace.setValue(wsId.value());
        tfWorkspace.setReadOnly(true);

        IntegerField nfNodes = new IntegerField("Max Digital Twin Nodes");
        nfNodes.setValue(quota.maxNodes());
        nfNodes.setMin(10);
        nfNodes.setMax(50000);

        IntegerField nfDisruptions = new IntegerField("Max Active Disruption Incidents");
        nfDisruptions.setValue(quota.maxActiveDisruptions());
        nfDisruptions.setMin(1);
        nfDisruptions.setMax(1000);

        IntegerField nfSimulations = new IntegerField("Max Concurrent Simulations");
        nfSimulations.setValue(quota.maxConcurrentSimulations());
        nfSimulations.setMin(1);
        nfSimulations.setMax(50);

        Checkbox cbEnabled = new Checkbox("Tenant Workspace Active", quota.enabled());

        form.add(tfWorkspace, nfNodes, nfDisruptions, nfSimulations, cbEnabled);
        add(form);

        Grid<WorkspaceQuotaRecord> grid = new Grid<>();
        grid.addColumn(q -> q.workspaceId().value()).setHeader("Workspace ID").setAutoWidth(true);
        grid.addColumn(WorkspaceQuotaRecord::maxNodes).setHeader("Max Nodes");
        grid.addColumn(WorkspaceQuotaRecord::maxActiveDisruptions).setHeader("Max Disruptions");
        grid.addColumn(WorkspaceQuotaRecord::maxConcurrentSimulations).setHeader("Max Simulations");
        grid.addColumn(q -> q.enabled() ? "ACTIVE" : "SUSPENDED").setHeader("Status");
        grid.addColumn(WorkspaceQuotaRecord::updatedAt).setHeader("Last Updated");

        Button btnSave = new Button("Apply Quota Changes", e -> {
            try {
                workspaceService.configureQuota(wsId, nfNodes.getValue(), nfDisruptions.getValue(), nfSimulations.getValue());
                workspaceService.toggleWorkspace(wsId, cbEnabled.getValue());
                WorkspaceQuotaRecord updated = workspaceService.getOrCreateQuota(wsId);
                grid.setItems(List.of(updated));
                Notification.show("Workspace quota configuration successfully updated", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Failed to update quota: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(btnSave);

        H3 tableTitle = new H3("Workspace Quota Summary");
        tableTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        add(tableTitle);

        grid.setItems(List.of(quota));
        grid.setHeight("250px");
        add(grid);
    }
}
