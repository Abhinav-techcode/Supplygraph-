package com.supplygraph.web.views;

import com.supplygraph.application.graph.NetworkGraphView;
import com.supplygraph.application.graph.NetworkGraphViewService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.web.components.DigitalTwinNetworkGraphComponent;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Digital twin explorer: graph, node table and lane table are all rendered from the same {@link NetworkGraphView} of the persisted twin. */
@PageTitle("Supply Chain Network | ORBIX")
@Route(value = "digital-twin", layout = MainLayout.class)
public class DigitalTwinGraphExplorerView extends VerticalLayout {

    public DigitalTwinGraphExplorerView(NetworkGraphViewService graphService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        NetworkGraphView view = graphService.build(WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID));

        H2 header = new H2("Supply chain network");
        header.getStyle().set("margin-bottom", "0");
        Paragraph sub = new Paragraph("The persisted digital twin: facilities, the transport lanes between them, and the disruptions currently active against them.");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "var(--lumo-space-xs)");
        add(header, sub);

        add(new DigitalTwinNetworkGraphComponent(view));

        HorizontalLayout actions = new HorizontalLayout();
        Button disruptions = new Button("Disruptions", VaadinIcon.WARNING.create(), e -> UI.getCurrent().navigate(DisruptionsView.class));
        disruptions.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button impact = new Button("Impact analysis", VaadinIcon.SEARCH.create(), e -> UI.getCurrent().navigate(ImpactAssessmentView.class));
        impact.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button simulate = new Button("Monte Carlo risk simulation", VaadinIcon.PLAY.create(), e -> UI.getCurrent().navigate(MonteCarloWorkspaceView.class));
        Button optimize = new Button("Recovery optimization", VaadinIcon.SLIDERS.create(), e -> UI.getCurrent().navigate(OptimizationWorkspaceView.class));
        actions.add(disruptions, impact, simulate, optimize);
        add(actions);
        if (view.isEmpty()) return;

        add(new H3("Facilities"));
        Grid<NetworkGraphView.Node> nodes = new Grid<>();
        nodes.addColumn(NetworkGraphView.Node::id).setHeader("ID").setAutoWidth(true);
        nodes.addColumn(NetworkGraphView.Node::name).setHeader("Name").setAutoWidth(true);
        nodes.addColumn(n -> n.type().name().replace('_', ' ')).setHeader("Type").setAutoWidth(true);
        nodes.addColumn(NetworkGraphView.Node::location).setHeader("Location").setAutoWidth(true);
        nodes.addColumn(NetworkGraphView.Node::tierLabel).setHeader("Tier").setAutoWidth(true);
        nodes.addColumn(n -> String.format("%,.0f", n.onHandUnits())).setHeader("On hand").setAutoWidth(true);
        nodes.addColumn(NetworkGraphView.Node::capacityText).setHeader("Capacity / attributes").setAutoWidth(true);
        nodes.addComponentColumn(n -> {
            Span b = new Span(n.disrupted() ? "DISRUPTED" : "OPERATIONAL");
            b.getElement().getThemeList().add(n.disrupted() ? "badge error small" : "badge success small");
            return b;
        }).setHeader("Health").setAutoWidth(true);
        nodes.setItems(view.nodes());
        nodes.setHeight("350px");
        add(nodes);

        add(new H3("Transport lanes"));
        Grid<NetworkGraphView.Lane> lanes = new Grid<>();
        lanes.addColumn(NetworkGraphView.Lane::id).setHeader("Lane").setAutoWidth(true);
        lanes.addColumn(NetworkGraphView.Lane::fromId).setHeader("From").setAutoWidth(true);
        lanes.addColumn(NetworkGraphView.Lane::toId).setHeader("To").setAutoWidth(true);
        lanes.addColumn(NetworkGraphView.Lane::mode).setHeader("Mode").setAutoWidth(true);
        lanes.addColumn(l -> l.transitHours() + " h").setHeader("Nominal transit").setAutoWidth(true);
        lanes.addComponentColumn(l -> {
            Span b = new Span(l.disrupted() ? "DISRUPTED" : "OPERATIONAL");
            b.getElement().getThemeList().add(l.disrupted() ? "badge error small" : "badge success small");
            return b;
        }).setHeader("Status").setAutoWidth(true);
        lanes.setItems(view.lanes());
        lanes.setHeight("300px");
        add(lanes);
    }
}
