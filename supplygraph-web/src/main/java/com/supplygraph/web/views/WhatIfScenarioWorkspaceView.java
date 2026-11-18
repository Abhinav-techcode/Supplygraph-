package com.supplygraph.web.views;

import com.supplygraph.application.whatif.WhatIfAnalysisService;
import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.domain.model.whatif.WhatIfScenarioConfig;
import com.supplygraph.domain.model.whatif.WhatIfScenarioId;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "what-if-workspace", layout = MainLayout.class)
@PageTitle("SupplyGraph :: What-If Scenario Workspace")
public class WhatIfScenarioWorkspaceView extends VerticalLayout {

    private final WhatIfAnalysisService whatIfService;
    private final Grid<WhatIfComparisonResult> historyGrid = new Grid<>();
    private final VerticalLayout comparisonResultContainer = new VerticalLayout();

    public WhatIfScenarioWorkspaceView() {
        this(null);
    }

    @Autowired
    public WhatIfScenarioWorkspaceView(WhatIfAnalysisService whatIfService) {
        this.whatIfService = whatIfService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("What-If Scenario Workspace");
        Paragraph subtitle = new Paragraph("Isolated simulation sandbox evaluating counterfactual supply chain parameters, freight modes, and buffer policies without mutating production digital twin state.");

        // Configuration Form
        FormLayout form = new FormLayout();
        TextField nameField = new TextField("Scenario Name");
        nameField.setValue("Expedited Air Freight + Safety Buffer Multiplier");

        NumberField capacityField = new NumberField("Supplier Capacity Factor");
        capacityField.setValue(0.85);
        capacityField.setMin(0.1);
        capacityField.setMax(2.0);
        capacityField.setStep(0.05);

        NumberField stockMultiplierField = new NumberField("Safety Stock Buffer Multiplier");
        stockMultiplierField.setValue(1.50);
        stockMultiplierField.setMin(0.5);
        stockMultiplierField.setMax(3.0);
        stockMultiplierField.setStep(0.1);

        ComboBox<String> freightModeField = new ComboBox<>("Freight Mode Override");
        freightModeField.setItems("STANDARD", "AIR", "SEA", "RAIL");
        freightModeField.setValue("AIR");

        NumberField leadTimeDeltaField = new NumberField("Lead Time Delta (Days)");
        leadTimeDeltaField.setValue(-3.0);

        NumberField durationField = new NumberField("Disruption Duration (Days)");
        durationField.setValue(14.0);

        form.add(nameField, capacityField, stockMultiplierField, freightModeField, leadTimeDeltaField, durationField);

        Button runBtn = new Button("Execute Isolated What-If Evaluation", VaadinIcon.PLAY.create());
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runBtn.addClickListener(e -> {
            if (whatIfService != null) {
                WhatIfScenarioConfig config = WhatIfScenarioConfig.of(
                        WhatIfScenarioId.generate(),
                        nameField.getValue(),
                        "User-configured counterfactual scenario",
                        "WS-AUTO-01",
                        capacityField.getValue(),
                        stockMultiplierField.getValue(),
                        leadTimeDeltaField.getValue().intValue(),
                        freightModeField.getValue(),
                        durationField.getValue().intValue(),
                        0.5,
                        0.3,
                        0.2
                );
                WhatIfComparisonResult result = whatIfService.executeScenario(config);
                Notification.show("What-If Scenario Evaluated!", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                displayComparisonResult(result);
                refreshHistory();
            }
        });

        configureHistoryGrid();

        add(header, subtitle, form, runBtn, comparisonResultContainer, new H3("Evaluation History"), historyGrid);
        refreshHistory();
    }

    private void configureHistoryGrid() {
        historyGrid.setHeight("250px");
        historyGrid.addColumn(WhatIfComparisonResult::scenarioName).setHeader("Scenario").setAutoWidth(true);
        historyGrid.addColumn(r -> "$" + r.scenarioCostUsd()).setHeader("Scenario Cost").setAutoWidth(true);
        historyGrid.addColumn(r -> r.scenarioTimeToRecoverDays() + " days").setHeader("Recovery Time (TTR)").setAutoWidth(true);
        historyGrid.addColumn(r -> r.scenarioServiceLevelPct() + "%").setHeader("Service Level").setAutoWidth(true);
        historyGrid.addColumn(r -> String.format("%+.1f%%", r.costVariancePct())).setHeader("Cost Delta %").setAutoWidth(true);
        historyGrid.addColumn(WhatIfComparisonResult::evaluatedAt).setHeader("Evaluated At").setAutoWidth(true);

        historyGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                displayComparisonResult(event.getValue());
            }
        });
    }

    private void displayComparisonResult(WhatIfComparisonResult res) {
        comparisonResultContainer.removeAll();
        comparisonResultContainer.getStyle().set("border", "1px solid var(--lumo-primary-50pct)");
        comparisonResultContainer.getStyle().set("border-radius", "8px");
        comparisonResultContainer.getStyle().set("background-color", "var(--lumo-base-color)");
        comparisonResultContainer.setPadding(true);

        H3 title = new H3("Evaluation Result: " + res.scenarioName());
        Paragraph summary = new Paragraph(res.varianceSummary());

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();

        cards.add(
                createKpiCard("Cost Differential", "$" + res.scenarioCostUsd(), String.format("%+.1f%% vs Baseline", res.costVariancePct())),
                createKpiCard("Time-to-Recover", res.scenarioTimeToRecoverDays() + " days", String.format("%+.1f days delta", res.recoveryTimeDeltaDays())),
                createKpiCard("Service Level", res.scenarioServiceLevelPct() + "%", String.format("%.1f%% vs %.1f%%", res.scenarioServiceLevelPct(), res.baselineServiceLevelPct())),
                createKpiCard("Resilience Score", res.scenarioResilienceIndex() + " / 100", String.format("%.1f vs %.1f baseline", res.scenarioResilienceIndex(), res.baselineResilienceIndex()))
        );

        comparisonResultContainer.add(title, summary, cards);
    }

    private VerticalLayout createKpiCard(String label, String value, String subtext) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("background", "var(--lumo-contrast-5pct)");
        card.getStyle().set("border-radius", "6px");
        card.setPadding(true);
        card.setSpacing(false);

        Span l = new Span(label);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)");
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");

        H3 v = new H3(value);
        v.getStyle().set("margin", "4px 0");

        Span s = new Span(subtext);
        s.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        s.getStyle().set("color", "var(--lumo-primary-text-color)");

        card.add(l, v, s);
        return card;
    }

    private void refreshHistory() {
        if (whatIfService != null) {
            List<WhatIfComparisonResult> list = whatIfService.findAll();
            historyGrid.setItems(list);
            if (!list.isEmpty()) {
                displayComparisonResult(list.get(0));
            }
        }
    }
}

