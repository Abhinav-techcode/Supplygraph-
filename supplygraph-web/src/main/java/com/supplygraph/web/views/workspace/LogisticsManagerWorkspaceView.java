package com.supplygraph.web.views.workspace;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.planning.SupplyChainNetworkPlanningService;
import com.supplygraph.application.sustainability.CarbonAccountingService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.planning.TransportMode;
import com.supplygraph.domain.model.sustainability.EmissionScope;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;

@Route(value = "workspace/logistics", layout = MainLayout.class)
@PageTitle("Logistics Manager Workspace | ORBIX")
public class LogisticsManagerWorkspaceView extends WorkspaceViewBase {

    private final SupplyChainNetworkPlanningService networkService;
    private final CarbonAccountingService carbonService;
    private final OrderCollaborationService orderService;

    public LogisticsManagerWorkspaceView(
            SupplyChainNetworkPlanningService networkService,
            CarbonAccountingService carbonService,
            OrderCollaborationService orderService
    ) {
        super("Logistics & Transportation Cockpit", "Multi-modal transport trade-offs, advance shipping notice (ASN) telemetry, and upstream freight carbon intensity.", "Role :: Logistics & Freight Manager");
        this.networkService = networkService;
        this.carbonService = carbonService;
        this.orderService = orderService;

        WorkspaceId ws = currentWorkspaceId();
        List<AdvanceShippingNotice> asns = orderService.listAsns(ws);
        Map<EmissionScope, Double> emissions = carbonService.getEmissionsByScope(ws);
        double freightCo2 = emissions.getOrDefault(EmissionScope.SCOPE_3_UPSTREAM_FREIGHT, 0.0);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("In-Transit ASNs", String.valueOf(asns.size()), "Tracked carrier consignments", "primary"));
        kpiRow.add(createKpiCard("Freight Scope 3 GHG", String.format("%.2f t", freightCo2), "Ton-km carbon footprint", "warning"));
        kpiRow.add(createKpiCard("On-Time In-Full (OTIF)", "No data", "No carrier-performance source connected", "success"));
        kpiRow.add(createKpiCard("Transport Modes", "No data", "Derived from lanes when a network is loaded", "contrast"));

        add(kpiRow);

        add(new H3("Active Advance Shipping Notices (Inbound Deliveries)"));
        Grid<AdvanceShippingNotice> grid = new Grid<>();
        grid.addColumn(a -> a.id().value()).setHeader("ASN ID").setAutoWidth(true);
        grid.addColumn(a -> a.poId().value()).setHeader("Linked PO").setAutoWidth(true);
        grid.addColumn(AdvanceShippingNotice::carrierName).setHeader("3PL Carrier").setAutoWidth(true);
        grid.addColumn(AdvanceShippingNotice::trackingNumber).setHeader("Waybill / Tracking").setAutoWidth(true);
        grid.addColumn(a -> a.shippedQuantity() + " units").setHeader("Shipped Volume").setAutoWidth(true);
        grid.addColumn(a -> a.estimatedArrivalDate().toString()).setHeader("ETA").setAutoWidth(true);
        grid.addColumn(a -> a.dispatchedAt().toString()).setHeader("Dispatched").setAutoWidth(true);

        grid.setItems(asns);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("Modal Trade-Off Planner", VaadinIcon.TRAIN.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/network"))));
        actionRow.add(new Button("Freight Emissions Explorer", VaadinIcon.GLOBE.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/carbon/transport"))));
        actionRow.add(new Button("ASN Confirmation Hub", VaadinIcon.TRUCK.create(), e -> getUI().ifPresent(ui -> ui.navigate("portal/deliveries"))));

        add(grid, actionRow);
    }
}

