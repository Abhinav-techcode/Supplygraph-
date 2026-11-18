package com.supplygraph.web.views;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.collaboration.RfqDocument;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.sustainability.TraceabilityNode;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "command-center", layout = MainLayout.class)
@PageTitle("Unified Command Centre | SupplyGraph")
public class UnifiedCommandCentreView extends VerticalLayout {

    public record SearchResultItem(String category, String id, String title, String status, String targetRoute) {}

    private final DisruptionApplicationService disruptionService;
    private final OrderCollaborationService orderService;
    private final RfqCollaborationService rfqService;
    private final SupplierQualityCollaborationService qualityService;
    private final ProductTraceabilityService traceabilityService;
    private final com.supplygraph.application.graph.NetworkGraphViewService graphService;

    public UnifiedCommandCentreView(
            DisruptionApplicationService disruptionService,
            OrderCollaborationService orderService,
            RfqCollaborationService rfqService,
            SupplierQualityCollaborationService qualityService,
            ProductTraceabilityService traceabilityService,
            com.supplygraph.application.graph.NetworkGraphViewService graphService
    ) {
        this.graphService = graphService;
        this.disruptionService = disruptionService;
        this.orderService = orderService;
        this.rfqService = rfqService;
        this.qualityService = qualityService;
        this.traceabilityService = traceabilityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Unified Enterprise Command Centre");
        header.getStyle().set("margin-bottom", "0");
        Paragraph sub = new Paragraph("Permission-aware global search across all network nodes, purchase orders, disruptions, RFQs, lots, and incidents.");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "var(--lumo-space-xs)");

        HorizontalLayout topBar = new HorizontalLayout(header);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.BASELINE);
        Span badge = new Span("Command centre");
        badge.getElement().getThemeList().add("badge small primary");
        topBar.add(badge);

        add(topBar, sub);

        WorkspaceId ws = currentWorkspaceId();

        TextField searchBar = new TextField();
        searchBar.setPlaceholder("Global Search (e.g. 'PO', 'CATL', 'BAT', 'Disruption', 'Lithium', '8D')...");
        searchBar.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchBar.setWidth("650px");
        searchBar.setClearButtonVisible(true);
        searchBar.setValueChangeMode(ValueChangeMode.EAGER);

        Grid<SearchResultItem> resultGrid = new Grid<>();
        resultGrid.addComponentColumn(r -> {
            Span b = new Span(r.category());
            b.getElement().getThemeList().add("badge small contrast");
            return b;
        }).setHeader("Category").setAutoWidth(true);
        resultGrid.addColumn(SearchResultItem::id).setHeader("Identifier").setAutoWidth(true);
        resultGrid.addColumn(SearchResultItem::title).setHeader("Title / Summary").setAutoWidth(true);
        resultGrid.addColumn(SearchResultItem::status).setHeader("Status").setAutoWidth(true);
        resultGrid.addComponentColumn(r -> new Button("Open", VaadinIcon.ARROW_RIGHT.create(), e -> getUI().ifPresent(ui -> ui.navigate(r.targetRoute())))).setHeader("Action").setAutoWidth(true);

        Span noResults = new Span("No matches. Search covers network facilities, disruptions, purchase orders, RFQs, quality incidents and traceability lots in the current workspace.");
        noResults.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Runnable doSearch = () -> {
            String query = searchBar.getValue() != null ? searchBar.getValue().toLowerCase().trim() : "";
            List<SearchResultItem> results = new ArrayList<>();

            // Network facilities and lanes from the persisted twin
            var network = graphService.build(ws);
            for (var n : network.nodes()) {
                if (query.isEmpty() || n.id().toLowerCase().contains(query) || n.name().toLowerCase().contains(query) || n.location().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("FACILITY", n.id(), n.name() + " — " + n.location(), n.disrupted() ? "DISRUPTED" : "OPERATIONAL", "digital-twin"));
                }
            }

            // Disruptions
            for (Disruption d : disruptionService.listDisruptions(ws)) {
                if (query.isEmpty() || d.title().toLowerCase().contains(query) || d.id().value().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("DISRUPTION", d.id().value(), d.title(), d.status().name(), "recommendations-explainable"));
                }
            }

            // Purchase Orders
            for (PoCollaborationRecord po : orderService.listPos(ws)) {
                if (query.isEmpty() || po.poNumber().toLowerCase().contains(query) || po.materialId().value().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("PURCHASE_ORDER", po.poNumber(), po.materialId().value() + " (" + po.orderQuantity() + " units)", po.status().name(), "portal/orders"));
                }
            }

            // RFQs
            for (RfqDocument rfq : rfqService.listRfqs(ws)) {
                if (query.isEmpty() || rfq.title().toLowerCase().contains(query) || rfq.targetMaterialId().value().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("RFQ_TENDER", rfq.id().value(), rfq.title(), rfq.status().name(), "collaboration/rfq"));
                }
            }

            // Quality 8D
            for (QualityIncident8D inc : qualityService.listIncidents(ws)) {
                if (query.isEmpty() || inc.defectTitle().toLowerCase().contains(query) || inc.id().value().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("QUALITY_8D", inc.id().value(), inc.defectTitle(), inc.currentStep().name(), "collaboration/quality"));
                }
            }

            // Traceability Nodes
            for (TraceabilityNode node : traceabilityService.getOrCreateDag(ws).getAllNodes()) {
                if (query.isEmpty() || node.batchNumber().toLowerCase().contains(query) || node.materialId().value().toLowerCase().contains(query)) {
                    results.add(new SearchResultItem("TRACE_LOT", node.batchNumber(), node.materialId().value() + " @ " + node.locationId().value(), node.type().name(), "sustainability/traceability/lots"));
                }
            }

            resultGrid.setItems(results);
            noResults.setVisible(results.isEmpty());
        };

        searchBar.addValueChangeListener(e -> doSearch.run());
        doSearch.run();

        add(searchBar, new H3("Search results"), noResults, resultGrid);

        HorizontalLayout personaShortcuts = new HorizontalLayout();
        personaShortcuts.add(new Button("Executive", VaadinIcon.DASHBOARD.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/executive"))));
        personaShortcuts.add(new Button("Planner", VaadinIcon.CALC_BOOK.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/planner"))));
        personaShortcuts.add(new Button("Procurement", VaadinIcon.CART.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/procurement"))));
        personaShortcuts.add(new Button("Production", VaadinIcon.FACTORY.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/production"))));
        personaShortcuts.add(new Button("Logistics", VaadinIcon.TRUCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/logistics"))));
        personaShortcuts.add(new Button("Quality", VaadinIcon.DIPLOMA.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/quality"))));
        personaShortcuts.add(new Button("Analyst", VaadinIcon.SLIDERS.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/analyst"))));
        personaShortcuts.add(new Button("Supplier", VaadinIcon.EXTERNAL_LINK.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/supplier"))));
        personaShortcuts.add(new Button("Admin", VaadinIcon.LOCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("workspace/admin"))));

        add(new H3("Role-Specific Enterprise Workspaces"), personaShortcuts);
    }

    private WorkspaceId currentWorkspaceId() {
        UserProfile user = SecurityContext.getCurrentUser();
        if (user != null && !user.assignedWorkspaces().isEmpty()) {
            return user.assignedWorkspaces().iterator().next();
        }
        return new WorkspaceId("ws-default");
    }
}
