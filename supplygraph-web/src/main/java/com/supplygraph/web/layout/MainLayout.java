package com.supplygraph.web.layout;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enterprise Navigation & Application Layout for SupplyGraph.
 * Provides complete access to all 85+ views across Phases 1 through 15,
 * with role-based persona switching and on-demand demo data replenishment.
 */
public class MainLayout extends AppLayout {

    private final DemoDataInitializer demoDataInitializer;

    private final boolean demoRoleSwitcher;

    public MainLayout() {
        this(null, false);
    }

    @Autowired
    public MainLayout(@Autowired(required = false) DemoDataInitializer demoDataInitializer,
                      @org.springframework.beans.factory.annotation.Value("${supplygraph.ui.demo-role-switcher:false}") boolean demoRoleSwitcher) {
        this.demoDataInitializer = demoDataInitializer;
        this.demoRoleSwitcher = demoRoleSwitcher;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        H1 title = new H1("ORBIX");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "800")
                .set("letter-spacing", "0.14em")
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span badge = new Span("Supply Chain Digital Twin & Recovery");
        badge.getElement().getThemeList().add("badge success small");
        badge.getStyle().set("margin-left", "var(--lumo-space-s)");

        Button seedBtn = new Button("Seed / Reset Demo Data", VaadinIcon.DATABASE.create(), e -> {
            if (demoDataInitializer != null) {
                try {
                    demoDataInitializer.seedDemoData(true);
                    Notification.show("Complete automotive supply chain network, disruptions, inventory, and supplier catalog re-seeded!", 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().getPage().reload();
                } catch (Exception ex) {
                    Notification.show("Error seeding demo data: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Demo data initializer is not active in this environment.", 3000, Notification.Position.TOP_CENTER);
            }
        });
        seedBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        seedBtn.getStyle().set("margin-left", "auto");

        Select<UserRole> roleSelect = new Select<>();
        roleSelect.setLabel("Active Persona");
        roleSelect.setItems(UserRole.values());
        roleSelect.setItemLabelGenerator(UserRole::getDisplayName);
        roleSelect.setWidth("240px");
        roleSelect.getStyle().set("margin-left", "var(--lumo-space-m)");

        UserProfile current = SecurityContext.getCurrentUser();
        if (current != null) {
            roleSelect.setValue(current.role());
        }

        roleSelect.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && e.getValue() != e.getOldValue()) {
                VaadinSession.getCurrent().getSession().setAttribute(
                        UiPrincipalFilter.SESSION_ATTRIBUTE, UiPrincipalFilter.principalFor(e.getValue()));
                UI.getCurrent().getPage().reload();
            }
        });

        badge.getStyle().set("margin-left", "auto");

        HorizontalLayout header = new HorizontalLayout(toggle, title, badge);
        if (demoRoleSwitcher) header.add(roleSelect);   // demo only: acting as another role without signing in again
        if (current != null) {
            Span who = new Span(current.displayName() + " · " + current.role().getDisplayName());
            who.addClassName("sg-whoami");
            Button out = new Button("Sign out", VaadinIcon.SIGN_OUT.create(), e -> {
                com.supplygraph.web.auth.AuthSession.signOut();
                UI.getCurrent().getPage().setLocation("/welcome");
            });
            out.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            out.addClassName("sg-signout");
            header.add(who, out);
        }
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private static Span heading(String text) {
        Span s = new Span(text.toUpperCase());
        s.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("font-weight", "700").set("letter-spacing", "0.06em")
                .set("color", "var(--lumo-secondary-text-color)").set("padding", "10px 12px 2px");
        return s;
    }

    private void createDrawer() {
        // The few pages most people need, always visible. Everything else sits behind "All tools".
        SideNav core = new SideNav();
        core.getElement().setAttribute("aria-label", "Start here");
        core.addItem(new SideNavItem("Home", "", VaadinIcon.HOME.create()));
        core.addItem(new SideNavItem("Business simulator", "simulation", VaadinIcon.PLAY_CIRCLE.create()));
        core.addItem(new SideNavItem("Guided recovery", "incident-workspace", VaadinIcon.LIFEBUOY.create()));
        core.addItem(new SideNavItem("Network map", "digital-twin", VaadinIcon.SITEMAP.create()));
        core.addItem(new SideNavItem("Add your data", "network-import", VaadinIcon.UPLOAD.create()));
        core.addItem(new SideNavItem("Database", "database", VaadinIcon.DATABASE.create()));

        SideNav nav = new SideNav();   // the full, advanced menu

        // 1. Role-Specific Workspaces
        SideNavItem workspaces = new SideNavItem("Role Workspaces", "workspace/executive", VaadinIcon.USER_CARD.create());
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

        // 2. Incidents & Continuous Monitoring
        SideNavItem incidents = new SideNavItem("Incidents", "disruptions", VaadinIcon.WARNING.create());
        incidents.addItem(new SideNavItem("Command centre", "command-center"));
        incidents.addItem(new SideNavItem("Disruptions Register", "disruptions"));
        incidents.addItem(new SideNavItem("Cascading Impact Assessment", "impact"));
        incidents.addItem(new SideNavItem("Operational Risk Alerts", "risk-alerts"));
        incidents.addItem(new SideNavItem("Operational Events Stream", "events"));
        incidents.addItem(new SideNavItem("Event Ingestion", "event-ingestion"));
        incidents.addItem(new SideNavItem("Event Ingestion History", "event-ingestion-history"));
        incidents.addItem(new SideNavItem("Monitoring Rules Config", "monitoring-rules"));
        incidents.addItem(new SideNavItem("Reassessments History", "reassessments"));
        incidents.addItem(new SideNavItem("Adaptive Replanning Monitor", "adaptive-replanning"));
        nav.addItem(incidents);

        // 3. Decisions & Bounded AI Agents
        SideNavItem decisions = new SideNavItem("Decisions", "decision-centre", VaadinIcon.LIGHTBULB.create());
        decisions.addItem(new SideNavItem("Recovery Decision Centre", "decision-centre"));
        decisions.addItem(new SideNavItem("Explainable Recommendations", "recommendations-explainable"));
        decisions.addItem(new SideNavItem("Decision Provenance", "decision-provenance"));
        decisions.addItem(new SideNavItem("Specialist Agent History", "agent-history"));
        decisions.addItem(new SideNavItem("What-If Simulation Workspace", "what-if-workspace"));
        decisions.addItem(new SideNavItem("Executive Decision Intelligence", "decision-intelligence"));
        nav.addItem(decisions);

        // 4. Governed Execution Center
        SideNavItem execution = new SideNavItem("Execution", "execution-centre", VaadinIcon.TASKS.create());
        execution.addItem(new SideNavItem("Execution Centre", "execution-centre"));
        execution.addItem(new SideNavItem("Workflow Designer", "execution-designer"));
        execution.addItem(new SideNavItem("Execution Approvals", "execution-approvals"));
        execution.addItem(new SideNavItem("Task Dependency Explorer", "task-dependencies"));
        execution.addItem(new SideNavItem("Live Execution Monitor", "execution-monitor"));
        execution.addItem(new SideNavItem("Failure Management", "execution-failures"));
        execution.addItem(new SideNavItem("Execution Acknowledgements", "execution-acknowledgements"));
        execution.addItem(new SideNavItem("Reconciliation Centre", "execution-reconciliation"));
        execution.addItem(new SideNavItem("Compensation Actions", "execution-compensation"));
        execution.addItem(new SideNavItem("Audit Timeline", "execution-timeline"));
        execution.addItem(new SideNavItem("Plan vs Actual Reassessment", "plan-vs-actual"));
        execution.addItem(new SideNavItem("Closed-Loop Feedback", "closed-loop"));
        nav.addItem(execution);

        // 5. Supply Chain Planning
        SideNavItem planning = new SideNavItem("Planning", "planning", VaadinIcon.CALENDAR.create());
        planning.addItem(new SideNavItem("Planning Command Centre", "planning"));
        planning.addItem(new SideNavItem("Demand Workspace", "planning/demand"));
        planning.addItem(new SideNavItem("Demand Forecasts", "planning/demand/forecasts"));
        planning.addItem(new SideNavItem("Forecast Detail Explorer", "planning/demand/detail"));
        planning.addItem(new SideNavItem("Inventory Centre", "planning/inventory"));
        planning.addItem(new SideNavItem("Safety Stock Planner", "planning/inventory/safety-stock"));
        planning.addItem(new SideNavItem("Reorder Workspace", "planning/inventory/reorder"));
        planning.addItem(new SideNavItem("Procurement Centre", "planning/procurement"));
        planning.addItem(new SideNavItem("Supplier Allocation", "planning/procurement/allocation"));
        planning.addItem(new SideNavItem("Production Workspace", "planning/production"));
        planning.addItem(new SideNavItem("Capacity Explorer", "planning/production/capacity"));
        planning.addItem(new SideNavItem("Multi-Echelon Network", "planning/network"));
        planning.addItem(new SideNavItem("Scenario Designer", "planning/scenarios"));
        planning.addItem(new SideNavItem("Results Explorer", "planning/scenarios/results"));
        planning.addItem(new SideNavItem("Approval Handoff", "planning/approval"));
        planning.addItem(new SideNavItem("Inventory Intelligence", "inventory-intelligence"));
        planning.addItem(new SideNavItem("Material Criticality", "material-criticality"));
        nav.addItem(planning);

        // 6. Supplier Portal Gateway
        SideNavItem portal = new SideNavItem("Supplier Portal", "portal", VaadinIcon.SHOP.create());
        portal.addItem(new SideNavItem("Portal Gateway", "portal"));
        portal.addItem(new SideNavItem("Organization Directory", "portal/organization"));
        portal.addItem(new SideNavItem("Supplier Profile", "portal/profile"));
        portal.addItem(new SideNavItem("Assurance Evidence", "portal/evidence"));
        portal.addItem(new SideNavItem("Purchase Orders", "portal/orders"));
        portal.addItem(new SideNavItem("Submit RFQ Bid", "portal/rfq/submit"));
        portal.addItem(new SideNavItem("Delivery ASNs", "portal/deliveries"));
        portal.addItem(new SideNavItem("Capacity Commitment", "portal/capacity"));
        portal.addItem(new SideNavItem("Supplier Intelligence", "supplier-intelligence"));
        portal.addItem(new SideNavItem("Supplier Dependencies", "supplier-dependencies"));
        nav.addItem(portal);

        // 7. Partner Collaboration (SRM)
        SideNavItem collab = new SideNavItem("Partners", "collaboration/suppliers", VaadinIcon.USERS.create());
        collab.addItem(new SideNavItem("SRM Relationship Hub", "collaboration/suppliers"));
        collab.addItem(new SideNavItem("RFQ Sourcing", "collaboration/rfq"));
        collab.addItem(new SideNavItem("Quotation Comparison", "collaboration/rfq/compare"));
        collab.addItem(new SideNavItem("8D Quality Investigations", "collaboration/quality"));
        collab.addItem(new SideNavItem("Document Vault", "collaboration/documents"));
        collab.addItem(new SideNavItem("Activity Timeline", "collaboration/activity"));
        nav.addItem(collab);

        // 8. Sustainability & ESG
        SideNavItem sustainability = new SideNavItem("Sustainability", "sustainability", VaadinIcon.GLOBE.create());
        sustainability.addItem(new SideNavItem("Sustainability Centre", "sustainability"));
        sustainability.addItem(new SideNavItem("Carbon Footprint (Scope 1-3)", "sustainability/carbon"));
        sustainability.addItem(new SideNavItem("Transport Freight Emissions", "sustainability/carbon/transport"));
        sustainability.addItem(new SideNavItem("Manufacturing Scope 1/2", "sustainability/carbon/manufacturing"));
        sustainability.addItem(new SideNavItem("Supplier ESG Assurance", "sustainability/esg"));
        sustainability.addItem(new SideNavItem("Material Origin Explorer", "sustainability/traceability/origin"));
        sustainability.addItem(new SideNavItem("Product Genealogy", "sustainability/traceability/genealogy"));
        sustainability.addItem(new SideNavItem("Batch/Lot Ledger Traceability", "sustainability/traceability/lots"));
        sustainability.addItem(new SideNavItem("Recall Impact Analysis", "sustainability/traceability/recall"));
        sustainability.addItem(new SideNavItem("Circular Recovery", "sustainability/circular"));
        sustainability.addItem(new SideNavItem("Compliance & Evidence Vault", "sustainability/evidence"));
        sustainability.addItem(new SideNavItem("Decarbonization Pathways", "sustainability/scenarios"));
        nav.addItem(sustainability);

        // 9. Analytics, Simulation & Optimization
        SideNavItem analytics = new SideNavItem("Analytics lab", "simulation", VaadinIcon.CHART_LINE.create());
        analytics.addItem(new SideNavItem("Business simulator", "simulation"));
        analytics.addItem(new SideNavItem("Simulate a recorded incident", "simulation/incident"));
        analytics.addItem(new SideNavItem("Monte Carlo Engine", "monte-carlo"));
        analytics.addItem(new SideNavItem("OR-Tools Optimization", "optimization"));
        analytics.addItem(new SideNavItem("Pareto Multi-Objective", "pareto-comparison"));
        analytics.addItem(new SideNavItem("Sensitivity Analysis", "sensitivity-analysis"));
        analytics.addItem(new SideNavItem("Advanced Resilience", "advanced-resilience"));
        analytics.addItem(new SideNavItem("Supply Chain Resilience", "resilience-analytics"));
        analytics.addItem(new SideNavItem("Research Experiment Manager", "experiment-manager"));
        analytics.addItem(new SideNavItem("Benchmark Studio", "benchmark-studio"));
        analytics.addItem(new SideNavItem("Benchmark Dashboard", "benchmark-dashboard"));
        analytics.addItem(new SideNavItem("Statistical Results", "statistical-results"));
        nav.addItem(analytics);

        // 10. Enterprise Integration
        SideNavItem integration = new SideNavItem("Integration", "integration-centre", VaadinIcon.CONNECT_O.create());
        integration.addItem(new SideNavItem("Integration Centre", "integration-centre"));
        integration.addItem(new SideNavItem("Connector Registry", "connectors"));
        integration.addItem(new SideNavItem("Connector Configuration", "connector-config"));
        integration.addItem(new SideNavItem("Connector Health Monitor", "connector-health"));
        integration.addItem(new SideNavItem("Synchronization Centre", "synchronization"));
        integration.addItem(new SideNavItem("Sync History", "sync-history"));
        integration.addItem(new SideNavItem("Event Delivery Monitor", "event-delivery"));
        integration.addItem(new SideNavItem("Dead Letter Resolution", "dead-letters"));
        integration.addItem(new SideNavItem("Event Replay Centre", "event-replay"));
        integration.addItem(new SideNavItem("Reconciliation Centre", "reconciliation"));
        integration.addItem(new SideNavItem("Security & Audit", "integration-security"));
        integration.addItem(new SideNavItem("Runtime Operations", "runtime-operations"));
        nav.addItem(integration);

        // 11. Enterprise Platform & Administration
        SideNavItem admin = new SideNavItem("Administration", "admin/enterprise", VaadinIcon.COG.create());
        admin.addItem(new SideNavItem("Platform Admin Centre", "admin/enterprise"));
        admin.addItem(new SideNavItem("Security Admin & RBAC", "security-admin"));
        admin.addItem(new SideNavItem("User Governance", "admin/users"));
        admin.addItem(new SideNavItem("Workspace Quotas", "admin/workspaces"));
        admin.addItem(new SideNavItem("Industry Policies", "admin/industry-policies"));
        admin.addItem(new SideNavItem("Policy Version History", "admin/industry-policies/history"));
        admin.addItem(new SideNavItem("Policy Diff & Compare", "admin/industry-policies/diff"));
        admin.addItem(new SideNavItem("Audit Governance Log", "audit-log"));
        admin.addItem(new SideNavItem("Enterprise Data Integration", "data-integration"));
        admin.addItem(new SideNavItem("Import Validation Preview", "import-preview"));
        admin.addItem(new SideNavItem("Import History", "import-history"));
        admin.addItem(new SideNavItem("Data Quality Dashboard", "data-quality"));
        admin.addItem(new SideNavItem("Data Provenance Explorer", "provenance"));
        admin.addItem(new SideNavItem("Industry Configuration", "industry-config"));
        admin.addItem(new SideNavItem("API Developer Centre", "admin/api"));
        admin.addItem(new SideNavItem("Extension Registry", "admin/extensions"));
        admin.addItem(new SideNavItem("System monitor", "monitoring"));
        admin.addItem(new SideNavItem("Database", "database"));
        admin.addItem(new SideNavItem("Platform Health Dashboard", "admin/health"));
        admin.addItem(new SideNavItem("Performance Dashboard", "admin/performance"));
        admin.addItem(new SideNavItem("Backup & Recovery Vault", "admin/backup"));
        admin.addItem(new SideNavItem("Release & Runtime Specs", "admin/version"));
        admin.addItem(new SideNavItem("System Health Monitor", "health"));
        nav.addItem(admin);

        boolean showAll = !Boolean.FALSE.equals(com.vaadin.flow.server.VaadinSession.getCurrent() == null ? null
                : com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("sg.allTools"));
        nav.setVisible(showAll);
        Button toggle = new Button(showAll ? "Collapse full menu" : "Show full menu", showAll ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toggle.addClassName("sg-all-tools");
        toggle.addClickListener(e -> {
            boolean now = !nav.isVisible();
            nav.setVisible(now);
            toggle.setText(now ? "Collapse full menu" : "Show full menu");
            toggle.setIcon(now ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
            if (com.vaadin.flow.server.VaadinSession.getCurrent() != null) com.vaadin.flow.server.VaadinSession.getCurrent().setAttribute("sg.allTools", now);
        });
        com.vaadin.flow.component.orderedlayout.VerticalLayout menu = new com.vaadin.flow.component.orderedlayout.VerticalLayout(heading("Start here"), core, heading("All pages, by area"), toggle, nav);
        menu.setPadding(false);
        menu.setSpacing(false);

        Scroller scroller = new Scroller(menu);
        scroller.setClassName("p-s");
        addToDrawer(scroller);
    }
}
