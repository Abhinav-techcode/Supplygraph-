package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.PlanningScenarioService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;

@Route(value = "planning/scenarios", layout = MainLayout.class)
@PageTitle("Planning Scenario Designer | ORBIX")
public class PlanningScenarioDesignerView extends PlanningViewBase {

    private final PlanningScenarioService scenarioService;

    public PlanningScenarioDesignerView(PlanningScenarioService scenarioService) {
        super("Planning Scenario Designer", "Synthesize and evaluate tactical planning alternatives against demand surges, lead time volatility, and single-source disruptions.");
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();

        TextField nameField = new TextField("Scenario Title");
        nameField.setValue("Q2 Surge Resilient Buffer Plan");
        nameField.setWidth("350px");

        ComboBox<PlanningScenarioType> typeSelect = new ComboBox<>("Scenario Archetype", PlanningScenarioType.values());
        typeSelect.setItemLabelGenerator(PlanningScenarioType::getDescription);
        typeSelect.setValue(PlanningScenarioType.DEMAND_SURGE);
        typeSelect.setWidth("350px");

        row1.add(nameField, typeSelect);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setWidthFull();

        NumberField costField = new NumberField("Projected Total Cost ($)");
        costField.setValue(1250000.0);

        NumberField fillRateField = new NumberField("Target Service Fill Rate (%)");
        fillRateField.setValue(98.5);

        NumberField leadTimeField = new NumberField("Average Lead Time (Days)");
        leadTimeField.setValue(3.4);

        NumberField co2Field = new NumberField("Projected Carbon Footprint (Tons CO2e)");
        co2Field.setValue(85.0);

        row2.add(costField, fillRateField, leadTimeField, co2Field);

        Button saveBtn = new Button("Synthesize & Evaluate Scenario", e -> {
            try {
                PlanningScenarioComparison scenario = scenarioService.createScenario(
                        ws,
                        nameField.getValue(),
                        typeSelect.getValue(),
                        BigDecimal.valueOf(costField.getValue()),
                        fillRateField.getValue(),
                        leadTimeField.getValue(),
                        co2Field.getValue(),
                        "lead-planner"
                );
                Notification.show("Scenario synthesized: " + scenario.scenarioId().value(), 3000, Notification.Position.TOP_END);
                getUI().ifPresent(ui -> ui.navigate("planning/scenarios/results"));
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.getElement().getThemeList().add("primary");

        add(row1, row2, saveBtn);
    }
}

