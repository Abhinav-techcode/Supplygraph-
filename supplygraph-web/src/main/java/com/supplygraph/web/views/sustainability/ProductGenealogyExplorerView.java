package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.GenealogyEdge;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeId;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;

@Route(value = "sustainability/traceability/genealogy", layout = MainLayout.class)
@PageTitle("Product Genealogy Explorer | ORBIX")
public class ProductGenealogyExplorerView extends SustainabilityViewBase {

    private final ProductTraceabilityService traceabilityService;

    public ProductGenealogyExplorerView(ProductTraceabilityService traceabilityService) {
        super("Product Genealogy & Traceability DAG Explorer", "Directed Acyclic Graph (DAG) modeling multi-tier transformations from raw materials to final customer deliveries.");
        this.traceabilityService = traceabilityService;

        WorkspaceId ws = currentWorkspaceId();
        GenealogyDAG dag = traceabilityService.getOrCreateDag(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("DAG Nodes", String.valueOf(dag.getAllNodes().size()), "Traceable lots & batches", "primary"));
        kpiRow.add(createKpiCard("DAG Edges", String.valueOf(dag.getAllEdges().size()), "Genealogy transformation links", "success"));
        kpiRow.add(createKpiCard("Root Origins", String.valueOf(dag.getAllNodes().stream().filter(n -> n.type() == TraceabilityNodeType.RAW_MATERIAL_ORIGIN).count()), "Tier-3 raw inputs", "contrast"));
        kpiRow.add(createKpiCard("Finished Deliveries", String.valueOf(dag.getAllNodes().stream().filter(n -> n.type() == TraceabilityNodeType.SHIPMENT_CONSIGNMENT).count()), "Customer consignments", "warning"));

        add(kpiRow);

        Grid<TraceabilityNode> nodeGrid = new Grid<>();
        Grid<GenealogyEdge> edgeGrid = new Grid<>();

        add(new H3("Register Traceability Node"));
        HorizontalLayout nodeForm = new HorizontalLayout();
        nodeForm.setAlignItems(Alignment.END);

        TextField batchField = new TextField("Batch Number", "BAT-CELL-2026-X1");
        ComboBox<TraceabilityNodeType> typeBox = new ComboBox<>("Node Type");
        typeBox.setItems(TraceabilityNodeType.values());
        typeBox.setValue(TraceabilityNodeType.WORK_ORDER_BATCH);

        TextField matField = new TextField("Material ID", "MAT-NMC-811");
        TextField locField = new TextField("Location ID", "FAC-CELL-PLANT");
        NumberField qtyField = new NumberField("Quantity");
        qtyField.setValue(500.0);

        Button addNodeBtn = new Button("Add Node", e -> {
            if (batchField.getValue() == null || typeBox.getValue() == null || matField.getValue() == null || locField.getValue() == null || qtyField.getValue() == null) {
                Notification.show("Please enter all node attributes", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            TraceabilityNode node = new TraceabilityNode(
                    TraceabilityNodeId.generate(),
                    batchField.getValue(),
                    typeBox.getValue(),
                    new MaterialId(matField.getValue()),
                    new NodeId(locField.getValue()),
                    qtyField.getValue(),
                    "units",
                    "Internal Facility",
                    Instant.now()
            );
            traceabilityService.registerNode(ws, node);
            Notification.show("Registered node: " + batchField.getValue(), 3000, Notification.Position.TOP_CENTER);
            refreshGrids(nodeGrid, edgeGrid, ws);
        });
        addNodeBtn.getElement().getThemeList().add("primary");
        nodeForm.add(batchField, typeBox, matField, locField, qtyField, addNodeBtn);
        add(nodeForm);

        add(new H3("Traceability DAG Nodes"));
        nodeGrid.addColumn(n -> n.id().value()).setHeader("Node ID").setAutoWidth(true);
        nodeGrid.addColumn(TraceabilityNode::batchNumber).setHeader("Batch Number").setAutoWidth(true);
        nodeGrid.addComponentColumn(n -> {
            Span badge = new Span(n.type().name());
            badge.getElement().getThemeList().add("badge small contrast");
            return badge;
        }).setHeader("Type").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.materialId().value()).setHeader("Material").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.locationId().value()).setHeader("Location").setAutoWidth(true);
        nodeGrid.addColumn(r -> r.quantity() + " units").setHeader("Quantity").setAutoWidth(true);
        nodeGrid.addColumn(r -> r.recordedAt().toString()).setHeader("Timestamp").setAutoWidth(true);

        nodeGrid.setHeight("250px");
        add(nodeGrid);

        add(new H3("Genealogy Edges (Transformations)"));
        edgeGrid.addColumn(e -> e.sourceNodeId().value()).setHeader("Source Node ID").setAutoWidth(true);
        edgeGrid.addColumn(e -> e.targetNodeId().value()).setHeader("Target Node ID").setAutoWidth(true);
        edgeGrid.addColumn(e -> e.quantityTransferred() + " units").setHeader("Transferred Qty").setAutoWidth(true);
        edgeGrid.addColumn(GenealogyEdge::transformationDetails).setHeader("Process Details").setAutoWidth(true);

        edgeGrid.setHeight("250px");
        add(edgeGrid);

        refreshGrids(nodeGrid, edgeGrid, ws);
    }

    private void refreshGrids(Grid<TraceabilityNode> nodeGrid, Grid<GenealogyEdge> edgeGrid, WorkspaceId ws) {
        GenealogyDAG dag = traceabilityService.getOrCreateDag(ws);
        nodeGrid.setItems(dag.getAllNodes());
        edgeGrid.setItems(dag.getAllEdges());
    }
}
