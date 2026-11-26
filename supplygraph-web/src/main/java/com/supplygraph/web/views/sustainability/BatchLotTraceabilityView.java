package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.domain.model.sustainability.TraceabilityNodeType;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "sustainability/traceability/lots", layout = MainLayout.class)
@PageTitle("Batch & Lot Traceability | ORBIX")
public class BatchLotTraceabilityView extends SustainabilityViewBase {

    private final ProductTraceabilityService traceabilityService;

    public BatchLotTraceabilityView(ProductTraceabilityService traceabilityService) {
        super("Batch & Lot Traceability Ledger", "Granular unit-level and lot-level tracking across all inventory staging locations and manufacturing lines.");
        this.traceabilityService = traceabilityService;

        WorkspaceId ws = currentWorkspaceId();
        GenealogyDAG dag = traceabilityService.getOrCreateDag(ws);
        List<TraceabilityNode> nodes = dag.getAllNodes();

        HorizontalLayout filterRow = new HorizontalLayout();
        filterRow.setAlignItems(Alignment.END);

        TextField searchField = new TextField("Search Batch Number");
        searchField.setPlaceholder("e.g. LOT-2026...");
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        ComboBox<TraceabilityNodeType> typeFilter = new ComboBox<>("Filter by Lot Type");
        typeFilter.setItems(TraceabilityNodeType.values());
        typeFilter.setClearButtonVisible(true);

        Grid<TraceabilityNode> grid = new Grid<>();
        grid.addColumn(n -> n.id().value()).setHeader("Trace ID").setAutoWidth(true);
        grid.addColumn(TraceabilityNode::batchNumber).setHeader("Batch / Lot #").setAutoWidth(true);
        grid.addComponentColumn(n -> {
            Span badge = new Span(n.type().name());
            badge.getElement().getThemeList().add("badge small " + switch (n.type()) {
                case RAW_MATERIAL_ORIGIN -> "success";
                case SUPPLIER_BATCH, WORK_ORDER_BATCH -> "primary";
                case INVENTORY_RECEIPT -> "contrast";
                case FINISHED_PRODUCT_LOT -> "success";
                case SHIPMENT_CONSIGNMENT -> "warning";
            });
            return badge;
        }).setHeader("Category").setAutoWidth(true);
        grid.addColumn(n -> n.materialId().value()).setHeader("Material Part #").setAutoWidth(true);
        grid.addColumn(n -> n.locationId().value()).setHeader("Current Location").setAutoWidth(true);
        grid.addColumn(n -> n.quantity() + " units").setHeader("Lot Quantity").setAutoWidth(true);
        grid.addColumn(n -> n.recordedAt().toString()).setHeader("Production Date").setAutoWidth(true);

        grid.setItems(nodes);
        grid.setHeight("450px");

        Runnable updateFilter = () -> {
            String text = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
            TraceabilityNodeType type = typeFilter.getValue();

            List<TraceabilityNode> filtered = nodes.stream()
                    .filter(n -> text.isEmpty() || n.batchNumber().toLowerCase().contains(text))
                    .filter(n -> type == null || n.type() == type)
                    .toList();
            grid.setItems(filtered);
        };

        searchField.addValueChangeListener(e -> updateFilter.run());
        typeFilter.addValueChangeListener(e -> updateFilter.run());

        filterRow.add(searchField, typeFilter);
        add(filterRow, new H3("Traceability Records"), grid);
    }
}
