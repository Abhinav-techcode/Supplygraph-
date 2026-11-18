package com.supplygraph.web.views.workspace;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.security.UserManagementService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/admin", layout = MainLayout.class)
@PageTitle("Platform Admin Workspace | SupplyGraph")
public class PlatformAdminWorkspaceView extends WorkspaceViewBase {

    private final UserManagementService userService;
    private final AuditGovernanceService auditService;

    public PlatformAdminWorkspaceView(
            UserManagementService userService,
            AuditGovernanceService auditService
    ) {
        super("Platform Administration & Governance", "Tenant workspace isolation, RBAC role assignments, transactional outbox telemetry, and immutable audit logs.", "Role :: Platform Administrator");
        this.userService = userService;
        this.auditService = auditService;

        WorkspaceId ws = currentWorkspaceId();
        List<AuditLogEntry> auditLogs = auditService.getAuditTrail(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Registered Users", String.valueOf(userService.listAllUsers().size()), "Enterprise RBAC accounts", "primary"));
        kpiRow.add(createKpiCard("Audit Trail Records", String.valueOf(auditLogs.size()), "Immutable governance events", "success"));
        kpiRow.add(createKpiCard("System Health", "See System Health", "Live status is on the System Health page", "contrast"));
        kpiRow.add(createKpiCard("Industry Packs", "4 registered", "EV Battery, Pharmaceutical, Semiconductor, Automotive", "warning"));

        add(kpiRow);

        add(new H3("Recent System Audit & Security Trail"));
        Grid<AuditLogEntry> grid = new Grid<>();
        grid.addColumn(a -> a.id().value()).setHeader("Audit ID").setAutoWidth(true);
        grid.addColumn(a -> a.userId().value()).setHeader("User ID").setAutoWidth(true);
        grid.addColumn(AuditLogEntry::action).setHeader("Action").setAutoWidth(true);
        grid.addColumn(AuditLogEntry::targetEntityId).setHeader("Target Entity ID").setAutoWidth(true);
        grid.addColumn(AuditLogEntry::rationale).setHeader("Rationale").setAutoWidth(true);
        grid.addColumn(a -> a.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);

        grid.setItems(auditLogs);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Security & RBAC Admin", VaadinIcon.LOCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("security-admin"))));
        actionRow.add(new Button("Audit Governance Ledger", VaadinIcon.DIPLOMA.create(), e -> getUI().ifPresent(ui -> ui.navigate("audit-log"))));
        actionRow.add(new Button("Industry Configuration", VaadinIcon.OPTIONS.create(), e -> getUI().ifPresent(ui -> ui.navigate("industry-config"))));
        actionRow.add(new Button("System Health Telemetry", VaadinIcon.HEART.create(), e -> getUI().ifPresent(ui -> ui.navigate("health"))));

        add(grid, actionRow);
    }
}
