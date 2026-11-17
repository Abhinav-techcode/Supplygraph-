package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.MonteCarloApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.montecarlo.*;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Monte Carlo Risk & Uncertainty Analysis Workspace.
 * Computes empirical probability distributions, tail risk percentiles (P90, P95),
 * and SLA violation probabilities across thousands of pseudo-random replications.
 */
@Route(value = "monte-carlo", layout = MainLayout.class)
@PageTitle("SupplyGraph :: Monte Carlo Uncertainty Analysis")
public class MonteCarloWorkspaceView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final MonteCarloApplicationService monteCarloService;

    private final ComboBox<Disruption> disruptionSelector = new ComboBox<>();
    private final NumberField replicationsField = new NumberField("Replications");
    private final NumberField seedField = new NumberField("Random Seed");
    private final VerticalLayout resultsContainer = new VerticalLayout();

    public MonteCarloWorkspaceView() {
        this(null, null);
    }

    @Autowired
    public MonteCarloWorkspaceView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) MonteCarloApplicationService monteCarloService
    ) {
        this.disruptionService = disruptionService;
        this.monteCarloService = monteCarloService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildControlBar();
        add(resultsContainer);
    }

    private void buildHeader() {
        H2 title = new H2("Monte Carlo Risk & Stochastic Uncertainty Workspace");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph desc = new Paragraph(
                "Stochastic multi-replication simulator capturing supply-chain variance across lead times, yield rates, "
                        + "and demand spikes. Generates empirical confidence intervals (P90, P95) and SLA compliance probability."
        );
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(title, desc);
    }

    private void buildControlBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        disruptionSelector.setLabel("Disruption Incident");
        disruptionSelector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        disruptionSelector.setWidth("420px");

        if (disruptionService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<Disruption> disruptions = disruptionService.listDisruptions(wsId);
            disruptionSelector.setItems(disruptions);
            if (!disruptions.isEmpty()) {
                disruptionSelector.setValue(disruptions.getFirst());
            }
        }

        replicationsField.setValue(250.0);
        replicationsField.setMin(10.0);
        replicationsField.setMax(5000.0);
        replicationsField.setWidth("160px");

        seedField.setValue(424242.0);
        seedField.setWidth("160px");

        Button btnRun = new Button("Run Monte Carlo Analysis", VaadinIcon.CHART_LINE.create(), e -> executeMonteCarlo());
        btnRun.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(disruptionSelector, replicationsField, seedField, btnRun);
        add(bar);
    }

    private void executeMonteCarlo() {
        Disruption d = disruptionSelector.getValue();
        if (d == null) {
            Notification.show("Please select a disruption incident", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (monteCarloService == null) {
            Notification.show("Monte Carlo service unavailable", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        try {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            int reps = replicationsField.getValue() != null ? replicationsField.getValue().intValue() : 250;
            long seed = seedField.getValue() != null ? seedField.getValue().longValue() : 424242L;

            List<MonteCarloParameter> parameters = List.of(
                    new MonteCarloParameter("leadTimeDelayDays", "GLOBAL", new TriangularDistribution(3.0, 7.0, 18.0)),
                    new MonteCarloParameter("demandSurgeFactor", "GLOBAL", new NormalDistribution(1.0, 0.15, 0.5, 2.0)),
                    new MonteCarloParameter("transitDelayHours", "GLOBAL", new UniformDistribution(12.0, 72.0))
            );

            java.time.Instant start = java.time.Instant.now();
            com.supplygraph.simulation.engine.SimulationConfiguration simConfig =
                    com.supplygraph.simulation.engine.SimulationConfiguration.defaultHorizon(start, 86400L * 28);
            MonteCarloConfiguration config = new MonteCarloConfiguration(seed, reps, 10000, parameters, simConfig);
            MonteCarloSummaryResult result = monteCarloService.runAnalysis(
                    wsId, d.id(), null, config
            );

            renderSummary(result);
            Notification.show("Monte Carlo uncertainty analysis completed!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Monte Carlo error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void renderSummary(MonteCarloSummaryResult s) {
        resultsContainer.removeAll();

        // 1. KPI Cards Row
        HorizontalLayout kpiRow1 = new HorizontalLayout();
        kpiRow1.setWidthFull();
        kpiRow1.setSpacing(true);

        String slaStr = String.format("%.1f%%", (1.0 - s.slaViolationProbability()) * 100.0);
        String meanDelayStr = String.format("%.1f Days", s.meanDelayDays());
        String p90DelayStr = String.format("%.1f Days", s.p90DelayDays());
        String p95DelayStr = String.format("%.1f Days", s.p95DelayDays());

        kpiRow1.add(
                createCard("SLA Compliance Probability", slaStr, "var(--lumo-success-color)"),
                createCard("Mean Transit Delay", meanDelayStr, "var(--lumo-contrast-90pct)"),
                createCard("P90 Worst-Case Delay", p90DelayStr, "var(--lumo-warning-color)"),
                createCard("P95 Extreme Delay", p95DelayStr, "var(--lumo-error-color)")
        );

        HorizontalLayout kpiRow2 = new HorizontalLayout();
        kpiRow2.setWidthFull();
        kpiRow2.setSpacing(true);

        kpiRow2.add(
                createCard("Mean Shortage Units", s.meanShortageQuantity().toPlainString() + " units", "var(--lumo-contrast-90pct)"),
                createCard("Max Tail Risk Shortage", s.maxShortageQuantity().toPlainString() + " units", "var(--lumo-error-color)"),
                createCard("SLA Violation Probability", String.format("%.1f%%", s.slaViolationProbability() * 100.0), "var(--lumo-warning-color)"),
                createCard("Financial Exposure", "$" + s.estimatedMeanCost().toPlainString(), "var(--lumo-primary-color)")
        );

        // 2. Statistical Diagnostics Section
        VerticalLayout repSection = new VerticalLayout();
        repSection.setPadding(false);
        H2 repTitle = new H2("Stochastic Simulation Analysis (" + s.replicationCount() + " Replications, Seed: " + s.seed() + ")");
        repTitle.getStyle().set("margin-top", "var(--lumo-space-l)");

        Paragraph statsDesc = new Paragraph(
                "• Median Delay: " + String.format("%.2f Days", s.medianDelayDays()) + "\n"
                        + "• Recovery Within Deadline Probability: " + String.format("%.1f%%", s.recoveryWithinDeadlineProbability() * 100.0) + "\n"
                        + "• Execution Status: " + s.status() + "\n"
                        + "• Model Convergence: Evaluated using pseudorandom variates across Triangular, Normal, and Uniform parameter distributions."
        );
        statsDesc.getStyle().set("white-space", "pre-line").set("color", "var(--lumo-secondary-text-color)");

        repSection.add(repTitle, statsDesc);
        resultsContainer.add(kpiRow1, kpiRow2, repSection);
    }

    private VerticalLayout createCard(String label, String value, String color) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.04)");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "600");

        Span val = new Span(value);
        val.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", color);

        card.add(lbl, val);
        return card;
    }
}

