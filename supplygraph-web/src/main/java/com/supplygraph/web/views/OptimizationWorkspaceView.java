package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.OptimizationApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.simulation.optimization.*;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * Google OR-Tools Mathematical Recovery Optimization Workspace.
 * Solves mixed-integer programming (MIP/Linear) allocation problems for alternative supplier selection,
 * production rebalancing, and transportation mode trade-offs.
 */
@Route(value = "optimization", layout = MainLayout.class)
@PageTitle("ORBIX :: Google OR-Tools Recovery Optimization")
public class OptimizationWorkspaceView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final OptimizationApplicationService optimizationService;

    private final ComboBox<Disruption> disruptionSelector = new ComboBox<>();
    private final NumberField requiredDemandField = new NumberField("Required Demand (Units)");
    private final ComboBox<OptimizationObjective> objectiveBox = new ComboBox<>("Optimization Objective");
    private final NumberField costWeightField = new NumberField("Cost Weight");
    private final NumberField timeWeightField = new NumberField("Time Weight");
    private final NumberField riskWeightField = new NumberField("Risk Weight");

    private final VerticalLayout resultsContainer = new VerticalLayout();

    public OptimizationWorkspaceView() {
        this(null, null);
    }

    @Autowired
    public OptimizationWorkspaceView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) OptimizationApplicationService optimizationService
    ) {
        this.disruptionService = disruptionService;
        this.optimizationService = optimizationService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildControlBar();
        add(resultsContainer);
    }

    private void buildHeader() {
        H2 title = new H2("Mathematical Recovery Optimization (Google OR-Tools)");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph desc = new Paragraph(
                "Solves linear and mixed-integer programming formulations (MPSolver GLOP/SCIP) to identify Pareto-optimal "
                        + "recovery paths across alternative suppliers, expedited logistics lanes, and production rebalancing schedules."
        );
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(title, desc);
    }

    private void buildControlBar() {
        HorizontalLayout bar1 = new HorizontalLayout();
        bar1.setWidthFull();
        bar1.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar1.setSpacing(true);

        disruptionSelector.setLabel("Disruption Incident");
        disruptionSelector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        disruptionSelector.setWidth("400px");

        if (disruptionService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<Disruption> disruptions = disruptionService.listDisruptions(wsId);
            disruptionSelector.setItems(disruptions);
            if (!disruptions.isEmpty()) {
                disruptionSelector.setValue(disruptions.getFirst());
            }
        }

        requiredDemandField.setValue(15000.0);
        requiredDemandField.setMin(100.0);
        requiredDemandField.setWidth("200px");

        objectiveBox.setItems(OptimizationObjective.values());
        objectiveBox.setValue(OptimizationObjective.WEIGHTED_MULTI_OBJECTIVE);
        objectiveBox.setWidth("280px");

        bar1.add(disruptionSelector, requiredDemandField, objectiveBox);

        HorizontalLayout bar2 = new HorizontalLayout();
        bar2.setWidthFull();
        bar2.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar2.setSpacing(true);

        costWeightField.setValue(0.5);
        costWeightField.setMin(0.0);
        costWeightField.setMax(1.0);
        costWeightField.setWidth("140px");

        timeWeightField.setValue(0.3);
        timeWeightField.setMin(0.0);
        timeWeightField.setMax(1.0);
        timeWeightField.setWidth("140px");

        riskWeightField.setValue(0.2);
        riskWeightField.setMin(0.0);
        riskWeightField.setMax(1.0);
        riskWeightField.setWidth("140px");

        Button btnSolve = new Button("Solve Recovery Problem", VaadinIcon.SLIDERS.create(), e -> executeOptimization());
        btnSolve.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        bar2.add(costWeightField, timeWeightField, riskWeightField, btnSolve);
        add(bar1, bar2);
    }

    private void executeOptimization() {
        Disruption d = disruptionSelector.getValue();
        if (d == null) {
            Notification.show("Please select a disruption incident", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (optimizationService == null) {
            Notification.show("Optimization service unavailable", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        try {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            BigDecimal demand = BigDecimal.valueOf(requiredDemandField.getValue() != null ? requiredDemandField.getValue() : 15000.0);
            OptimizationObjective obj = objectiveBox.getValue() != null ? objectiveBox.getValue() : OptimizationObjective.WEIGHTED_MULTI_OBJECTIVE;

            OptimizationWeights weights = new OptimizationWeights(
                    costWeightField.getValue() != null ? costWeightField.getValue() : 0.4,
                    timeWeightField.getValue() != null ? timeWeightField.getValue() : 0.3,
                    0.2,
                    riskWeightField.getValue() != null ? riskWeightField.getValue() : 0.1
            );

            // Candidate Alternatives
            List<AlternativeOption> candidates = List.of(
                    new AlternativeOption("SUP-KYOTO", "Kyoto Microelectronics Fast Sourcing",
                            BigDecimal.valueOf(10000), BigDecimal.valueOf(18.50), 86400L * 5, 0.12),
                    new AlternativeOption("SUP-HSINCHU", "Hsinchu Wafer Foundry Contract",
                            BigDecimal.valueOf(8000), BigDecimal.valueOf(22.00), 86400L * 3, 0.25),
                    new AlternativeOption("PLANT-STUTTGART", "Stuttgart Safety Buffer Release",
                            BigDecimal.valueOf(3500), BigDecimal.valueOf(14.00), 86400L * 1, 0.05)
            );

            OptimizationResult result = optimizationService.solveRecovery(
                    wsId, d.id(), demand, candidates, obj, weights, 15
            );
            renderResult(result);
            Notification.show("Optimal recovery allocation solved!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Optimization error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void renderResult(OptimizationResult r) {
        resultsContainer.removeAll();

        // 1. Solver Banner & KPI Row
        HorizontalLayout banner = new HorizontalLayout();
        banner.setWidthFull();
        banner.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        banner.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H3 statusHeader = new H3("Solver Status: " + r.solverStatus().name());
        statusHeader.getStyle().set("margin", "0");
        Span statusBadge = new Span(r.solverStatus() == SolverStatus.OPTIMAL ? "OPTIMAL SOLUTION FOUND" : "FEASIBLE SOLUTION");
        statusBadge.getElement().getThemeList().add(r.solverStatus() == SolverStatus.OPTIMAL ? "badge success" : "badge warning");

        banner.add(statusHeader, statusBadge);

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.setSpacing(true);

        String costStr = "$" + r.estimatedCost().toPlainString();
        String timeStr = String.format("%.1f Days", r.expectedDelayDays());
        String unfulfilledStr = r.unmetDemandQuantity().toPlainString() + " units";

        kpis.add(
                createCard("Total Recovery Cost", costStr, "var(--lumo-primary-color)"),
                createCard("Expected Delay", timeStr, "var(--lumo-contrast-90pct)"),
                createCard("Unmet Demand", unfulfilledStr, r.unmetDemandQuantity().compareTo(BigDecimal.ZERO) > 0 ? "var(--lumo-error-color)" : "var(--lumo-success-color)"),
                createCard("Solver Duration", r.solverRuntimeMillis() + " ms", "var(--lumo-contrast-90pct)")
        );

        // 2. Allocation Decisions Grid
        Grid<OptimizationDecision> decisionGrid = new Grid<>(OptimizationDecision.class, false);
        decisionGrid.addColumn(OptimizationDecision::decisionId).setHeader("Decision ID").setAutoWidth(true);
        decisionGrid.addColumn(OptimizationDecision::decisionType).setHeader("Decision Type").setAutoWidth(true);
        decisionGrid.addColumn(OptimizationDecision::description).setHeader("Allocation Target").setAutoWidth(true).setFlexGrow(2);
        decisionGrid.addColumn(d -> d.allocatedQuantity().toPlainString() + " units").setHeader("Allocated Quantity").setAutoWidth(true);
        decisionGrid.addColumn(d -> "$" + d.estimatedCost().toPlainString()).setHeader("Incurred Cost").setAutoWidth(true);
        decisionGrid.addColumn(d -> String.format("%.1f Days", d.delayReductionDays())).setHeader("Delay Reduction").setAutoWidth(true);

        decisionGrid.setItems(r.decisions());
        decisionGrid.setAllRowsVisible(true);

        resultsContainer.add(banner, kpis, decisionGrid);
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

