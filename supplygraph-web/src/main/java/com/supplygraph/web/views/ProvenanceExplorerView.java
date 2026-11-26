package com.supplygraph.web.views;

import com.supplygraph.application.provenance.DataProvenanceService;
import com.supplygraph.domain.model.provenance.ProvenanceRecord;
import com.supplygraph.domain.model.provenance.ProvenanceType;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "provenance", layout = MainLayout.class)
@PageTitle("Data Provenance & Traceability | ORBIX")
public class ProvenanceExplorerView extends VerticalLayout {

    private final DataProvenanceService provenanceService;
    private final Grid<ProvenanceRecord> provenanceGrid;
    private final ComboBox<String> typeFilter;

    public ProvenanceExplorerView(DataProvenanceService provenanceService) {
        this.provenanceService = provenanceService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Data Provenance, Asset Classification & Lineage");
        Paragraph desc = new Paragraph("Strict separation between authoritative ERP/telemetry facts, synthetic fixtures, simulation outputs, and non-authoritative AI agent proposals.");

        typeFilter = new ComboBox<>("Provenance Classification");
        typeFilter.setItems("ALL", "REAL_IMPORTED", "MANUAL_OPERATOR", "SYNTHETIC_DEVELOPMENT", "SIMULATION_OUTPUT", "AI_PROPOSAL", "DETERMINISTIC_CALCULATION");
        typeFilter.setValue("ALL");
        typeFilter.addValueChangeListener(e -> refreshRecords());

        HorizontalLayout toolbar = new HorizontalLayout(typeFilter);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        VerticalLayout lineageCard = createLineageWorkflowCard();

        provenanceGrid = new Grid<>();
        provenanceGrid.setSizeFull();

        provenanceGrid.addColumn(p -> p.id().value()).setHeader("Provenance ID").setAutoWidth(true);
        provenanceGrid.addColumn(ProvenanceRecord::entityId).setHeader("Asset / Entity ID").setAutoWidth(true);
        provenanceGrid.addColumn(ProvenanceRecord::entityType).setHeader("Type").setAutoWidth(true);

        provenanceGrid.addComponentColumn(rec -> {
            Span badge = new Span(rec.type().name());
            String theme = switch (rec.type()) {
                case REAL_IMPORTED, MANUAL_OPERATOR -> "badge success";
                case DETERMINISTIC_CALCULATION -> "badge primary";
                case AI_PROPOSAL -> "badge error"; // non-authoritative
                case SIMULATION_OUTPUT -> "badge";
                case SYNTHETIC_DEVELOPMENT -> "badge contrast";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Data Classification").setAutoWidth(true);

        provenanceGrid.addColumn(ProvenanceRecord::sourceReference).setHeader("Source Reference").setAutoWidth(true);
        provenanceGrid.addColumn(ProvenanceRecord::stateVersion).setHeader("Twin Version").setAutoWidth(true);
        provenanceGrid.addColumn(p -> p.timestamp().toString()).setHeader("Ingestion Timestamp").setAutoWidth(true);
        provenanceGrid.addColumn(p -> p.attributes().toString()).setHeader("Attributes & Metadata").setWidth("280px");

        add(title, desc, lineageCard, toolbar, provenanceGrid);
        refreshRecords();
    }

    private VerticalLayout createLineageWorkflowCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)");

        H3 title = new H3("End-to-End Decision Traceability Chain");
        Paragraph subtitle = new Paragraph("Every recovery recommendation retains verifiable pointers across each phase of the intelligence pipeline:");

        HorizontalLayout steps = new HorizontalLayout();
        steps.setWidthFull();
        steps.setJustifyContentMode(JustifyContentMode.BETWEEN);

        steps.add(
                createStepBadge("1. Disruption Trigger", "Telemetry / Event"),
                createArrow(),
                createStepBadge("2. Ingested Evidence", "ERP / Sensor Provenance"),
                createArrow(),
                createStepBadge("3. Twin State Snapshot", "StateVersion Check"),
                createArrow(),
                createStepBadge("4. Impact Assessment", "Multi-Tier Subgraph"),
                createArrow(),
                createStepBadge("5. ConcurGuard Arbitration", "Conflict Resolution"),
                createArrow(),
                createStepBadge("6. Simulation & Solver", "Discrete-Event / OR-Tools"),
                createArrow(),
                createStepBadge("7. Human Manager Sign-Off", "Cryptographic Audit Log")
        );

        card.add(title, subtitle, steps);
        return card;
    }

    private VerticalLayout createStepBadge(String step, String detail) {
        VerticalLayout stepLayout = new VerticalLayout();
        stepLayout.setPadding(false);
        stepLayout.setSpacing(false);
        Span s = new Span(step);
        s.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-s)");
        Span d = new Span(detail);
        d.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");
        stepLayout.add(s, d);
        return stepLayout;
    }

    private Span createArrow() {
        Span arrow = new Span("→");
        arrow.getStyle().set("font-size", "var(--lumo-font-size-l)").set("color", "var(--lumo-contrast-40pct)");
        return arrow;
    }

    private void refreshRecords() {
        List<ProvenanceRecord> records = provenanceService.getAllRecords();
        String sel = typeFilter.getValue();
        if (sel != null && !"ALL".equalsIgnoreCase(sel)) {
            records = records.stream().filter(r -> sel.equalsIgnoreCase(r.type().name())).toList();
        }
        provenanceGrid.setItems(records);
    }
}

