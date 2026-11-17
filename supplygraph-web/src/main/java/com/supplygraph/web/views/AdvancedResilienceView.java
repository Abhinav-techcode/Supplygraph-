package com.supplygraph.web.views;

import com.supplygraph.application.resilience.AdvancedResilienceAnalyticsService;
import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;
import com.supplygraph.domain.model.resilience.advanced.NodeResilienceMetric;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "advanced-resilience", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Advanced Network Resilience")
public class AdvancedResilienceView extends VerticalLayout {

    private final AdvancedResilienceAnalyticsService resilienceService;
    private final HorizontalLayout kpiCardsLayout = new HorizontalLayout();
    private final Grid<NodeResilienceMetric> nodeGrid = new Grid<>();

    public AdvancedResilienceView() {
        this(null);
    }

    @Autowired
    public AdvancedResilienceView(AdvancedResilienceAnalyticsService resilienceService) {
        this.resilienceService = resilienceService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Advanced Supply Chain Resilience & Redundancy Analytics");
        Paragraph subtitle = new Paragraph("Quantifies operational survival horizons (Time-to-Survive, TTS), recovery restoration duration (Time-to-Recover, TTR), single-point bottleneck exposure, and multi-echelon path redundancy.");

        Button refreshBtn = new Button("Recalculate Network Resilience", VaadinIcon.REFRESH.create(), e -> refreshData());

        kpiCardsLayout.setWidthFull();

        configureGrid();

        add(header, subtitle, refreshBtn, kpiCardsLayout, new H3("Node-Level Survival & Bottleneck Risk Profile"), nodeGrid);
        refreshData();
    }

    private void configureGrid() {
        nodeGrid.setHeight("350px");
        nodeGrid.addColumn(NodeResilienceMetric::nodeId).setHeader("Node ID").setAutoWidth(true);
        nodeGrid.addColumn(NodeResilienceMetric::nodeName).setHeader("Facility / Supplier Name").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.localTTSDays() + " days").setHeader("Local TTS (Survival)").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.localTTRDays() + " days").setHeader("Local TTR (Recovery)").setAutoWidth(true);
        nodeGrid.addColumn(NodeResilienceMetric::alternatePathsCount).setHeader("Redundant Paths").setAutoWidth(true);

        nodeGrid.addComponentColumn(n -> {
            Span badge = new Span(String.format("%.0f%%", n.dependencyCriticality() * 100));
            if (n.dependencyCriticality() > 0.75) {
                badge.getElement().getThemeList().add("badge error");
            } else if (n.dependencyCriticality() > 0.5) {
                badge.getElement().getThemeList().add("badge warning");
            } else {
                badge.getElement().getThemeList().add("badge success");
            }
            return badge;
        }).setHeader("Criticality Index").setAutoWidth(true);
    }

    private void refreshData() {
        if (resilienceService == null) return;

        AdvancedResilienceMetrics metrics = resilienceService.findLatestByWorkspaceId("default-workspace")
                .orElseGet(() -> resilienceService.evaluateWorkspaceResilience("default-workspace"));

        kpiCardsLayout.removeAll();
        kpiCardsLayout.add(
                createCard("Time-to-Survive (TTS)", String.format("%.1f Days", metrics.timeToSurviveDays()), "Network operational buffer before stockout"),
                createCard("Time-to-Recover (TTR)", String.format("%.1f Days", metrics.timeToRecoverDays()), "Mean duration to restore full operational capacity"),
                createCard("Critical Exposure", String.format("%.0f%%", metrics.criticalSupplierExposureIndex() * 100), metrics.bottleneckNodeCount() + " single-path bottlenecks identified"),
                createCard("Resilience Index", String.format("%.1f / 100", metrics.compositeResilienceScore()), "Composite network fault-tolerance rating")
        );

        nodeGrid.setItems(metrics.nodeResilienceBreakdown());
    }

    private VerticalLayout createCard(String title, String value, String sub) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("background", "var(--lumo-contrast-5pct)");
        card.getStyle().set("border-radius", "8px");
        card.setPadding(true);
        card.setSpacing(false);

        Span l = new Span(title);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)");
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");

        H3 v = new H3(value);
        v.getStyle().set("margin", "6px 0");

        Span s = new Span(sub);
        s.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        card.add(l, v, s);
        return card;
    }
}
