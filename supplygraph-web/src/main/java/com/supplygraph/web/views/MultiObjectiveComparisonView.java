package com.supplygraph.web.views;

import com.supplygraph.application.pareto.MultiObjectiveParetoService;
import com.supplygraph.domain.model.pareto.ParetoFrontier;
import com.supplygraph.domain.model.pareto.ParetoSolution;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "pareto-comparison", layout = MainLayout.class)
@PageTitle("ORBIX :: Multi-Objective Pareto Frontier")
public class MultiObjectiveComparisonView extends VerticalLayout {

    private final MultiObjectiveParetoService paretoService;
    private final TextField disruptionIdField = new TextField("Disruption Reference");
    private final Grid<ParetoSolution> solutionsGrid = new Grid<>();
    private final VerticalLayout frontierSummary = new VerticalLayout();

    public MultiObjectiveComparisonView() {
        this(null);
    }

    @Autowired
    public MultiObjectiveComparisonView(MultiObjectiveParetoService paretoService) {
        this.paretoService = paretoService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Multi-Objective Decision Analysis & Pareto Optimization");
        Paragraph subtitle = new Paragraph("Evaluates candidate recovery alternatives across 5 conflicting dimensions: cost minimization, recovery time, unmet customer demand, service degradation, and supplier concentration risk.");

        HorizontalLayout toolbar = new HorizontalLayout();
        disruptionIdField.setValue("DISR-ROTTERDAM-01");
        disruptionIdField.setWidth("280px");

        Button recomputeBtn = new Button("Recompute Pareto Frontier", VaadinIcon.CALC_BOOK.create(), e -> evaluateFrontier(disruptionIdField.getValue()));
        toolbar.add(disruptionIdField, recomputeBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        configureGrid();

        add(header, subtitle, toolbar, frontierSummary, new H3("Evaluated Recovery Alternatives & Dominance Status"), solutionsGrid);
        evaluateFrontier(disruptionIdField.getValue());
    }

    private void configureGrid() {
        solutionsGrid.setHeight("350px");
        solutionsGrid.addColumn(ParetoSolution::alternativeName).setHeader("Recovery Alternative").setAutoWidth(true);
        solutionsGrid.addColumn(s -> "$" + s.costUsd()).setHeader("Cost (Minimization)").setAutoWidth(true);
        solutionsGrid.addColumn(s -> s.recoveryTimeHours() + " hrs").setHeader("Recovery Time (Min)").setAutoWidth(true);
        solutionsGrid.addColumn(s -> s.unmetDemandUnits() + " units").setHeader("Unmet Demand").setAutoWidth(true);
        solutionsGrid.addColumn(s -> s.serviceDegradationPct() + "%").setHeader("Service Degradation").setAutoWidth(true);
        solutionsGrid.addColumn(s -> String.format("%.2f", s.supplierConcentrationRisk())).setHeader("Concentration Risk").setAutoWidth(true);

        solutionsGrid.addComponentColumn(s -> {
            Span badge = new Span(s.isDominant() ? "PARETO-OPTIMAL (FRONTIER)" : "DOMINATED (SUB-OPTIMAL)");
            badge.getElement().getThemeList().add(s.isDominant() ? "badge success" : "badge error");
            return badge;
        }).setHeader("Pareto Status").setAutoWidth(true);

        solutionsGrid.addColumn(ParetoSolution::tradeOffExplanation).setHeader("Trade-Off Profile").setAutoWidth(true);
    }

    private void evaluateFrontier(String disruptionId) {
        if (paretoService == null) return;
        ParetoFrontier frontier = paretoService.findLatestByDisruptionId(disruptionId)
                .orElseGet(() -> paretoService.evaluateAlternatives(disruptionId, List.of()));

        solutionsGrid.setItems(frontier.allSolutions());

        frontierSummary.removeAll();
        frontierSummary.getStyle().set("background", "var(--lumo-contrast-5pct)");
        frontierSummary.getStyle().set("border-radius", "8px");
        frontierSummary.setPadding(true);

        H3 summaryTitle = new H3(String.format("Frontier Analysis: %d of %d Alternatives are Non-Dominated (Pareto-Optimal)",
                frontier.frontierSolutions().size(), frontier.allSolutions().size()));
        Paragraph desc = new Paragraph("Sub-optimal recovery strategies that are strictly dominated by other plans across all 5 dimensions have been mathematically pruned from recommendation consideration.");

        frontierSummary.add(summaryTitle, desc);
    }
}

