package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.SupplyChainNetworkPlanningService;
import com.supplygraph.domain.model.planning.TransportModeTradeoff;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning/network", layout = MainLayout.class)
@PageTitle("Multi-Echelon Network Planner | SupplyGraph")
public class MultiEchelonNetworkPlannerView extends PlanningViewBase {

    private final SupplyChainNetworkPlanningService networkService;
    private final Grid<TransportModeTradeoff> grid = new Grid<>();

    public MultiEchelonNetworkPlannerView(SupplyChainNetworkPlanningService networkService) {
        super("Multi-Echelon Network Planner", "Multi-commodity network flow optimization and freight transportation mode trade-off analysis.");
        this.networkService = networkService;

        HorizontalLayout inputRow = new HorizontalLayout();
        inputRow.setWidthFull();
        inputRow.setAlignItems(Alignment.BASELINE);

        TextField originField = new TextField("Origin Node");
        originField.setValue("PLANT-BERLIN");

        TextField destField = new TextField("Destination Hub");
        destField.setValue("HUB-LEIPZIG");

        NumberField distanceField = new NumberField("Lane Distance (km)");
        distanceField.setValue(185.0);

        NumberField weightField = new NumberField("Consignment Weight (tons)");
        weightField.setValue(24.0);

        Button evaluateBtn = new Button("Evaluate Modal Trade-offs", e -> {
            List<TransportModeTradeoff> tradeoffs = networkService.evaluateTransportTradeoffs(
                    distanceField.getValue(), weightField.getValue()
            );
            grid.setItems(tradeoffs);
        });
        evaluateBtn.getElement().getThemeList().add("primary");

        inputRow.add(originField, destField, distanceField, weightField, evaluateBtn);
        add(inputRow);

        add(new H3("Modal Trade-Off Evaluation (Cost vs Time vs Carbon)"));
        grid.addColumn(t -> t.mode().getDescription()).setHeader("Transport Mode").setAutoWidth(true);
        grid.addColumn(t -> "$" + t.freightCost()).setHeader("Freight Cost").setAutoWidth(true);
        grid.addColumn(t -> t.transitDays() + " days").setHeader("Transit Lead Time").setAutoWidth(true);
        grid.addColumn(t -> String.format("%.1f kg CO2", t.co2KgPerTon())).setHeader("Carbon Footprint (per ton)").setAutoWidth(true);
        grid.addColumn(t -> t.reliabilityPercent() + "%").setHeader("Historical Reliability").setAutoWidth(true);

        grid.setHeight("250px");
        grid.setItems(networkService.evaluateTransportTradeoffs(185.0, 24.0));
        add(grid);
    }
}

