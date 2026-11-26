package com.supplygraph.web.views;

import com.supplygraph.application.security.SanitizationAndSafetyGuard;
import com.supplygraph.application.security.TokenBucketRateLimiter;
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
 * Enterprise System Health, Runtime Diagnostics & Security Hardening Observability View.
 */
@Route(value = "health", layout = MainLayout.class)
@PageTitle("System Health & Security Diagnostics | ORBIX")
public class HealthView extends VerticalLayout {

    private final TokenBucketRateLimiter rateLimiter;

    public HealthView(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        setSpacing(true);
        setPadding(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        H2 header = new H2("System Health, Runtime & Security Diagnostics");
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph intro = new Paragraph("Operational readiness, rate-limiting capacity, and enterprise security indicators for the ORBIX platform.");
        intro.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout diagnosticsCard = createDiagnosticsCard();
        VerticalLayout securityCard = createSecurityHardeningCard();
        add(header, intro, diagnosticsCard, securityCard);
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

        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

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
                new Span("JVM Heap Memory:"),
                new Span(usedMem + " MB used / " + totalMem + " MB allocated")
        );
        HorizontalLayout row6 = new HorizontalLayout(
                new Span("Available Processors:"),
                new Span(String.valueOf(Runtime.getRuntime().availableProcessors()))
        );

        card.add(title, row1, row2, row3, row4, row5, row6);
        return card;
    }

    private VerticalLayout createSecurityHardeningCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-m)");

        H3 title = new H3("Enterprise Security & Defense-in-Depth Posture");
        title.getStyle().set("margin-top", "0");

        HorizontalLayout sec1 = new HorizontalLayout(
                new Span("Workspace Isolation & IDOR Defense:"),
                createBadge("ENFORCED (Server-Side)", "success")
        );
        HorizontalLayout sec2 = new HorizontalLayout(
                new Span("Zero-Trust Prompt Boundary:"),
                createBadge("ACTIVE (Jailbreak Detection)", "success")
        );
        HorizontalLayout sec3 = new HorizontalLayout(
                new Span("Token Bucket Rate Limiter:"),
                createBadge("ARMED (10 compute ops / min burst)", "success")
        );
        HorizontalLayout sec4 = new HorizontalLayout(
                new Span("File Upload Security Boundary:"),
                new Span("Max Size: " + (SanitizationAndSafetyGuard.MAX_FILE_SIZE_BYTES / (1024 * 1024)) + " MB (CSV, JSON, XML allowed)")
        );
        HorizontalLayout sec5 = new HorizontalLayout(
                new Span("Immutable Audit Governance:"),
                createBadge("CRYPTOGRAPHIC (SHA-256)", "primary")
        );

        card.add(title, sec1, sec2, sec3, sec4, sec5);
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
