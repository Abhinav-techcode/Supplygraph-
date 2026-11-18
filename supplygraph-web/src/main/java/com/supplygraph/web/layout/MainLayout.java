package com.supplygraph.web.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Standard enterprise responsive application layout for SupplyGraph.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        H1 title = new H1("SupplyGraph");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span badge = new Span("SupplyGraph");
        badge.getElement().getThemeList().add("badge success small");
        badge.getStyle().set("margin-left", "var(--lumo-space-s)");

        HorizontalLayout header = new HorizontalLayout(toggle, title, badge);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Overview", "", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Digital Twin Graph", "digital-twin", VaadinIcon.SITEMAP.create()));
        nav.addItem(new SideNavItem("Command Centre", "command-center", VaadinIcon.GLOBE.create()));

        SideNavItem workspaces = new SideNavItem("Role Workspaces", "command-center", VaadinIcon.USER_CARD.create());
        workspaces.addItem(new SideNavItem("Executive Cockpit", "workspace/executive"));
        workspaces.addItem(new SideNavItem("Supply Chain Planner", "workspace/planner"));
        workspaces.addItem(new SideNavItem("Procurement Manager", "workspace/procurement"));
        workspaces.addItem(new SideNavItem("Production Manager", "workspace/production"));
        workspaces.addItem(new SideNavItem("Logistics & Freight", "workspace/logistics"));
        workspaces.addItem(new SideNavItem("Quality & Compliance", "workspace/quality"));
        workspaces.addItem(new SideNavItem("Analyst Sandbox", "workspace/analyst"));
        workspaces.addItem(new SideNavItem("Supplier Partner", "workspace/supplier"));
        workspaces.addItem(new SideNavItem("Platform Admin", "workspace/admin"));
        nav.addItem(workspaces);

        nav.addItem(new SideNavItem("Disruptions", "disruptions", VaadinIcon.WARNING.create()));
        nav.addItem(new SideNavItem("Impact Assessment", "impact", VaadinIcon.SEARCH.create()));
        nav.addItem(new SideNavItem("Simulation", "simulation", VaadinIcon.PLAY_CIRCLE.create()));
        nav.addItem(new SideNavItem("Monte Carlo", "monte-carlo", VaadinIcon.CHART_LINE.create()));
        nav.addItem(new SideNavItem("Optimization", "optimization", VaadinIcon.SLIDERS.create()));
        nav.addItem(new SideNavItem("Decision Centre", "decision-centre", VaadinIcon.CHECK_CIRCLE.create()));
        nav.addItem(new SideNavItem("Operational Events", "events", VaadinIcon.RECORDS.create()));
        nav.addItem(new SideNavItem("Event Ingestion", "event-ingestion", VaadinIcon.UPLOAD.create()));
        nav.addItem(new SideNavItem("Monitoring Rules", "monitoring-rules", VaadinIcon.COG.create()));
        nav.addItem(new SideNavItem("Risk Alert Centre", "risk-alerts", VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem("Reassessments", "reassessments", VaadinIcon.REFRESH.create()));
        nav.addItem(new SideNavItem("Adaptive Replanning", "adaptive-replanning", VaadinIcon.CONNECT.create()));
        nav.addItem(new SideNavItem("Agent History", "agent-history", VaadinIcon.USER_CHECK.create()));
        nav.addItem(new SideNavItem("Data Integration", "data-integration", VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem("Import History", "import-history", VaadinIcon.ARCHIVE.create()));
        nav.addItem(new SideNavItem("Import Preview", "import-preview", VaadinIcon.FILE_SEARCH.create()));
        nav.addItem(new SideNavItem("Data Quality", "data-quality", VaadinIcon.CHECK_SQUARE_O.create()));
        nav.addItem(new SideNavItem("Supplier Intelligence", "supplier-intelligence", VaadinIcon.FACTORY.create()));
        nav.addItem(new SideNavItem("Supplier Dependencies", "supplier-dependencies", VaadinIcon.CLUSTER.create()));
        nav.addItem(new SideNavItem("Material Criticality", "material-criticality", VaadinIcon.CUBE.create()));
        nav.addItem(new SideNavItem("Inventory Intelligence", "inventory-intelligence", VaadinIcon.STORAGE.create()));
        nav.addItem(new SideNavItem("Industry Config", "industry-config", VaadinIcon.OPTIONS.create()));
        nav.addItem(new SideNavItem("Resilience Analytics", "resilience-analytics", VaadinIcon.SHIELD.create()));
        nav.addItem(new SideNavItem("Security Admin", "security-admin", VaadinIcon.LOCK.create()));
        nav.addItem(new SideNavItem("Audit Governance", "audit-log", VaadinIcon.DIPLOMA.create()));
        nav.addItem(new SideNavItem("Data Provenance", "provenance", VaadinIcon.CLUSTER.create()));
        nav.addItem(new SideNavItem("Benchmark Studio", "benchmark-studio", VaadinIcon.ACADEMY_CAP.create()));
        nav.addItem(new SideNavItem("Explainable Recovery", "recommendations-explainable", VaadinIcon.LIGHTBULB.create()));
        nav.addItem(new SideNavItem("Decision Provenance", "decision-provenance", VaadinIcon.SITEMAP.create()));
        nav.addItem(new SideNavItem("What-If Workspace", "what-if-workspace", VaadinIcon.SLIDERS.create()));
        nav.addItem(new SideNavItem("Pareto Optimization", "pareto-comparison", VaadinIcon.CHART_LINE.create()));
        nav.addItem(new SideNavItem("Sensitivity Analysis", "sensitivity-analysis", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Advanced Resilience", "advanced-resilience", VaadinIcon.SHIELD.create()));
        nav.addItem(new SideNavItem("Experiment Manager", "experiment-manager", VaadinIcon.ROCKET.create()));
        nav.addItem(new SideNavItem("Benchmark Dashboard", "benchmark-dashboard", VaadinIcon.TABLE.create()));
        nav.addItem(new SideNavItem("Statistical Results", "statistical-results", VaadinIcon.CALC.create()));
        nav.addItem(new SideNavItem("Decision Intelligence", "decision-intelligence", VaadinIcon.EYE.create()));
        SideNavItem integration = new SideNavItem("Enterprise Integration", "integration-centre", VaadinIcon.CONNECT_O.create());
        integration.addItem(new SideNavItem("Connector Registry", "connectors"));
        integration.addItem(new SideNavItem("Configuration", "connector-config"));
        integration.addItem(new SideNavItem("Health Monitor", "connector-health"));
        integration.addItem(new SideNavItem("Synchronization", "synchronization"));
        integration.addItem(new SideNavItem("Sync History", "sync-history"));
        integration.addItem(new SideNavItem("Event Delivery", "event-delivery"));
        integration.addItem(new SideNavItem("Dead Letters", "dead-letters"));
        integration.addItem(new SideNavItem("Event Replay", "event-replay"));
        integration.addItem(new SideNavItem("Reconciliation", "reconciliation"));
        integration.addItem(new SideNavItem("Security & Audit", "integration-security"));
        integration.addItem(new SideNavItem("Runtime Operations", "runtime-operations"));
        nav.addItem(integration);
        SideNavItem execution = new SideNavItem("Governed Execution", "execution-centre", VaadinIcon.PLAY.create());
        execution.addItem(new SideNavItem("Workflow Designer", "execution-designer"));
        execution.addItem(new SideNavItem("Approvals", "execution-approvals"));
        execution.addItem(new SideNavItem("Dependency Explorer", "task-dependencies"));
        execution.addItem(new SideNavItem("Live Monitor", "execution-monitor"));
        execution.addItem(new SideNavItem("Failures", "execution-failures"));
        execution.addItem(new SideNavItem("Acknowledgements", "execution-acknowledgements"));
        execution.addItem(new SideNavItem("Reconciliation", "execution-reconciliation"));
        execution.addItem(new SideNavItem("Compensation", "execution-compensation"));
        execution.addItem(new SideNavItem("Audit Timeline", "execution-timeline"));
        execution.addItem(new SideNavItem("Plan vs Actual", "plan-vs-actual"));
        execution.addItem(new SideNavItem("Closed-Loop Dashboard", "closed-loop"));
        nav.addItem(execution);

        SideNavItem planning = new SideNavItem("SC Planning", "planning", VaadinIcon.CALC_BOOK.create());
        planning.addItem(new SideNavItem("Command Centre", "planning"));
        planning.addItem(new SideNavItem("Demand Workspace", "planning/demand"));
        planning.addItem(new SideNavItem("Demand Forecasts", "planning/demand/forecasts"));
        planning.addItem(new SideNavItem("Forecast Detail", "planning/demand/detail"));
        planning.addItem(new SideNavItem("Inventory Centre", "planning/inventory"));
        planning.addItem(new SideNavItem("Safety Stock Planner", "planning/inventory/safety-stock"));
        planning.addItem(new SideNavItem("Reorder Workspace", "planning/inventory/reorder"));
        planning.addItem(new SideNavItem("Procurement Centre", "planning/procurement"));
        planning.addItem(new SideNavItem("Supplier Allocation", "planning/procurement/allocation"));
        planning.addItem(new SideNavItem("Production Workspace", "planning/production"));
        planning.addItem(new SideNavItem("Capacity Explorer", "planning/production/capacity"));
        planning.addItem(new SideNavItem("Network Planner", "planning/network"));
        planning.addItem(new SideNavItem("Scenario Designer", "planning/scenarios"));
        planning.addItem(new SideNavItem("Results Explorer", "planning/scenarios/results"));
        planning.addItem(new SideNavItem("Approval Handoff", "planning/approval"));
        nav.addItem(planning);

        SideNavItem sustainability = new SideNavItem("Sustainability & ESG", "sustainability", VaadinIcon.GLOBE.create());
        sustainability.addItem(new SideNavItem("Command Centre", "sustainability"));
        sustainability.addItem(new SideNavItem("Carbon Footprint", "sustainability/carbon"));
        sustainability.addItem(new SideNavItem("Transport Freight", "sustainability/carbon/transport"));
        sustainability.addItem(new SideNavItem("Manufacturing Scope 1/2", "sustainability/carbon/manufacturing"));
        sustainability.addItem(new SideNavItem("Supplier ESG", "sustainability/esg"));
        sustainability.addItem(new SideNavItem("Material Origin", "sustainability/traceability/origin"));
        sustainability.addItem(new SideNavItem("Product Genealogy", "sustainability/traceability/genealogy"));
        sustainability.addItem(new SideNavItem("Batch/Lot Ledger", "sustainability/traceability/lots"));
        sustainability.addItem(new SideNavItem("Recall Impact Analysis", "sustainability/traceability/recall"));
        sustainability.addItem(new SideNavItem("Circular Recovery", "sustainability/circular"));
        sustainability.addItem(new SideNavItem("Compliance & Evidence", "sustainability/evidence"));
        sustainability.addItem(new SideNavItem("Decarbonization Pathways", "sustainability/scenarios"));
        nav.addItem(sustainability);

        SideNavItem portal = new SideNavItem("Supplier Portal", "portal", VaadinIcon.EXTERNAL_LINK.create());
        portal.addItem(new SideNavItem("Portal Gateway", "portal"));
        portal.addItem(new SideNavItem("Organization Directory", "portal/organization"));
        portal.addItem(new SideNavItem("Supplier Profile", "portal/profile"));
        portal.addItem(new SideNavItem("Assurance Evidence", "portal/evidence"));
        portal.addItem(new SideNavItem("Purchase Orders", "portal/orders"));
        portal.addItem(new SideNavItem("Submit RFQ Bid", "portal/rfq/submit"));
        portal.addItem(new SideNavItem("Delivery ASNs", "portal/deliveries"));
        portal.addItem(new SideNavItem("Capacity Commitment", "portal/capacity"));
        nav.addItem(portal);

        SideNavItem collab = new SideNavItem("Partner Collaboration", "collaboration/suppliers", VaadinIcon.USERS.create());
        collab.addItem(new SideNavItem("SRM Relationship Hub", "collaboration/suppliers"));
        collab.addItem(new SideNavItem("RFQ Sourcing", "collaboration/rfq"));
        collab.addItem(new SideNavItem("Quotation Comparison", "collaboration/rfq/compare"));
        collab.addItem(new SideNavItem("8D Quality Investigations", "collaboration/quality"));
        collab.addItem(new SideNavItem("Document Vault", "collaboration/documents"));
        collab.addItem(new SideNavItem("Activity Timeline", "collaboration/activity"));
        nav.addItem(collab);

        SideNavItem admin = new SideNavItem("Enterprise Platform", "admin/enterprise", VaadinIcon.COG.create());
        admin.addItem(new SideNavItem("Admin Centre", "admin/enterprise"));
        admin.addItem(new SideNavItem("Workspace Quotas", "admin/workspaces"));
        admin.addItem(new SideNavItem("Industry Policies", "admin/industry-policies"));
        admin.addItem(new SideNavItem("Policy Version History", "admin/industry-policies/history"));
        admin.addItem(new SideNavItem("Policy Diff & Compare", "admin/industry-policies/diff"));
        admin.addItem(new SideNavItem("User Governance", "admin/users"));
        admin.addItem(new SideNavItem("API Developer Centre", "admin/api"));
        admin.addItem(new SideNavItem("Extension Registry", "admin/extensions"));
        admin.addItem(new SideNavItem("Platform Health", "admin/health"));
        admin.addItem(new SideNavItem("Performance Dashboard", "admin/performance"));
        admin.addItem(new SideNavItem("Disaster Recovery Vault", "admin/backup"));
        admin.addItem(new SideNavItem("Release & Runtime Specs", "admin/version"));
        admin.addItem(new SideNavItem("Audit & Governance", "admin/audit"));
        nav.addItem(admin);

        nav.addItem(new SideNavItem("System Health", "health", VaadinIcon.HEART.create()));

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("p-s");

        if (Boolean.parseBoolean(System.getenv("SUPPLYGRAPH_UI_DEMO_ROLE_SWITCHER"))) addToDrawer(roleSwitcher());
        addToDrawer(scroller);
    }

    /** DEMO only (no login system exists): lets one browser session act as different roles to exercise separation of duties. */
    private static com.vaadin.flow.component.Component roleSwitcher() {
        com.vaadin.flow.component.select.Select<com.supplygraph.domain.model.security.UserRole> roles = new com.vaadin.flow.component.select.Select<>();
        roles.setLabel("Acting as (DEMO role switcher — not authentication)");
        roles.setItems(com.supplygraph.domain.model.security.UserRole.values());
        roles.setWidthFull();
        com.supplygraph.domain.model.security.UserProfile current = com.supplygraph.application.security.SecurityContext.getCurrentUser();
        if (current != null) roles.setValue(current.role());
        roles.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && e.getValue() != e.getOldValue()) {
                org.slf4j.LoggerFactory.getLogger(MainLayout.class).info("DEMO role switch to {}", e.getValue());
                com.vaadin.flow.server.VaadinSession.getCurrent().getSession().setAttribute(
                        com.supplygraph.web.security.UiPrincipalFilter.SESSION_ATTRIBUTE, com.supplygraph.web.security.UiPrincipalFilter.principalFor(e.getValue()));
                com.vaadin.flow.component.UI.getCurrent().getPage().reload();
            }
        });
        com.vaadin.flow.component.orderedlayout.VerticalLayout box = new com.vaadin.flow.component.orderedlayout.VerticalLayout(roles);
        box.setPadding(true);
        box.getStyle().set("background", "var(--lumo-error-color-10pct)");
        return box;
    }
}
