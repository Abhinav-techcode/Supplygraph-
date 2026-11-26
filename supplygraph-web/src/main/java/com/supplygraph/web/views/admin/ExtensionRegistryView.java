package com.supplygraph.web.views.admin;

import com.supplygraph.industry.api.IndustryPackExtension;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

@PageTitle("Industry Extension Registry | ORBIX")
@Route(value = "admin/extensions", layout = MainLayout.class)
public class ExtensionRegistryView extends PlatformAdminViewBase {

    public ExtensionRegistryView(@Autowired(required = false) List<IndustryPackExtension> extensions) {
        super("Industry Extension Registry", "Service Provider Interface (SPI) registry of pluggable industry intelligence modules and domain rulesets.");

        List<IndustryPackExtension> packList = extensions != null ? extensions : Collections.emptyList();

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.add(
                createKpiCard("Active Industry Packs", String.valueOf(packList.size()), "Loaded SPI Modules", "primary"),
                createKpiCard("SPI Specification", "v1.0", "Decoupled Architecture", "success"),
                createKpiCard("Extensibility Status", "PLUGGABLE", "Core Remains Unmodified", "success")
        );
        add(kpis);

        H3 gridTitle = new H3("Registered Industry Intelligence Packs");
        gridTitle.getStyle().set("margin-top", "var(--lumo-space-l)");
        add(gridTitle);

        Grid<IndustryPackExtension> grid = new Grid<>();
        grid.addColumn(IndustryPackExtension::getDisplayName).setHeader("Pack Name").setAutoWidth(true);
        grid.addColumn(p -> p.getId() != null ? p.getId().toString() : "UNKNOWN").setHeader("Identifier");
        grid.addColumn(IndustryPackExtension::getVersion).setHeader("Version");
        grid.addColumn(IndustryPackExtension::getDescription).setHeader("Description").setAutoWidth(true);
        grid.addColumn(p -> String.join(", ", p.getSupportedScenarioCodes())).setHeader("Scenario Codes");
        grid.addColumn(p -> p.getCriticalMaterials().size() + " materials").setHeader("Critical Materials");

        grid.addComponentColumn(p -> {
            Span badge = new Span("ACTIVE");
            badge.getElement().getThemeList().add("badge success small");
            return badge;
        }).setHeader("Status");

        grid.setItems(packList);
        grid.setHeight("400px");
        add(grid);

        VerticalLayout detailCard = new VerticalLayout();
        detailCard.addClassNames("p-m", "border", "rounded-m");
        detailCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("margin-top", "var(--lumo-space-m)");

        detailCard.add(
                new H3("SPI Extensibility Architecture"),
                new Span("• Pluggable SPI: New industry domains are integrated by implementing `IndustryPackExtension` without altering core domain code."),
                new Span("• Dynamic Isolation: Industry policies, bill-of-materials structures, and validation algorithms execute inside their respective bounded contexts."),
                new Span("• Multi-Domain Operations: A single ORBIX instance can concurrently evaluate multi-tier networks across automotive, electronics, and aerospace domains.")
        );
        add(detailCard);
    }
}
