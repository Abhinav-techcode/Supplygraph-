package com.supplygraph.web.views.sustainability;

import com.supplygraph.application.sustainability.EnvironmentalScenarioService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.sustainability.EnvironmentalScenario;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "sustainability/scenarios", layout = MainLayout.class)
@PageTitle("Environmental Scenario Comparison | SupplyGraph")
public class EnvironmentalScenarioComparisonView extends SustainabilityViewBase {

    private final EnvironmentalScenarioService scenarioService;

    public EnvironmentalScenarioComparisonView(EnvironmentalScenarioService scenarioService) {
        super("Environmental Scenario & Decarbonization Planner", "Simulate supply chain decarbonization roadmaps, modal shifts, and marginal abatement cost curves (MACC).");
        this.scenarioService = scenarioService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        List<EnvironmentalScenario> scenarios = scenarioService.listScenarios(ws);
        double maxReduction = scenarios.stream().mapToDouble(EnvironmentalScenario::reductionPercentage).max().orElse(0.0);
        double avgAbatementCost = scenarios.isEmpty() ? 0.0 : scenarios.stream().mapToDouble(s -> s.costPerTonCo2AvoidedUsd().doubleValue()).average().orElse(0.0);

        kpiRow.add(createKpiCard("Decarbonization Scenarios", String.valueOf(scenarios.size()), "Simulated strategic pathways", "primary"));
        kpiRow.add(createKpiCard("Max Simulated Reduction", String.format("%.1f%%", maxReduction), "Deep decarbonization potential", "success"));
        kpiRow.add(createKpiCard("Avg Marginal Abatement", String.format("$%.2f / t", avgAbatementCost), "Cost per ton CO2e avoided", "warning"));
        kpiRow.add(createKpiCard("Carbon Shadow Price", "$85.00 / t", "Reference default used for comparisons (assumption, not measured)", "contrast"));

        add(kpiRow);

        add(new H3("Simulate Decarbonization Pathway"));
        HorizontalLayout simForm = new HorizontalLayout();
        simForm.setAlignItems(Alignment.END);

        TextField nameField = new TextField("Scenario Name", "Rail Modal Shift + 100% Green Tariff");
        NumberField baseCo2Field = new NumberField("Baseline CO2 (tons)");
        baseCo2Field.setValue(1250.0);
        NumberField projCo2Field = new NumberField("Projected CO2 (tons)");
        projCo2Field.setValue(680.0);
        BigDecimalField costField = new BigDecimalField("Total Capex/Opex Cost ($)");
        costField.setValue(BigDecimal.valueOf(45000.0));

        Grid<EnvironmentalScenario> grid = new Grid<>();

        Button runSimBtn = new Button("Run Scenario Simulation", e -> {
            if (nameField.getValue() == null || baseCo2Field.getValue() == null || projCo2Field.getValue() == null || costField.getValue() == null) {
                Notification.show("Please complete simulation inputs", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            EnvironmentalScenario s = scenarioService.simulateScenario(
                    ws,
                    nameField.getValue(),
                    baseCo2Field.getValue(),
                    projCo2Field.getValue(),
                    costField.getValue()
            );
            Notification.show("Simulated scenario '" + s.scenarioName() + "': " + String.format("%.1f%%", s.reductionPercentage()) + " reduction at $" + s.costPerTonCo2AvoidedUsd() + "/t", 4000, Notification.Position.TOP_CENTER);
            grid.setItems(scenarioService.listScenarios(ws));
        });
        runSimBtn.getElement().getThemeList().add("primary");

        simForm.add(nameField, baseCo2Field, projCo2Field, costField, runSimBtn);
        add(simForm);

        add(new H3("Decarbonization Scenario Comparison"));
        grid.addColumn(s -> s.id().value()).setHeader("Scenario ID").setAutoWidth(true);
        grid.addColumn(EnvironmentalScenario::scenarioName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f t", s.baselineCo2Tons())).setHeader("Baseline CO2").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f t", s.projectedCo2Tons())).setHeader("Projected CO2").setAutoWidth(true);
        grid.addColumn(s -> String.format("%.1f%%", s.reductionPercentage())).setHeader("CO2 Reduction").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.totalAbatementCostUsd()).setHeader("Abatement Cost").setAutoWidth(true);
        grid.addColumn(s -> "$" + s.costPerTonCo2AvoidedUsd() + " / t").setHeader("Cost / Ton Avoided").setAutoWidth(true);
        grid.addColumn(s -> s.evaluatedAt().toString()).setHeader("Simulation Time").setAutoWidth(true);

        grid.setItems(scenarios);
        grid.setHeight("350px");

        add(grid);
    }
}
