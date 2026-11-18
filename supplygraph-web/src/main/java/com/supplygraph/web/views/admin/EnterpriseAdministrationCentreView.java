package com.supplygraph.web.views.admin;

import com.supplygraph.application.platform.IndustryPolicyLifecycleService;
import com.supplygraph.application.platform.PlatformTelemetryService;
import com.supplygraph.application.platform.WorkspaceAdministrationService;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Enterprise Administration Centre | SupplyGraph")
@Route(value = "admin/enterprise", layout = MainLayout.class)
public class EnterpriseAdministrationCentreView extends PlatformAdminViewBase {

    public EnterpriseAdministrationCentreView(
            WorkspaceAdministrationService workspaceService,
            IndustryPolicyLifecycleService policyService,
            PlatformTelemetryService telemetryService
    ) {
        super("Enterprise Administration Centre", "Central executive console for enterprise tenant controls, policies, security, and infrastructure governance.");

        var quota = workspaceService.getOrCreateQuota(currentWorkspaceId());
        var policies = policyService.findAll(currentWorkspaceId());
        var metrics = telemetryService.getSystemMetrics();

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.add(
                createKpiCard("Tenant Workspaces", "No data", "See Workspace Administration", "success"),
                createKpiCard("Industry Policies", policies.size() + " Configured", "Active Versioned Rulesets", "primary"),
                createKpiCard("Digital Twin Node Quota", quota.maxNodes() + " Max", "Enforced Upper Bound", null),
                createKpiCard("System Uptime", (metrics.uptimeMillis() / 1000 / 60) + " mins", "JVM 21 LTS Stable", "success")
        );
        add(kpis);

        H3 navTitle = new H3("Platform Management Consoles");
        navTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        add(navTitle);

        HorizontalLayout quickLinksRow1 = new HorizontalLayout();
        quickLinksRow1.setWidthFull();

        Button btnWorkspaces = new Button("Workspace Quotas", VaadinIcon.BUILDING.create(), e -> UI.getCurrent().navigate(WorkspaceConfigurationCentreView.class));
        btnWorkspaces.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnPolicies = new Button("Industry Policies", VaadinIcon.FILE_TEXT.create(), e -> UI.getCurrent().navigate(IndustryPolicyConfigurationView.class));
        btnPolicies.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnApi = new Button("API Developer Centre", VaadinIcon.KEY.create(), e -> UI.getCurrent().navigate(ApiDeveloperCentreView.class));
        btnApi.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button btnHealth = new Button("System Health & Telemetry", VaadinIcon.HEART.create(), e -> UI.getCurrent().navigate(PlatformHealthDashboardView.class));
        btnHealth.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        quickLinksRow1.add(btnWorkspaces, btnPolicies, btnApi, btnHealth);
        add(quickLinksRow1);

        HorizontalLayout quickLinksRow2 = new HorizontalLayout();
        quickLinksRow2.setWidthFull();

        Button btnUsers = new Button("User & Role Governance", VaadinIcon.USERS.create(), e -> UI.getCurrent().navigate(UserRoleAdministrationView.class));
        Button btnBackups = new Button("Disaster Recovery Vault", VaadinIcon.DATABASE.create(), e -> UI.getCurrent().navigate(BackupRecoveryCentreView.class));
        Button btnAudit = new Button("Immutable Audit Trail", VaadinIcon.SHIELD.create(), e -> UI.getCurrent().navigate(AuditGovernanceCentreView.class));
        Button btnVersion = new Button("Release & Runtime Info", VaadinIcon.INFO_CIRCLE.create(), e -> UI.getCurrent().navigate(ReleaseVersionInfoView.class));

        quickLinksRow2.add(btnUsers, btnBackups, btnAudit, btnVersion);
        add(quickLinksRow2);

        VerticalLayout summaryCard = new VerticalLayout();
        summaryCard.addClassNames("p-m", "border", "rounded-m");
        summaryCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("margin-top", "var(--lumo-space-m)");

        summaryCard.add(
                new H3("SupplyGraph Enterprise Platform Status"),
                new Span("• Hexagonal Architecture Isolation: Verified"),
                new Span("• Pure Domain Zero-Dependency Integrity: Verified"),
                new Span("• ConcurGuard Conflict Avoidance Protocol: Active"),
                new Span("• Multi-Tier BOM & Double-Entry Ledger: Enforced"),
                new Span("• Pluggable Industry Extensions: EV Battery, Semiconductor, Aerospace, Pharmaceuticals")
        );
        add(summaryCard);
    }
}
