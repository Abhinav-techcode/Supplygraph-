package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ClosedLoopService;
import com.supplygraph.application.execution.ExecutionAdvisoryService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "plan-vs-actual", layout = MainLayout.class)
@PageTitle("Plan-versus-Actual Analytics | SupplyGraph")
public class PlanVersusActualAnalyticsView extends ExecutionViewBase {
    private final ClosedLoopService closedLoop;
    private final ExecutionAdvisoryService advisory;
    private final ComboBox<ExecutionWorkflow> selector = workflowSelector("Workflow");
    private final Grid<PlanOutcomeComparison.Row> grid = new Grid<>();
    private final Paragraph explanation = new Paragraph("Select a workflow.");
    private final ComboBox<OutcomeMetric> metric = new ComboBox<>("Metric", OutcomeMetric.values());
    private final NumberField value = new NumberField("Measured value");
    private final TextField source = new TextField("Source of measurement (e.g. ERP invoice number)");

    public PlanVersusActualAnalyticsView(ExecutionQueryService queries, ClosedLoopService closedLoop, ExecutionAdvisoryService advisory) {
        super(queries, "Plan-versus-Actual Analytics", "Approved expectation versus measured outcome. Missing measurements are UNKNOWN; measurements from SIMULATED workflows are SIMULATED and never trigger reassessment.");
        this.closedLoop = closedLoop;
        this.advisory = advisory;
        selector.addValueChangeListener(e -> refresh());
        grid.addColumn(r -> r.metric() + " (" + r.metric().unit() + ")").setHeader("Metric").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.2f", r.expected())).setHeader("Expected (plan)").setAutoWidth(true);
        grid.addColumn(r -> r.actual() == null ? "UNKNOWN" : String.format("%.2f", r.actual())).setHeader("Actual").setAutoWidth(true);
        grid.addColumn(r -> r.deltaPercent() == null ? "—" : String.format("%+.1f%%", r.deltaPercent())).setHeader("Deviation").setAutoWidth(true);
        grid.addColumn(r -> r.status().name()).setHeader("Basis").setAutoWidth(true);
        grid.addColumn(r -> r.actualSource() == null ? "" : r.actualSource()).setHeader("Source").setAutoWidth(true);
        grid.setHeight("14em");
        Button record = new Button("Record measurement", e -> {
            if (selector.getValue() == null || metric.isEmpty() || value.isEmpty() || source.isEmpty()) { UiSupport.error(new IllegalArgumentException("Workflow, metric, value and source are required")); return; }
            if (UiSupport.run("Measurement recorded", () -> closedLoop.recordActual(selector.getValue().id(), metric.getValue(), value.getValue(), source.getValue().trim(), false))) refresh();
        });
        Button evaluate = new Button("Evaluate deviation", e -> {
            if (selector.getValue() == null) { UiSupport.error(new IllegalArgumentException("Select a workflow")); return; }
            var r = UiSupport.call(() -> closedLoop.evaluateDeviation(selector.getValue().id()), java.util.Optional.<ReassessmentRequest>empty());
            UiSupport.run(r.isPresent() ? "Reassessment proposed — a human must decide it in the Closed-Loop dashboard" : "No reassessment proposed (below threshold, unknown/simulated, duplicate or depth limit)", () -> {});
            refresh();
        });
        body(selector, grid, explanation, new HorizontalLayout(metric, value, source, record, evaluate));
    }

    private void refresh() {
        if (selector.getValue() == null) return;
        String id = selector.getValue().id();
        PlanOutcomeComparison cmp = UiSupport.call(() -> closedLoop.comparison(id), new PlanOutcomeComparison(List.of()));
        grid.setItems(cmp.rows());
        var a = UiSupport.call(() -> advisory.explainDeviation(cmp), null);
        if (a != null) explanation.setText("[" + a.role().displayName() + " · advisory] " + a.text());
    }
}
