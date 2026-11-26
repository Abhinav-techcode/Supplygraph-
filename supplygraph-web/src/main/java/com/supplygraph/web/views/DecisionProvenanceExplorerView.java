package com.supplygraph.web.views;

import com.supplygraph.application.provenance.DecisionProvenanceGraphService;
import com.supplygraph.domain.model.provenance.graph.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "decision-provenance", layout = MainLayout.class)
@PageTitle("ORBIX :: Decision Provenance Explorer")
public class DecisionProvenanceExplorerView extends VerticalLayout {

    private final DecisionProvenanceGraphService provenanceService;
    private final TextField disruptionIdField = new TextField("Disruption ID / Filter");
    private final Grid<DecisionProvenanceNode> nodesGrid = new Grid<>();
    private final Grid<DecisionProvenanceEdge> edgesGrid = new Grid<>();
    private final VerticalLayout detailsContainer = new VerticalLayout();

    public DecisionProvenanceExplorerView() {
        this(null);
    }

    @Autowired
    public DecisionProvenanceExplorerView(DecisionProvenanceGraphService provenanceService) {
        this.provenanceService = provenanceService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("End-to-End Decision Provenance Explorer");
        Paragraph subtitle = new Paragraph("Causal and analytical provenance DAG tracing every recovery recommendation back to its originating disruption, telemetry, agent investigations, ConcurGuard arbitration, and audited human approval.");

        HorizontalLayout toolbar = new HorizontalLayout();
        disruptionIdField.setValue("DISR-ROTTERDAM-01");
        disruptionIdField.setWidth("300px");
        Button loadBtn = new Button("Trace Provenance DAG", VaadinIcon.SEARCH.create(), e -> loadGraph(disruptionIdField.getValue()));
        toolbar.add(disruptionIdField, loadBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        configureGrids();

        HorizontalLayout gridsLayout = new HorizontalLayout(nodesGrid, edgesGrid);
        gridsLayout.setSizeFull();

        add(header, subtitle, toolbar, gridsLayout, detailsContainer);

        loadGraph(disruptionIdField.getValue());
    }

    private void configureGrids() {
        nodesGrid.setHeight("350px");
        nodesGrid.setWidth("60%");
        nodesGrid.addColumn(n -> n.id().value()).setHeader("Node ID").setAutoWidth(true);
        nodesGrid.addComponentColumn(n -> {
            Span badge = new Span(n.type().getDisplayName());
            badge.getElement().getThemeList().add("badge");
            return badge;
        }).setHeader("Stage / Category").setAutoWidth(true);
        nodesGrid.addColumn(DecisionProvenanceNode::label).setHeader("Artifact Description").setAutoWidth(true);
        nodesGrid.addColumn(n -> n.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);

        edgesGrid.setHeight("350px");
        edgesGrid.setWidth("40%");
        edgesGrid.addColumn(e -> e.sourceNodeId().value() + " -> " + e.targetNodeId().value()).setHeader("Causal Link").setAutoWidth(true);
        edgesGrid.addComponentColumn(e -> {
            Span badge = new Span(e.type().getDisplayName());
            badge.getElement().getThemeList().add("badge contrast");
            return badge;
        }).setHeader("Relationship").setAutoWidth(true);
        edgesGrid.addColumn(DecisionProvenanceEdge::explanation).setHeader("Derivation Rationale").setAutoWidth(true);

        nodesGrid.asSingleSelect().addValueChangeListener(event -> {
            DecisionProvenanceNode selected = event.getValue();
            if (selected != null) {
                displayNodeDetails(selected);
            }
        });
    }

    private void displayNodeDetails(DecisionProvenanceNode node) {
        detailsContainer.removeAll();
        detailsContainer.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        detailsContainer.getStyle().set("border-radius", "8px");
        detailsContainer.setPadding(true);

        H3 title = new H3(node.label() + " (" + node.type().getDisplayName() + ")");
        Paragraph meta = new Paragraph("Entity Reference: " + node.entityReferenceId() + " | Timestamp: " + node.timestamp());

        UnorderedList attributes = new UnorderedList();
        node.metadata().forEach((k, v) -> attributes.add(new ListItem(k + ": " + v)));

        detailsContainer.add(title, meta, new H4("Provenance Stage Metadata"), attributes);
    }

    private void loadGraph(String disruptionId) {
        if (provenanceService == null) return;
        DecisionProvenanceGraph graph = provenanceService.findByRootDisruptionId(disruptionId)
                .orElseGet(() -> provenanceService.buildAndSaveProvenanceGraph("WS-AUTO-01", disruptionId, "USR-SCM-01"));

        nodesGrid.setItems(graph.getAllNodes());
        edgesGrid.setItems(graph.edges());

        if (!graph.getAllNodes().isEmpty()) {
            displayNodeDetails(graph.getAllNodes().get(0));
        }
    }
}

