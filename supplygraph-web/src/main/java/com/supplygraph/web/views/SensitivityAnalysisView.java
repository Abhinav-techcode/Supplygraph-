package com.supplygraph.web.views;

import com.supplygraph.application.sensitivity.SensitivityAnalysisService;
import com.supplygraph.domain.model.sensitivity.SensitivityAnalysisReport;
import com.supplygraph.domain.model.sensitivity.SensitivityTrialResult;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "sensitivity-analysis", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Sensitivity & Robustness Analysis")
public class SensitivityAnalysisView extends VerticalLayout {

    private final SensitivityAnalysisService sensitivityService;
    private final Grid<SensitivityTrialResult> trialsGrid = new Grid<>();
    private final VerticalLayout scorecardContainer = new VerticalLayout();

    public SensitivityAnalysisView() {
        this(null);
    }

    @Autowired
    public SensitivityAnalysisView(SensitivityAnalysisService sensitivityService) {
        this.sensitivityService = sensitivityService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Sensitivity & Robustness Stress Testing");
        Paragraph subtitle = new Paragraph("Systematically perturbs critical operational parameters (+/-10%, +/-25%, +/-50%) to quantify plan elasticity, tail risk, and composite robustness score under real-world volatility.");

        Button runBtn = new Button("Run Full Parameter Sensitivity Suite", VaadinIcon.DASHBOARD.create(), e -> runSuite());

        configureGrid();

        add(header, subtitle, runBtn, scorecardContainer, new H3("Perturbation Trials & Elasticity Index"), trialsGrid);
        runSuite();
    }

    private void configureGrid() {
        trialsGrid.setHeight("400px");
        trialsGrid.addColumn(t -> t.parameter().getDisplayName()).setHeader("Perturbed Parameter").setAutoWidth(true);
        trialsGrid.addColumn(t -> String.format("%+.0f%%", t.perturbationPercentage())).setHeader("Perturbation Delta").setAutoWidth(true);
        trialsGrid.addColumn(t -> "$" + t.resultantCostUsd()).setHeader("Resultant Cost").setAutoWidth(true);
        trialsGrid.addColumn(t -> String.format("%.1f days", t.resultantRecoveryDays())).setHeader("Recovery Time (TTR)").setAutoWidth(true);
        trialsGrid.addColumn(t -> String.format("%.1f%%", t.resultantServiceLevelPct())).setHeader("Service Level").setAutoWidth(true);

        trialsGrid.addComponentColumn(t -> {
            Span badge = new Span(String.format("Elasticity: %.2f", t.elasticityIndex()));
            if (t.elasticityIndex() < 0.5) {
                badge.getElement().getThemeList().add("badge success"); // Low sensitivity = Robust
            } else if (t.elasticityIndex() < 1.0) {
                badge.getElement().getThemeList().add("badge");
            } else {
                badge.getElement().getThemeList().add("badge error"); // High sensitivity = Fragile
            }
            return badge;
        }).setHeader("Sensitivity Classification").setAutoWidth(true);
    }

    private void runSuite() {
        if (sensitivityService == null) return;

        SensitivityAnalysisReport report = sensitivityService.runSensitivitySuite(
                "WS-AUTO-01",
                "ALT-AIR-EXPEDITED",
                BigDecimal.valueOf(65000),
                8.0,
                96.0
        );

        trialsGrid.setItems(report.trials());

        scorecardContainer.removeAll();
        scorecardContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
        scorecardContainer.getStyle().set("border-radius", "8px");
        scorecardContainer.setPadding(true);

        H3 title = new H3(String.format("Recovery Plan Robustness Score: %.0f%%", report.overallRobustnessScore() * 100));
        Paragraph summary = new Paragraph(String.format(
                "Plan evaluated across %d perturbation trials. Baseline: $%s cost, %.1f days TTR, %.1f%% service retention. Plan demonstrates resilient bounding against demand surges and lead time expansions.",
                report.trials().size(),
                report.baselineCostUsd(),
                report.baselineRecoveryDays(),
                report.baselineServiceLevelPct()
        ));

        scorecardContainer.add(title, summary);
    }
}

