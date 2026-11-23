package com.supplygraph.web.views;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.SimulationApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.engine.SimulationConfiguration;
import com.supplygraph.simulation.engine.SimulationResult;
import com.supplygraph.simulation.event.SimulationEvent;
import com.supplygraph.simulation.state.TrajectoryPoint;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Discrete-Event Simulation Workspace.
 * Dispatches chronological simulation events against isolated in-memory digital twin states,
 * generating time-series inventory, capacity, and service-level trajectories.
 */
@Route(value = "simulation/incident", layout = MainLayout.class)
@PageTitle("Simulation of a recorded incident | SupplyGraph")
public class SimulationIncidentView extends VerticalLayout {

    private final DisruptionApplicationService disruptionService;
    private final SimulationApplicationService simulationService;

    private final ComboBox<Disruption> disruptionSelector = new ComboBox<>();
    private final TextField scenarioField = new TextField("Scenario Identifier");
    private final NumberField horizonDaysField = new NumberField("Horizon (Days)");
    private final VerticalLayout resultsContainer = new VerticalLayout();

    public SimulationIncidentView() {
        this(null, null);
    }

    @Autowired
    public SimulationIncidentView(
            @Autowired(required = false) DisruptionApplicationService disruptionService,
            @Autowired(required = false) SimulationApplicationService simulationService
    ) {
        this.disruptionService = disruptionService;
        this.simulationService = simulationService;

        setSpacing(true);
        setPadding(true);
        setMaxWidth("1300px");
        getStyle().set("margin", "0 auto");

        buildHeader();
        buildControlBar();
        add(resultsContainer);
    }

    private void buildHeader() {
        H2 title = new H2("Simulate a recorded incident");
        title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Paragraph desc = new Paragraph(
                "Replays an incident that is already recorded in the system, day by day, on a copy of your supply chain, and shows the event timeline and stock trajectories. "
                        + "For a simpler \"what if this site stops?\" question with plain-language results, use the Business simulator."
        );
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(title, desc);
    }

    private void buildControlBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        disruptionSelector.setLabel("Disruption Scenario");
        disruptionSelector.setItemLabelGenerator(d -> d.title() + " [" + d.status() + "]");
        disruptionSelector.setWidth("380px");

        if (disruptionService != null) {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            List<Disruption> disruptions = disruptionService.listDisruptions(wsId);
            disruptionSelector.setItems(disruptions);
            if (!disruptions.isEmpty()) {
                disruptionSelector.setValue(disruptions.getFirst());
            }
        }

        scenarioField.setValue("SCENARIO-AUTO-MITIGATED");
        scenarioField.setWidth("240px");

        horizonDaysField.setValue(28.0);
        horizonDaysField.setMin(1.0);
        horizonDaysField.setMax(180.0);
        horizonDaysField.setWidth("140px");

        Button btnRun = new Button("Run Simulation", VaadinIcon.PLAY_CIRCLE.create(), e -> executeSimulation());
        btnRun.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(disruptionSelector, scenarioField, horizonDaysField, btnRun);
        add(bar);
    }

    private void executeSimulation() {
        Disruption d = disruptionSelector.getValue();
        if (d == null) {
            Notification.show("Please select a disruption incident", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (simulationService == null) {
            Notification.show("Simulation service unavailable", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        try {
            WorkspaceId wsId = WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
            Instant start = Instant.now();
            long days = horizonDaysField.getValue() != null ? horizonDaysField.getValue().longValue() : 28;
            Instant end = start.plus(Duration.ofDays(days));

            SimulationConfiguration config = new SimulationConfiguration(
                    start, end, 86400L, 50000, true
            );

            SimulationResult result = simulationService.runSimulation(
                    wsId, d.id(), null, config
            );

            renderResult(result);
            Notification.show("Discrete-event simulation completed!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Simulation error: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void renderResult(SimulationResult res) {
        resultsContainer.removeAll();

        // 1. KPI Cards
        HorizontalLayout kpis = new HorizontalLayout();
        kpis.setWidthFull();
        kpis.setSpacing(true);

        String serviceLevelStr = String.format("%.1f%%", res.serviceLevelPercentage());
        String shortageStr = res.totalShortageQuantity().toPlainString() + " units";
        long delayDays = res.maxDelaySeconds() / 86400L;

        kpis.add(
                createCard("Achieved Service Level", serviceLevelStr, "var(--lumo-success-color)"),
                createCard("Total Shortage Quantity", shortageStr, "var(--lumo-primary-color)"),
                createCard("Events Dispatched", String.valueOf(res.processedEventCount()), "var(--lumo-contrast-90pct)"),
                createCard("Max Delay", delayDays + " Days", "var(--lumo-warning-color)")
        );

        // 2. TabSheet with Events & Trajectories
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        // Event Log Grid
        Grid<SimulationEvent> eventGrid = new Grid<>(SimulationEvent.class, false);
        eventGrid.addColumn(SimulationEvent::eventId).setHeader("Event ID").setAutoWidth(true);
        eventGrid.addColumn(e -> e.timestamp().toString()).setHeader("Simulated Time").setAutoWidth(true);
        eventGrid.addComponentColumn(e -> {
            Span badge = new Span(e.eventType().name());
            badge.getElement().getThemeList().add("badge");
            return badge;
        }).setHeader("Event Type").setAutoWidth(true);
        eventGrid.addColumn(SimulationEvent::entityId).setHeader("Target Entity").setAutoWidth(true);
        eventGrid.addColumn(SimulationEvent::description).setHeader("Description").setFlexGrow(2);
        eventGrid.setItems(res.eventTimeline());
        tabSheet.add("Event Timeline (" + res.eventTimeline().size() + ")", eventGrid);

        // Trajectories Grid
        Grid<TrajectoryPoint> trajGrid = new Grid<>(TrajectoryPoint.class, false);
        trajGrid.addColumn(t -> t.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);
        trajGrid.addColumn(TrajectoryPoint::entityKey).setHeader("Entity Key").setAutoWidth(true);
        trajGrid.addColumn(TrajectoryPoint::metric).setHeader("Metric").setAutoWidth(true);
        trajGrid.addColumn(t -> t.value().toPlainString()).setHeader("Value").setAutoWidth(true);
        trajGrid.setItems(res.inventoryTrajectories());
        tabSheet.add("Simulated Trajectories (" + res.inventoryTrajectories().size() + ")", trajGrid);

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

