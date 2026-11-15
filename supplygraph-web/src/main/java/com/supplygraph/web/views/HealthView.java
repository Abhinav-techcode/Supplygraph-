package com.supplygraph.web.views;

import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;

/**
 * Platform Foundation Health and Runtime Diagnostics View.
 */
@Route(value = "health", layout = MainLayout.class)
@PageTitle("SupplyGraph :: System Health")
public class HealthView extends VerticalLayout {

    public HealthView() {
        setSpacing(true);
        setPadding(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        H2 header = new H2("System Health & Runtime Diagnostics");
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph intro = new Paragraph("Operational readiness indicators for the SupplyGraph platform foundation.");
        intro.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout diagnosticsCard = createDiagnosticsCard();
        add(header, intro, diagnosticsCard);
    }

    private VerticalLayout createDiagnosticsCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)");

        H3 title = new H3("Operational Diagnostics");
        title.getStyle().set("margin-top", "0");

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtime.getUptime();
        String uptimeFormatted = formatUptime(uptimeMs);

        HorizontalLayout row1 = new HorizontalLayout(
                new Span("Application Status:"),
                createBadge("OPERATIONAL (UP)", "success")
        );
        HorizontalLayout row2 = new HorizontalLayout(
                new Span("Actuator Health Endpoint:"),
                createBadge("/actuator/health", "contrast")
        );
        HorizontalLayout row3 = new HorizontalLayout(
                new Span("JVM Runtime:"),
                new Span(System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")")
        );
        HorizontalLayout row4 = new HorizontalLayout(
                new Span("System Uptime:"),
                new Span(uptimeFormatted)
        );
        HorizontalLayout row5 = new HorizontalLayout(
                new Span("Available Processors:"),
                new Span(String.valueOf(Runtime.getRuntime().availableProcessors()))
        );

        card.add(title, row1, row2, row3, row4, row5);
        return card;
    }

    private Span createBadge(String text, String theme) {
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge " + theme);
        return badge;
    }

    private String formatUptime(long uptimeMs) {
        Duration d = Duration.ofMillis(uptimeMs);
        return "%d min, %d sec".formatted(d.toMinutes(), d.toSecondsPart());
    }
}

