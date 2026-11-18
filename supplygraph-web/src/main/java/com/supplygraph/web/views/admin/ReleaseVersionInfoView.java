package com.supplygraph.web.views.admin;

import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Release & Runtime Information | SupplyGraph")
@Route(value = "admin/version", layout = MainLayout.class)
public class ReleaseVersionInfoView extends PlatformAdminViewBase {

    public record ReleaseComponent(
            String component,
            String version,
            String status,
            String notes
    ) {}

    public ReleaseVersionInfoView() {
        super("Release & Runtime Information", "Official technical specifications, dependency versions, architecture maturity, and release governance.");

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.add(
                createKpiCard("SupplyGraph Version", "v0.1.0-RELEASE", "Phases 1-15 Delivered", "success"),
                createKpiCard("Java Platform", "Java 21 LTS", "OpenJDK 64-Bit VM", "primary"),
                createKpiCard("UI Framework", "Vaadin Flow 24.5.4", "Java-First Full-Stack", "primary"),
                createKpiCard("Persistence Engine", "PostgreSQL 16+", "Flyway v15 Schema", "success")
        );
        add(kpis);

        H3 gridTitle = new H3("Enterprise Stack & Framework Specifications");
        gridTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        add(gridTitle);

        Grid<ReleaseComponent> grid = new Grid<>();
        grid.addColumn(ReleaseComponent::component).setHeader("Subsystem / Dependency").setAutoWidth(true);
        grid.addColumn(ReleaseComponent::version).setHeader("Pinned Version");
        grid.addColumn(ReleaseComponent::status).setHeader("Maturity Status");
        grid.addColumn(ReleaseComponent::notes).setHeader("Architectural Role").setAutoWidth(true);

        List<ReleaseComponent> components = List.of(
                new ReleaseComponent("SupplyGraph Core Domain", "0.1.0-SNAPSHOT", "PRODUCTION READY", "100% Pure Java 21 standard library; zero external dependencies"),
                new ReleaseComponent("Spring Boot", "3.4.1", "STABLE GA", "Modular monolithic application orchestration"),
                new ReleaseComponent("Vaadin Flow", "24.5.4", "STABLE GA", "Java-first enterprise UI component framework"),
                new ReleaseComponent("Google OR-Tools", "9.11.4210", "STABLE GA", "MIP and LP mathematical solver for multi-echelon optimization"),
                new ReleaseComponent("PostgreSQL Driver", "42.7.4", "STABLE GA", "ACID transactional relational data persistence"),
                new ReleaseComponent("Flyway Migration", "11.1.0", "STABLE GA", "Deterministic forward database schema migrations v1 through v15"),
                new ReleaseComponent("Hibernate ORM", "6.6.4.Final", "STABLE GA", "JPA persistence engine and dirty checking"),
                new ReleaseComponent("Testcontainers", "1.20.4", "STABLE GA", "Real PostgreSQL containerized integration verification"),
                new ReleaseComponent("ArchUnit", "1.3.0", "VERIFIED", "Enforcement of hexagonal architecture boundaries")
        );

        grid.setItems(components);
        grid.setHeight("400px");
        add(grid);

        add(new com.vaadin.flow.component.html.Paragraph("Component versions above are the build's declared dependencies. Capability status is documented in docs/product/SUPPLYGRAPH_INTENDED_VS_ACTUAL.md."));
    }
}
