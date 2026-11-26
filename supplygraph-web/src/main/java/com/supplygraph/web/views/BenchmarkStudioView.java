package com.supplygraph.web.views;

import com.supplygraph.application.benchmark.BenchmarkExperimentRunner;
import com.supplygraph.application.port.out.BenchmarkExperimentRepositoryPort;
import com.supplygraph.domain.model.benchmark.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "benchmark-studio", layout = MainLayout.class)
@PageTitle("Research Benchmark Studio | ORBIX")
public class BenchmarkStudioView extends VerticalLayout {

    private final BenchmarkExperimentRunner runner;
    private final BenchmarkExperimentRepositoryPort experimentRepository;

    private final ComboBox<BenchmarkScenarioType> scenarioSelect;
    private final IntegerField trialRunsInput;
    private final Grid<BenchmarkExperimentRecord> comparisonGrid;
    private final Grid<BenchmarkExperimentRecord> historyGrid;

    public BenchmarkStudioView(
            BenchmarkExperimentRunner runner,
            BenchmarkExperimentRepositoryPort experimentRepository
    ) {
        this.runner = runner;
        this.experimentRepository = experimentRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Empirical Research Benchmark Studio");
        Paragraph desc = new Paragraph("Reproducible multi-baseline comparative evaluation across 9 contention scenarios demonstrating ConcurGuard coordination and selective replanning.");

        scenarioSelect = new ComboBox<>("Evaluation Scenario");
        scenarioSelect.setItems(BenchmarkScenarioType.values());
        scenarioSelect.setItemLabelGenerator(BenchmarkScenarioType::getDisplayName);
        scenarioSelect.setValue(BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION);
        scenarioSelect.setWidth("380px");

        trialRunsInput = new IntegerField("Trial Runs (N)");
        trialRunsInput.setValue(5);
        trialRunsInput.setMin(1);
        trialRunsInput.setMax(20);
        trialRunsInput.setWidth("120px");

        Button runComparisonBtn = new Button("Run 4-Baseline Comparative Study", ev -> runComparativeStudy());
        runComparisonBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout controlRow = new HorizontalLayout(scenarioSelect, trialRunsInput, runComparisonBtn);
        controlRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        H3 compHeading = new H3("Baseline Performance Comparison Matrix");
        comparisonGrid = new Grid<>();
        configureMetricsGrid(comparisonGrid);
        comparisonGrid.setAllRowsVisible(true);

        H3 historyHeading = new H3("Reproducible Experiment Trial History");
        historyGrid = new Grid<>();
        configureMetricsGrid(historyGrid);
        historyGrid.setSizeFull();

        add(title, desc, controlRow, compHeading, comparisonGrid, historyHeading, historyGrid);
        refreshHistory();
    }

    private void configureMetricsGrid(Grid<BenchmarkExperimentRecord> grid) {
        grid.addColumn(e -> e.id().value()).setHeader("Exp ID").setAutoWidth(true);
        grid.addColumn(e -> e.baseline().getDisplayName()).setHeader("Baseline Strategy").setWidth("240px");
        grid.addColumn(e -> e.scenario().getDisplayName()).setHeader("Scenario").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f%%", e.metrics().conflictPrecision() * 100.0)).setHeader("Precision").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f%%", e.metrics().conflictRecall() * 100.0)).setHeader("Recall").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f%%", e.metrics().falsePositiveRate() * 100.0)).setHeader("FPR").setAutoWidth(true);
        grid.addColumn(e -> e.metrics().invalidActionsPrevented()).setHeader("Invalid Prevented").setAutoWidth(true);
        grid.addColumn(e -> e.metrics().validActionsPreserved()).setHeader("Valid Preserved").setAutoWidth(true);
        grid.addColumn(e -> e.metrics().replanningScopeSize()).setHeader("Replanning Scope").setAutoWidth(true);
        grid.addColumn(e -> e.metrics().totalCoordinationOverheadMs() + " ms").setHeader("Coordination Overhead").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.1f%%", e.metrics().serviceLevelPercentage())).setHeader("Service Level").setAutoWidth(true);
        grid.addColumn(e -> String.format("%.0f%%", e.metrics().optimizationFeasibilityRate() * 100.0)).setHeader("Feasibility").setAutoWidth(true);
    }

    private void runComparativeStudy() {
        BenchmarkScenarioType scenario = scenarioSelect.getValue();
        int trials = trialRunsInput.getValue() != null ? trialRunsInput.getValue() : 1;

        List<BenchmarkExperimentRecord> comparativeResults = new ArrayList<>();

        for (BenchmarkBaseline baseline : BenchmarkBaseline.values()) {
            BenchmarkExperimentRecord record = runner.runTrial(
                    "Study: " + scenario.getDisplayName() + " [" + baseline.name() + "]",
                    scenario,
                    baseline,
                    42L,
                    trials
            );
            comparativeResults.add(record);
        }

        comparisonGrid.setItems(comparativeResults);
        refreshHistory();

        Notification.show(
                "Completed 4-baseline comparative study for " + scenario.getDisplayName() + " (" + trials + " trials each)",
                4000,
                Notification.Position.BOTTOM_END
        );
    }

    private void refreshHistory() {
        if (experimentRepository != null) {
            historyGrid.setItems(experimentRepository.findAll());
        }
    }
}

