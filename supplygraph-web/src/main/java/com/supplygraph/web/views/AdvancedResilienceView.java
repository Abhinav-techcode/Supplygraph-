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
@PageTitle("ORBIX :: Advanced Network Resilience")
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
        Paragraph subtitle = new Paragraph("Read from the digital twin: how long each manufacturing site can run from its own stock, which facilities are the only supplier of something downstream, and how much of the network depends on each facility. Figures the twin cannot supply are shown as \"not measured\".");

        Button refreshBtn = new Button("Recalculate Network Resilience", VaadinIcon.REFRESH.create(), e -> { resilienceService.evaluateWorkspaceResilience("WS-AUTO-01"); refreshData(); });

        kpiCardsLayout.setWidthFull();

        configureGrid();

        add(header, subtitle, refreshBtn, kpiCardsLayout, new H3("Node-Level Survival & Bottleneck Risk Profile"), nodeGrid);
        refreshData();
    }

    private void configureGrid() {
        nodeGrid.setHeight("350px");
        nodeGrid.addColumn(NodeResilienceMetric::nodeId).setHeader("Node ID").setAutoWidth(true);
        nodeGrid.addColumn(NodeResilienceMetric::nodeName).setHeader("Facility / Supplier Name").setAutoWidth(true);
        nodeGrid.addColumn(n -> days(n.localTTSDays())).setHeader("Days of own stock").setAutoWidth(true);
        nodeGrid.addColumn(n -> days(n.localTTRDays())).setHeader("Recovery time").setAutoWidth(true);
        nodeGrid.addColumn(NodeResilienceMetric::alternatePathsCount).setHeader("Lanes in + out").setAutoWidth(true);

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
        }).setHeader("Share of network downstream").setAutoWidth(true);
    }

    private void refreshData() {
        if (resilienceService == null) return;

        AdvancedResilienceMetrics metrics = resilienceService.findLatestByWorkspaceId("WS-AUTO-01")
                .orElseGet(() -> resilienceService.evaluateWorkspaceResilience("WS-AUTO-01"));

        kpiCardsLayout.removeAll();
        kpiCardsLayout.add(
                createCard("Time-to-Survive (TTS)", days(metrics.timeToSurviveDays()), "Average days a manufacturing site can run from its own stock at its stated throughput"),
                createCard("Time-to-Recover (TTR)", days(metrics.timeToRecoverDays()), "Recovery times are not recorded in the digital twin, so none is shown"),
                createCard("Critical Exposure", String.format("%.0f%%", metrics.criticalSupplierExposureIndex() * 100), metrics.bottleneckNodeCount() + " facilities are the only supplier of something downstream"),
                createCard("Structural resilience", String.format("%.0f / 100", metrics.compositeResilienceScore()), "100 minus the share of facilities that are single points of failure")
        );

        nodeGrid.setItems(metrics.nodeResilienceBreakdown());
    }

    private static String days(double v) { return v < 0 ? "not measured" : String.format("%.1f days", v); }

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

