package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "sustainability/traceability/origin", layout = MainLayout.class)
@PageTitle("Material Origin Explorer | ORBIX")
public class MaterialOriginExplorerView extends SustainabilityViewBase {

    private final ProductTraceabilityService traceabilityService;

    public MaterialOriginExplorerView(ProductTraceabilityService traceabilityService) {
        super("Material Origin & Provenance Explorer", "Backward traceability DAG traversal from finished products and assemblies back to Tier-3 raw material extraction origins.");
        this.traceabilityService = traceabilityService;

        WorkspaceId ws = currentWorkspaceId();
        GenealogyDAG dag = traceabilityService.getOrCreateDag(ws);

        HorizontalLayout searchBar = new HorizontalLayout();
        searchBar.setAlignItems(Alignment.END);

        ComboBox<TraceabilityNode> targetNodeBox = new ComboBox<>("Select Product / Lot for Origin Trace");
        targetNodeBox.setItems(dag.getAllNodes());
        targetNodeBox.setItemLabelGenerator(n -> n.batchNumber() + " [" + n.type().name() + " @ " + n.locationId().value() + "]");
        targetNodeBox.setWidth("450px");

        Grid<TraceabilityNode> originGrid = new Grid<>();

        Button traceBtn = new Button("Trace Backward to Origin", e -> {
            TraceabilityNode selected = targetNodeBox.getValue();
            if (selected == null) {
                Notification.show("Please select a lot to trace", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            List<TraceabilityNode> originNodes = traceabilityService.traceBackwardToOrigin(ws, selected.id());
            originGrid.setItems(originNodes);
            Notification.show("Discovered " + originNodes.size() + " upstream origin nodes", 3000, Notification.Position.TOP_CENTER);
        });
        traceBtn.getElement().getThemeList().add("primary");

        searchBar.add(targetNodeBox, traceBtn);
        add(searchBar);

        add(new H3("Upstream Provenance & Origin Nodes"));
        originGrid.addColumn(n -> n.id().value()).setHeader("Node ID").setAutoWidth(true);
        originGrid.addColumn(TraceabilityNode::batchNumber).setHeader("Batch / Lot Number").setAutoWidth(true);
        originGrid.addComponentColumn(n -> {
            Span badge = new Span(n.type().name());
            badge.getElement().getThemeList().add("badge small " + (n.type() == TraceabilityNodeType.RAW_MATERIAL_ORIGIN ? "success" : "contrast"));
            return badge;
        }).setHeader("Tier Type").setAutoWidth(true);
        originGrid.addColumn(n -> n.materialId().value()).setHeader("Material ID").setAutoWidth(true);
        originGrid.addColumn(n -> n.locationId().value()).setHeader("Origin Location / Mine").setAutoWidth(true);
        originGrid.addColumn(n -> n.quantity() + " units").setHeader("Transferred Volume").setAutoWidth(true);
        originGrid.addColumn(n -> n.recordedAt().toString()).setHeader("Certified Timestamp").setAutoWidth(true);

        originGrid.setHeight("400px");
        add(originGrid);
    }
}
