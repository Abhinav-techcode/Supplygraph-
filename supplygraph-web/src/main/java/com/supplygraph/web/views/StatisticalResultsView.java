package com.supplygraph.web.views;

import com.supplygraph.application.benchmark.ComprehensiveBenchmarkSuiteService;
import com.supplygraph.domain.model.benchmark.BenchmarkBaseline;
import com.supplygraph.domain.model.benchmark.statistical.StatisticalSignificanceResult;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "statistical-results", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Statistical Results & Hypothesis Testing")
public class StatisticalResultsView extends VerticalLayout {

    private final ComprehensiveBenchmarkSuiteService benchmarkSuiteService;
    private final ComboBox<BenchmarkBaseline> baselineASelector = new ComboBox<>("Comparison Baseline (Control)");
    private final ComboBox<BenchmarkBaseline> baselineBSelector = new ComboBox<>("Evaluated Baseline (Treatment)");
    private final Grid<StatisticalSignificanceResult> significanceGrid = new Grid<>();
    private final VerticalLayout methodologyContainer = new VerticalLayout();

    public StatisticalResultsView() {
        this(null);
    }

    @Autowired
    public StatisticalResultsView(ComprehensiveBenchmarkSuiteService benchmarkSuiteService) {
        this.benchmarkSuiteService = benchmarkSuiteService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Statistical Significance & Hypothesis Testing");
        Paragraph subtitle = new Paragraph("Formal paired hypothesis testing (two-tailed Student's t-test, 95% CI, p < 0.05) validating empirical improvements of ConcurGuard Selective Replanning over established coordination baselines.");

        HorizontalLayout selectors = new HorizontalLayout();
        baselineASelector.setItems(BenchmarkBaseline.values());
        baselineASelector.setItemLabelGenerator(BenchmarkBaseline::getDisplayName);
        baselineASelector.setValue(BenchmarkBaseline.CONFLICT_DETECTION_WITHOUT_SELECTIVE_REPLANNING);
        baselineASelector.setWidth("380px");

        baselineBSelector.setItems(BenchmarkBaseline.values());
        baselineBSelector.setItemLabelGenerator(BenchmarkBaseline::getDisplayName);
        baselineBSelector.setValue(BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING);
        baselineBSelector.setWidth("380px");

        baselineASelector.addValueChangeListener(e -> refreshTests());
        baselineBSelector.addValueChangeListener(e -> refreshTests());

        selectors.add(baselineASelector, baselineBSelector);
        selectors.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        configureGrid();
        configureMethodology();

        add(header, subtitle, selectors, methodologyContainer, new H3("Paired Hypothesis Testing Results"), significanceGrid);
        refreshTests();
    }

    private void configureGrid() {
        significanceGrid.setHeight("350px");
        significanceGrid.addColumn(StatisticalSignificanceResult::metricName).setHeader("Evaluated Metric").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("%.2f", r.meanA())).setHeader("Control Mean").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("%.2f", r.meanB())).setHeader("Treatment Mean").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("%+.2f", r.meanDifference())).setHeader("Mean Difference (Delta)").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("t = %.2f", r.tStatistic())).setHeader("t-Statistic").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("p = %.4f", r.pValue())).setHeader("p-Value").setAutoWidth(true);
        significanceGrid.addColumn(r -> String.format("[%.2f, %.2f]", r.confidenceInterval95Lower(), r.confidenceInterval95Upper())).setHeader("95% Conf. Interval").setAutoWidth(true);

        significanceGrid.addComponentColumn(r -> {
            Span badge = new Span(r.statisticallySignificant() ? "SIGNIFICANT (p < 0.05)" : "NOT SIGNIFICANT");
            badge.getElement().getThemeList().add(r.statisticallySignificant() ? "badge success" : "badge warning");
            return badge;
        }).setHeader("Null Hypothesis H0").setAutoWidth(true);
    }

    private void configureMethodology() {
        methodologyContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
        methodologyContainer.getStyle().set("border-radius", "8px");
        methodologyContainer.setPadding(true);

        H3 methTitle = new H3("Statistical Evaluation Protocol");
        Paragraph desc = new Paragraph("Experiments employ paired-difference sampling across identical synthetic disruptions under fixed pseudo-random seeds. A two-tailed paired t-test evaluates the null hypothesis H0 (no difference in mean outcome). P-values below alpha = 0.05 reject H0, providing statistical confidence that ConcurGuard's localized selective replanning outperforms global restart and locking baselines.");

        methodologyContainer.add(methTitle, desc);
    }

    private void refreshTests() {
        if (benchmarkSuiteService == null) return;
        BenchmarkBaseline baseA = baselineASelector.getValue();
        BenchmarkBaseline baseB = baselineBSelector.getValue();
        if (baseA == null || baseB == null) return;

        List<StatisticalSignificanceResult> tests = benchmarkSuiteService.computeSignificance(baseA, baseB);
        significanceGrid.setItems(tests);
    }
}
