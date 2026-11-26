package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.MultiEchelonInventoryService;
import com.supplygraph.domain.model.planning.SafetyStockResult;
import com.supplygraph.domain.model.planning.ServiceLevel;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "planning/inventory/safety-stock", layout = MainLayout.class)
@PageTitle("Safety Stock Planner | ORBIX")
public class SafetyStockPlannerView extends PlanningViewBase {

    private final MultiEchelonInventoryService inventoryService;
    private final VerticalLayout resultsLayout = new VerticalLayout();

    public SafetyStockPlannerView(MultiEchelonInventoryService inventoryService) {
        super("Stochastic Safety Stock & EOQ Planner", "Formulate safety stock buffers accounting for lead-time variance, demand volatility, and service level targets.");
        this.inventoryService = inventoryService;

        HorizontalLayout inputRow = new HorizontalLayout();
        inputRow.setWidthFull();
        inputRow.setAlignItems(Alignment.BASELINE);

        NumberField meanDemand = new NumberField("Mean Daily Demand (units)");
        meanDemand.setValue(60.0);

        NumberField stdDevDemand = new NumberField("Daily Demand Std Dev (units)");
        stdDevDemand.setValue(12.0);

        IntegerField leadTime = new IntegerField("Lead Time (days)");
        leadTime.setValue(10);
        leadTime.setMin(1);

        NumberField stdDevLeadTime = new NumberField("Lead Time Std Dev (days)");
        stdDevLeadTime.setValue(2.0);

        ComboBox<ServiceLevel> serviceLevel = new ComboBox<>("Target Cycle Service Level", ServiceLevel.values());
        serviceLevel.setItemLabelGenerator(ServiceLevel::getLabel);
        serviceLevel.setValue(ServiceLevel.SL_95);

        NumberField holdingCost = new NumberField("Holding Cost/Unit/Year ($)");
        holdingCost.setValue(8.50);

        NumberField orderingCost = new NumberField("Fixed Order Cost ($)");
        orderingCost.setValue(150.00);

        Button calculateBtn = new Button("Calculate Buffer", e -> {
            SafetyStockResult res = inventoryService.calculateSafetyStock(
                    meanDemand.getValue(),
                    stdDevDemand.getValue(),
                    leadTime.getValue(),
                    stdDevLeadTime.getValue(),
                    serviceLevel.getValue(),
                    holdingCost.getValue(),
                    orderingCost.getValue()
            );
            renderResults(res);
        });
        calculateBtn.getElement().getThemeList().add("primary");

        inputRow.add(meanDemand, stdDevDemand, leadTime, stdDevLeadTime, serviceLevel, holdingCost, orderingCost, calculateBtn);
        add(inputRow, resultsLayout);
    }

    private void renderResults(SafetyStockResult res) {
        resultsLayout.removeAll();
        resultsLayout.add(new H3("Stochastic Buffer Recommendations"));

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();

        cards.add(card("Recommended Safety Stock", String.format("%.0f units", res.recommendedSafetyStock()), "Silver-Pyke combined buffer", "primary"));
        cards.add(card("Reorder Point (ROP)", String.format("%.0f units", res.reorderPoint()), "Lead-time demand + safety stock", "success"));
        cards.add(card("Economic Order Qty (EOQ)", String.format("%.0f units", res.economicOrderQuantity()), "Wilson lot-size optimum", "contrast"));
        cards.add(card("Lead-Time Variance Component", String.format("%.0f units", res.leadTimeVariabilityBuffer()), "Supply-side uncertainty buffer", "warning"));

        resultsLayout.add(cards);
    }

    private VerticalLayout card(String title, String val, String sub, String theme) {
        VerticalLayout c = new VerticalLayout();
        c.addClassNames("border", "p-m", "rounded-m");
        c.add(new Span(title), new H3(val), new Span(sub));
        c.getElement().getThemeList().add("badge " + theme);
        return c;
    }
}

