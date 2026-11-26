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
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.vaadin.flow.component.combobox.ComboBox;
import com.supplygraph.web.init.DemoDataInitializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "sensitivity-analysis", layout = MainLayout.class)
@PageTitle("ORBIX :: Sensitivity & Robustness Analysis")
public class SensitivityAnalysisView extends VerticalLayout {

    private final SensitivityAnalysisService sensitivityService;
    private final RecoveryPlanningApplicationService plans;
    private final Grid<SensitivityTrialResult> trialsGrid = new Grid<>();
    private final VerticalLayout scorecardContainer = new VerticalLayout();
    private final ComboBox<RecoveryPlan> planPicker = new ComboBox<>("Recovery plan to stress-test");

    @Autowired
    public SensitivityAnalysisView(SensitivityAnalysisService sensitivityService, RecoveryPlanningApplicationService plans) {
        this.sensitivityService = sensitivityService;
        this.plans = plans;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Sensitivity & Robustness Stress Testing");
        Paragraph subtitle = new Paragraph("Takes the real cost, recovery time and service level of one of your recovery plans and re-computes them when lead time, disruption length, demand, "
                + "stock and yield move by -25% to +50%. The size of each parameter's effect is a fixed modelling assumption of the application, not something measured from your data.");
        List<RecoveryPlan> all = plans.listAllPlans(WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID));
        planPicker.setItems(all);
        planPicker.setItemLabelGenerator(RecoveryPlan::planName);
        planPicker.setWidth("460px");
        planPicker.addValueChangeListener(e -> runSuite());
        configureGrid();
        add(header, subtitle, planPicker, scorecardContainer, new H3("Perturbation Trials & Elasticity Index"), trialsGrid);
        if (all.isEmpty()) {
            scorecardContainer.add(new Paragraph("There is no recovery plan yet. Create or load an incident, generate its recovery options, then return here."));
        } else {
            planPicker.setValue(all.stream().filter(x -> x.metrics().totalCost().signum() > 0).findFirst().orElse(all.get(0)));
        }
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
        RecoveryPlan plan = planPicker.getValue();
        scorecardContainer.removeAll();
        if (plan == null) { trialsGrid.setItems(List.of()); return; }
        var m = plan.metrics();
        SensitivityAnalysisReport report;
        try {
            report = sensitivityService.runSensitivitySuite(plan.workspaceId().value(), plan.id().value(), m.totalCost(), m.recoveryDurationDays(), m.serviceLevelImpactPercentage());
        } catch (RuntimeException ex) {
            scorecardContainer.add(new Paragraph("This plan cannot be stress-tested: " + ex.getMessage()));
            trialsGrid.setItems(List.of());
            return;
        }
        trialsGrid.setItems(report.trials());
        scorecardContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
        scorecardContainer.getStyle().set("border-radius", "8px");
        scorecardContainer.setPadding(true);
        H3 title = new H3(String.format("Robustness score under the model's assumptions: %.0f%%", report.overallRobustnessScore() * 100));
        Paragraph summary = new Paragraph(String.format("%d perturbation trials. Baseline taken from the plan: $%s cost, %.1f days to recover, %.1f%% service level.",
                report.trials().size(), report.baselineCostUsd(), report.baselineRecoveryDays(), report.baselineServiceLevelPct()));
        scorecardContainer.add(title, summary);
    }
}
