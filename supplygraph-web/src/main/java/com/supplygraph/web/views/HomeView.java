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

/**
 * Platform Foundation Landing View.
 * Displays foundation status and architectural readiness.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Overview")
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSpacing(true);
        setPadding(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        H2 header = new H2("Supply Chain Intelligence & Conflict-Aware Recovery Platform");
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph intro = new Paragraph(
                "SupplyGraph is a software-only, Java-first enterprise platform designed to model complex multi-tier "
                        + "supply networks, identify cascading disruptions, coordinate bounded AI recovery agents, "
                        + "arbitrate plan conflicts via ConcurGuard, and support human governance."
        );
        intro.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout statusCard = createStatusCard();
        VerticalLayout architectureCard = createArchitectureCard();

        add(header, intro, statusCard, architectureCard);
    }

    private VerticalLayout createStatusCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)");

        H3 title = new H3("Foundation Status");
        title.getStyle().set("margin-top", "0");

        HorizontalLayout row1 = new HorizontalLayout(
                new Span("Build Toolchain:"),
                createBadge("Maven Wrapper (Maven 3.9.9)", "success")
        );
        HorizontalLayout row2 = new HorizontalLayout(
                new Span("Language & Bytecode Target:"),
                createBadge("Java 21 LTS (--release 21)", "success")
        );
        HorizontalLayout row3 = new HorizontalLayout(
                new Span("Backend Runtime:"),
                createBadge("Spring Boot 3.4.1", "success")
        );
        HorizontalLayout row4 = new HorizontalLayout(
                new Span("Frontend Technology:"),
                createBadge("Vaadin Flow 24 (100% Java-Authored UI)", "success")
        );
        HorizontalLayout row5 = new HorizontalLayout(
                new Span("Database Migration Baseline:"),
                createBadge("Flyway 10.21 (PostgreSQL 16+)", "success")
        );

        Paragraph note = new Paragraph(
                "Phase 1 Foundation is successfully initialized. Business domain capabilities (Multi-Tier Digital Twin, "
                        + "ConcurGuard Conflict Detection, Google OR-Tools Optimization, and Agentic AI Investigation) "
                        + "are scheduled for subsequent gated phases."
        );
        note.getStyle()
                .set("font-style", "italic")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "var(--lumo-space-m)");

        card.add(title, row1, row2, row3, row4, row5, note);
        return card;
    }

    private VerticalLayout createArchitectureCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-top", "var(--lumo-space-m)");

        H3 title = new H3("Active Architecture Modules");
        title.getStyle().set("margin-top", "0");

        Paragraph modules = new Paragraph(
                "• supplygraph-core-domain (Pure Java 21 Domain Invariants)\n"
                        + "• supplygraph-industry-api (Pluggable SPI Contracts)\n"
                        + "• supplygraph-concurguard (Semantic Conflict Detection Engine)\n"
                        + "• supplygraph-simulation-engine (Discrete-Event Simulator & OR-Tools)\n"
                        + "• supplygraph-core-application (Use Cases & Transaction Boundaries)\n"
                        + "• supplygraph-industry-ev (EV Battery Intelligence Pack)\n"
                        + "• supplygraph-industry-pharma (Pharmaceutical Intelligence Pack)\n"
                        + "• supplygraph-industry-semiconductor (Semiconductor Intelligence Pack)\n"
                        + "• supplygraph-industry-automotive (Automotive Intelligence Pack)\n"
                        + "• supplygraph-adapter-persistence (PostgreSQL & Flyway Migrations)\n"
                        + "• supplygraph-adapter-ai (Spring AI & Deterministic Fake AI Provider)\n"
                        + "• supplygraph-web (Vaadin Flow UI & Composition Root)"
        );
        modules.getStyle().set("white-space", "pre-line");

        card.add(title, modules);
        return card;
    }

    private Span createBadge(String text, String theme) {
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge " + theme);
        return badge;
    }
}

