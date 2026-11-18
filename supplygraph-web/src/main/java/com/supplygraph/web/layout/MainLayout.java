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

        Span badge = new Span("Phase 9 & 10 :: Enterprise Integration Runtime & Governed Execution");
        badge.getElement().getThemeList().add("badge success small");
        badge.getStyle().set("margin-left", "var(--lumo-space-s)");

        HorizontalLayout header = new HorizontalLayout(toggle, title, badge);
        if (Boolean.parseBoolean(System.getenv("SUPPLYGRAPH_UI_DEMO_ROLE_SWITCHER"))) {
            // DEMO ONLY: there is no login system; this switches the session principal so separation of duties can be exercised.
            com.vaadin.flow.component.combobox.ComboBox<com.supplygraph.domain.model.security.UserRole> roles =
                    new com.vaadin.flow.component.combobox.ComboBox<>("", com.supplygraph.domain.model.security.UserRole.values());
            roles.setAllowCustomValue(false);
            roles.setWidth("16em");
            com.supplygraph.domain.model.security.UserProfile current = com.supplygraph.application.security.SecurityContext.getCurrentUser();
            if (current != null) roles.setValue(current.role());
            roles.addValueChangeListener(e -> {
                if (e.isFromClient() && e.getValue() != null) {
                    com.vaadin.flow.server.VaadinSession.getCurrent().getSession().setAttribute(
                            com.supplygraph.web.security.UiPrincipalFilter.SESSION_ATTRIBUTE, com.supplygraph.web.security.UiPrincipalFilter.principalFor(e.getValue()));
                    com.vaadin.flow.component.UI.getCurrent().getPage().reload();
                }
            });
            Span demo = new Span("DEMO role switcher — not authentication");
            demo.getElement().getThemeList().add("badge error small");
            header.add(demo, roles);
        }
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Overview", "", VaadinIcon.DASHBOARD.create()));
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
        nav.addItem(new SideNavItem("System Health", "health", VaadinIcon.HEART.create()));

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("p-s");

        addToDrawer(scroller);
    }
}
