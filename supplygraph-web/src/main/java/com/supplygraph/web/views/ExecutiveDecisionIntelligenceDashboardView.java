package com.supplygraph.web.views;

import com.supplygraph.application.explainability.ExplainableRecommendationService;
import com.supplygraph.application.pareto.MultiObjectiveParetoService;
import com.supplygraph.application.resilience.AdvancedResilienceAnalyticsService;
import com.supplygraph.application.sensitivity.SensitivityAnalysisService;
import com.supplygraph.domain.model.explainability.ExplainableRecommendation;
import com.supplygraph.domain.model.pareto.ParetoFrontier;
import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;
import com.supplygraph.domain.model.sensitivity.SensitivityAnalysisReport;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "decision-intelligence", layout = MainLayout.class)
@PageTitle("ORBIX :: Executive Decision Intelligence")
public class ExecutiveDecisionIntelligenceDashboardView extends VerticalLayout {

    private final ExplainableRecommendationService recommendationService;
    private final MultiObjectiveParetoService paretoService;
    private final SensitivityAnalysisService sensitivityService;
    private final AdvancedResilienceAnalyticsService resilienceService;

    private final HorizontalLayout topScorecardLayout = new HorizontalLayout();
    private final VerticalLayout recommendationCard = new VerticalLayout();
    private final HorizontalLayout analyticsRow = new HorizontalLayout();

    private final com.supplygraph.application.service.DisruptionApplicationService disruptionService;

    @Autowired
    public ExecutiveDecisionIntelligenceDashboardView(
            ExplainableRecommendationService recommendationService,
            MultiObjectiveParetoService paretoService,
            SensitivityAnalysisService sensitivityService,
            AdvancedResilienceAnalyticsService resilienceService,
            com.supplygraph.application.service.DisruptionApplicationService disruptionService
    ) {
        this.disruptionService = disruptionService;
        this.recommendationService = recommendationService;
        this.paretoService = paretoService;
        this.sensitivityService = sensitivityService;
        this.resilienceService = resilienceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Executive Decision Intelligence & Recovery Control");
        Paragraph subtitle = new Paragraph("Unified executive dashboard orchestrating continuous disruption intelligence, transparent explainable recommendations, multi-objective Pareto trade-offs, and audited human approvals.");

        topScorecardLayout.setWidthFull();
        analyticsRow.setWidthFull();

        add(header, subtitle, topScorecardLayout, recommendationCard, analyticsRow);
        refreshDashboard();
    }

    private void refreshDashboard() {
        topScorecardLayout.removeAll();
        recommendationCard.removeAll();
        analyticsRow.removeAll();

        // 1. Top Scorecard
        var ws = com.supplygraph.domain.model.WorkspaceId.of(com.supplygraph.web.init.DemoDataInitializer.DEMO_WORKSPACE_ID);
        var active = disruptionService.listDisruptions(ws).stream().filter(d -> d.status() == com.supplygraph.domain.model.disruption.DisruptionStatus.ACTIVE).toList();
        var metrics = resilienceService.findLatestByWorkspaceId(ws.value());
        topScorecardLayout.add(
                createCard("Active Disruptions", String.valueOf(active.size()), active.isEmpty() ? "None active" : active.get(0).title() + (active.size() > 1 ? " (+" + (active.size() - 1) + " more)" : "")),
                createCard("Time to Survive (TTS)", metrics.map(m -> String.format("%.1f days", m.timeToSurviveDays())).orElse("No assessment yet"), "Buffer inventory before stockout (latest assessment)"),
                createCard("Time to Recover (TTR)", metrics.map(m -> String.format("%.1f days", m.timeToRecoverDays())).orElse("No assessment yet"), "Estimated restoration lead time (latest assessment)"),
                createCard("Composite Resilience", metrics.map(m -> String.format("%.1f / 100", m.compositeResilienceScore())).orElse("No assessment yet"), "From the latest advanced resilience assessment")
        );

        // 2. Recommendation Card
        recommendationCard.getStyle().set("border", "1px solid var(--lumo-primary-50pct)");
        recommendationCard.getStyle().set("border-radius", "8px");
        recommendationCard.getStyle().set("background-color", "var(--lumo-base-color)");
        recommendationCard.setPadding(true);

        ExplainableRecommendation rec = null;
        if (recommendationService != null) {
            List<ExplainableRecommendation> list = recommendationService.findAll();
            if (!list.isEmpty()) {
                rec = list.get(0);
            }
        }

        if (rec != null) {
            H3 recTitle = new H3("Prime Recovery Recommendation: " + rec.title());
            Paragraph summary = new Paragraph(rec.summary());

            HorizontalLayout metricsRow = new HorizontalLayout();
            metricsRow.add(
                    new Span("Expected Cost: $" + rec.expectedCostUsd()),
                    new Span("Recovery Time: " + rec.expectedRecoveryTimeHours() + " hrs"),
                    new Span("Service Restoration: " + rec.serviceLevelRestorationPct() + "%")
            );

            Span approvalBadge = new Span(rec.approved() ? "STATUS: APPROVED BY " + rec.approvedBy() : "STATUS: PENDING HUMAN AUTHORIZATION");
            approvalBadge.getElement().getThemeList().add(rec.approved() ? "badge success" : "badge warning");

            Button actionBtn = new Button("Review & Sign Recommendation in Decision Centre", VaadinIcon.EXTERNAL_LINK.create());
            actionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actionBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ExplainableRecommendationCentreView.class)));

            recommendationCard.add(recTitle, summary, metricsRow, approvalBadge, actionBtn);
        } else {
            recommendationCard.add(new H3("No Active Pending Recommendation"), new Paragraph("Digital twin network operating within nominal parameter boundaries."));
        }

        // 3. Analytics Row (Pareto + Sensitivity)
        VerticalLayout paretoPanel = new VerticalLayout();
        paretoPanel.setWidth("50%");
        paretoPanel.getStyle().set("background", "var(--lumo-contrast-5pct)");
        paretoPanel.getStyle().set("border-radius", "8px");
        paretoPanel.setPadding(true);

        H4 paretoTitle = new H4("Multi-Objective Pareto Analysis");
        Paragraph paretoDesc = new Paragraph("Compare recovery alternatives across cost, time, service level, risk and emissions; dominated alternatives are pruned.");
        Button viewParetoBtn = new Button("Inspect Pareto Trade-Offs", VaadinIcon.CHART_LINE.create(), e -> getUI().ifPresent(ui -> ui.navigate(MultiObjectiveComparisonView.class)));
        paretoPanel.add(paretoTitle, paretoDesc, viewParetoBtn);

        VerticalLayout sensPanel = new VerticalLayout();
        sensPanel.setWidth("50%");
        sensPanel.getStyle().set("background", "var(--lumo-contrast-5pct)");
        sensPanel.getStyle().set("border-radius", "8px");
        sensPanel.setPadding(true);

        H4 sensTitle = new H4("Sensitivity & Tail Robustness");
        Paragraph sensDesc = new Paragraph("Perturb the plan's parameters and see how robust the recommendation is. Run a sensitivity suite to compute scores.");
        Button viewSensBtn = new Button("View Sensitivity Breakdown", VaadinIcon.SLIDERS.create(), e -> getUI().ifPresent(ui -> ui.navigate(SensitivityAnalysisView.class)));
        sensPanel.add(sensTitle, sensDesc, viewSensBtn);

        analyticsRow.add(paretoPanel, sensPanel);
    }

    private VerticalLayout createCard(String title, String value, String sub) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("background", "var(--lumo-contrast-5pct)");
        card.getStyle().set("border-radius", "8px");
        card.setPadding(true);
        card.setSpacing(false);

        Span l = new Span(title);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)");
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");

        H3 v = new H3(value);
        v.getStyle().set("margin", "6px 0");

        Span s = new Span(sub);
        s.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        card.add(l, v, s);
        return card;
    }
}

