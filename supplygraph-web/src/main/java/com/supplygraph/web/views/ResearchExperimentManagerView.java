package com.supplygraph.web.views;

import com.supplygraph.application.benchmark.BenchmarkExperimentRunner;
import com.supplygraph.application.benchmark.ComprehensiveBenchmarkSuiteService;
import com.supplygraph.domain.model.benchmark.BenchmarkBaseline;
import com.supplygraph.domain.model.benchmark.BenchmarkExperimentRecord;
import com.supplygraph.domain.model.benchmark.BenchmarkScenarioType;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "experiment-manager", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Research Experiment Manager")
public class ResearchExperimentManagerView extends VerticalLayout {

    private final ComprehensiveBenchmarkSuiteService benchmarkSuiteService;
    private final BenchmarkExperimentRunner runner;
    private final Grid<BenchmarkExperimentRecord> recordsGrid = new Grid<>();

    public ResearchExperimentManagerView() {
        this(null, null);
    }

    @Autowired
    public ResearchExperimentManagerView(
            ComprehensiveBenchmarkSuiteService benchmarkSuiteService,
            BenchmarkExperimentRunner runner
    ) {
        this.benchmarkSuiteService = benchmarkSuiteService;
        this.runner = runner;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Research Experiment Manager & Trial Orchestrator");
        Paragraph subtitle = new Paragraph("Configure and execute empirical benchmark experiments comparing coordination baselines against 9 multi-agent contention scenarios under strict statistical reproducibility.");

        FormLayout form = new FormLayout();
        TextField nameField = new TextField("Experiment Suite Name");
        nameField.setValue("EMPIRICAL-EVAL-P8");

        ComboBox<BenchmarkScenarioType> scenarioSelect = new ComboBox<>("Scenario Filter");
        scenarioSelect.setItems(BenchmarkScenarioType.values());
        scenarioSelect.setItemLabelGenerator(BenchmarkScenarioType::getDisplayName);
        scenarioSelect.setValue(BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION);

        ComboBox<BenchmarkBaseline> baselineSelect = new ComboBox<>("Coordination Baseline");
        baselineSelect.setItems(BenchmarkBaseline.values());
        baselineSelect.setItemLabelGenerator(BenchmarkBaseline::getDisplayName);
        baselineSelect.setValue(BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING);

        IntegerField runsField = new IntegerField("Trial Runs per Experiment");
        runsField.setValue(10);
        runsField.setMin(1);
        runsField.setMax(100);

        IntegerField seedField = new IntegerField("Random Seed (Reproducibility)");
        seedField.setValue(42);

        form.add(nameField, scenarioSelect, baselineSelect, runsField, seedField);

        HorizontalLayout actionRow = new HorizontalLayout();
        Button runSingleBtn = new Button("Execute Single Trial", VaadinIcon.PLAY.create());
        runSingleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runSingleBtn.addClickListener(e -> {
            if (runner != null) {
                runner.runTrial(
                        nameField.getValue() + "-" + scenarioSelect.getValue().name(),
                        scenarioSelect.getValue(),
                        baselineSelect.getValue(),
                        seedField.getValue().longValue(),
                        runsField.getValue()
                );
                Notification.show("Experiment Trial Executed!", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshRecords();
            }
        });

        Button runFullSuiteBtn = new Button("Run Full 4x9 Research Matrix (36 Experiments)", VaadinIcon.ROCKET.create());
        runFullSuiteBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        runFullSuiteBtn.addClickListener(e -> {
            if (benchmarkSuiteService != null) {
                benchmarkSuiteService.runFullSuite(nameField.getValue(), seedField.getValue().longValue());
                Notification.show("Complete 36-Experiment Benchmark Suite Completed!", 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshRecords();
            }
        });

        actionRow.add(runSingleBtn, runFullSuiteBtn);

        configureGrid();

        add(header, subtitle, form, actionRow, new H3("Persisted Experiment Records & Metrics"), recordsGrid);
        refreshRecords();
    }

    private void configureGrid() {
        recordsGrid.setHeight("350px");
        recordsGrid.addColumn(r -> r.id().value()).setHeader("ID").setAutoWidth(true);
        recordsGrid.addColumn(BenchmarkExperimentRecord::name).setHeader("Suite / Name").setAutoWidth(true);
        recordsGrid.addColumn(r -> r.scenario().getDisplayName()).setHeader("Scenario").setAutoWidth(true);
        recordsGrid.addColumn(r -> r.baseline().getDisplayName()).setHeader("Baseline").setAutoWidth(true);
        recordsGrid.addColumn(r -> (int) (r.metrics().conflictPrecision() * 100) + "%").setHeader("Precision").setAutoWidth(true);
        recordsGrid.addColumn(r -> (int) (r.metrics().conflictRecall() * 100) + "%").setHeader("Recall").setAutoWidth(true);
        recordsGrid.addColumn(r -> r.metrics().validActionsPreserved() + " acts").setHeader("Preserved Actions").setAutoWidth(true);
        recordsGrid.addColumn(r -> r.metrics().replanningExecutionTimeMs() + " ms").setHeader("Latency").setAutoWidth(true);
        recordsGrid.addColumn(BenchmarkExperimentRecord::executedAt).setHeader("Executed At").setAutoWidth(true);
    }

    private void refreshRecords() {
        if (benchmarkSuiteService != null) {
            List<BenchmarkExperimentRecord> records = benchmarkSuiteService.findAll();
            recordsGrid.setItems(records);
        }
    }
}
