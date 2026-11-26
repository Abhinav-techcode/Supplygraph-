package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.ImpactAnalysisApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.impact.*;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cascading Impact Assessment View.
 * Performs deterministic topological graph traversal on digital twin snapshots,
 * computing direct/indirect node disruptions, BOM explosion material shortages, and financial exposure.
 */
@Route(value = "impact", layout = MainLayout.class)
@PageTitle("ORBIX :: Cascading Impact Assessment")
public class ImpactAssessmentView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final ImpactAnalysisApplicationService impactService;

    private final ComboBox<Disruption> disruptionSelector = new ComboBox<>();
    private final VerticalLayout resultsContainer = new VerticalLayout();

    public ImpactAssessmentView() {
        this(null, null);
    }

    @Autowired
    public ImpactAssessmentView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) ImpactAnalysisApplicationService impactService
    ) {
        this.disruptionService = disruptionService;
        this.impactService = impactService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildControlBar();
        add(resultsContainer);

        // Preload latest assessment if available
        loadInitialAssessment();
    }

    private void buildHeader() {
        H2 title = new H2("Cascading Impact Assessment & Shortage Analysis");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph desc = new Paragraph(
                "Evaluates upstream and downstream supply-chain blast radius using deterministic topological graph traversal "
                        + "and multi-tier BOM explosion against immutable digital twin snapshots."
        );
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(title, desc);
    }

    private void buildControlBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        disruptionSelector.setLabel("Select Disruption Incident");
        disruptionSelector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        disruptionSelector.setWidth("450px");

        if (disruptionService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<Disruption> disruptions = disruptionService.listDisruptions(wsId);
            disruptionSelector.setItems(disruptions);
            if (!disruptions.isEmpty()) {
                disruptionSelector.setValue(disruptions.getFirst());
            }
        }

        Button btnRun = new Button("Run Cascading Impact Analysis", VaadinIcon.SEARCH.create(), e -> runAssessment());
        btnRun.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(disruptionSelector, btnRun);
        add(bar);
    }

    private void runAssessment() {
        Disruption d = disruptionSelector.getValue();
        if (d == null) {
            Notification.show("Please select a disruption incident", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (impactService == null) {
            Notification.show("Impact service unavailable", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        try {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            ImpactAssessment assessment = impactService.analyzeCascadingImpact(d.id(), wsId);
            renderAssessment(assessment);
            Notification.show("Cascading impact analysis computed successfully!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Analysis error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadInitialAssessment() {
        if (impactService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<ImpactAssessment> assessments = impactService.listImpactAssessments(wsId);
            if (!assessments.isEmpty()) {
                renderAssessment(assessments.getFirst());
            }
        }
    }

    private void renderAssessment(ImpactAssessment a) {
        resultsContainer.removeAll();

        // 1. Summary Cards
        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.setSpacing(true);

        kpis.add(
                createCard("Overall Blast Severity", a.overallSeverity().name(), "var(--lumo-error-color)"),
                createCard("Estimated Financial Exposure (assumes $85/unit baseline price)", "$" + a.totalEstimatedCost().toPlainString(), "var(--lumo-primary-color)"),
                createCard("Impacted Graph Nodes", String.valueOf(a.nodeImpacts().size()), "var(--lumo-contrast-90pct)"),
                createCard("Material Shortages", String.valueOf(a.materialShortages().size()), "var(--lumo-warning-color)")
        );

        // 2. Tabbed Detail Grids
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        // Nodes Tab
        Grid<NodeImpact> nodeGrid = new Grid<>(NodeImpact.class, false);
        nodeGrid.addColumn(n -> n.nodeId().value()).setHeader("Node ID").setAutoWidth(true);
        nodeGrid.addColumn(NodeImpact::nodeName).setHeader("Facility Name").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.nodeType().name()).setHeader("Type").setAutoWidth(true);
        nodeGrid.addComponentColumn(n -> {
            Span badge = new Span(n.impactType().name());
            String theme = n.impactType() == ImpactType.DIRECT ? "badge error" : "badge warning";
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Impact Layer").setAutoWidth(true);
        nodeGrid.addColumn(n -> n.capacityLossPercentage() + "%").setHeader("Capacity Loss").setAutoWidth(true);
        nodeGrid.addColumn(NodeImpact::impactSummary).setHeader("Impact Reason").setFlexGrow(2);
        nodeGrid.setItems(a.nodeImpacts());
        tabSheet.add("Impacted Facilities (" + a.nodeImpacts().size() + ")", nodeGrid);

        // Materials Tab
        Grid<MaterialShortageImpact> matGrid = new Grid<>(MaterialShortageImpact.class, false);
        matGrid.addColumn(m -> m.materialId().value()).setHeader("Material ID").setAutoWidth(true);
        matGrid.addColumn(MaterialShortageImpact::materialName).setHeader("Description").setAutoWidth(true);
        matGrid.addColumn(m -> m.requiredQuantity().toPlainString() + " " + m.unitOfMeasure()).setHeader("Demand").setAutoWidth(true);
        matGrid.addColumn(m -> m.availableQuantity().toPlainString() + " " + m.unitOfMeasure()).setHeader("Available Stock").setAutoWidth(true);
        matGrid.addComponentColumn(m -> {
            Span s = new Span(m.shortageQuantity().toPlainString() + " " + m.unitOfMeasure());
            s.getStyle().set("color", "var(--lumo-error-color)").set("font-weight", "bold");
            return s;
        }).setHeader("Net Shortage").setAutoWidth(true);
        matGrid.addColumn(m -> m.stockoutEstimatedTime() != null ? m.stockoutEstimatedTime().toString() : "Immediate").setHeader("Stockout Date").setAutoWidth(true);
        matGrid.setItems(a.materialShortages());
        tabSheet.add("Material Shortages (" + a.materialShortages().size() + ")", matGrid);

        // Capacity Losses Tab
        Grid<CapacityImpact> capGrid = new Grid<>(CapacityImpact.class, false);
        capGrid.addColumn(CapacityImpact::resourceOrFacilityId).setHeader("Resource / Facility").setAutoWidth(true);
        capGrid.addColumn(CapacityImpact::facilityName).setHeader("Facility Name").setAutoWidth(true);
        capGrid.addColumn(c -> c.reductionPercentage().toPlainString() + "%").setHeader("Capacity Reduction").setAutoWidth(true);
        capGrid.addColumn(CapacityImpact::timeWindowDescription).setHeader("Window").setAutoWidth(true);
        capGrid.setItems(a.capacityImpacts());
        tabSheet.add("Capacity Losses (" + a.capacityImpacts().size() + ")", capGrid);

        // Logistics Tab
        Grid<LogisticsImpact> logGrid = new Grid<>(LogisticsImpact.class, false);
        logGrid.addColumn(l -> l.laneId().value()).setHeader("Lane ID").setAutoWidth(true);
        logGrid.addColumn(l -> l.transportMode().name()).setHeader("Transport Mode").setAutoWidth(true);
        logGrid.addColumn(l -> (l.delaySeconds() / 3600L) + " hours").setHeader("Transit Delay").setAutoWidth(true);
        logGrid.addColumn(l -> "$" + l.costIncrease().toPlainString()).setHeader("Cost Increase").setAutoWidth(true);
        logGrid.setItems(a.logisticsImpacts());
        tabSheet.add("Logistics Bottlenecks (" + a.logisticsImpacts().size() + ")", logGrid);

        // Risk & Assumptions
        VerticalLayout notesLayout = new VerticalLayout();
        notesLayout.setPadding(true);
        H3 riskTitle = new H3("Risk Indicators & Governance Assumptions");
        riskTitle.getStyle().set("margin-top", "0");
        notesLayout.add(riskTitle);
        for (String risk : a.riskIndicators()) {
            Span s = new Span("⚠️ " + risk);
            s.getStyle().set("color", "var(--lumo-error-text-color)");
            notesLayout.add(s);
        }
        for (String assumption : a.assumptions()) {
            notesLayout.add(new Span("ℹ️ " + assumption));
        }
        tabSheet.add("Risk & Assumptions", notesLayout);

        resultsContainer.add(kpis, tabSheet);
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

