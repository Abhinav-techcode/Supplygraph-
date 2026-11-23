package com.supplygraph.web.components;

import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.MonteCarloApplicationService;
import com.supplygraph.application.service.SimulationApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.simulation.engine.SimulationConfiguration;
import com.supplygraph.simulation.montecarlo.MonteCarloConfiguration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.Instant;
import java.util.List;

/** Runs the discrete-event simulation and the Monte Carlo risk analysis on the current twin, and shows the answer in plain words. */
public class SimulationPanel extends VerticalLayout {
    private final Div results = new Div();

    public SimulationPanel(WorkspaceId ws, DisruptionApplicationService disruptions, SimulationApplicationService simulation, MonteCarloApplicationService monteCarlo) {
        setPadding(false);
        setSpacing(true);
        addClassName("sg-simulation-panel");
        H3 title = new H3("Simulation & risk");
        title.getStyle().set("margin", "0");
        Paragraph sub = new Paragraph("Replays the disruption over the next 28 days on your twin (simulation), then repeats it 200 times with random variation (Monte Carlo) to show how likely a bad outcome is.");
        sub.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        ComboBox<Disruption> incident = new ComboBox<>();
        incident.setWidth("280px");
        incident.setPlaceholder("Choose an incident");
        List<Disruption> list = disruptions.listDisruptions(ws);
        incident.setItems(list);
        incident.setItemLabelGenerator(Disruption::title);
        list.stream().findFirst().ifPresent(incident::setValue);
        Button run = new Button("Run simulation + Monte Carlo", VaadinIcon.PLAY_CIRCLE.create(), e -> {
            if (incident.getValue() == null) { Notification.show("Choose an incident first.", 2500, Notification.Position.MIDDLE); return; }
            try {
                Instant start = Instant.now();
                SimulationConfiguration sc = SimulationConfiguration.defaultHorizon(start, 86400L * 28);
                var sim = simulation.runSimulation(ws, incident.getValue().id(), null, sc);
                var mc = monteCarlo.runMonteCarlo(ws, incident.getValue().id(), null, MonteCarloConfiguration.standard(42L, 200, sc));
                results.removeAll();
                results.add(stat("Service level", String.format("%.1f%%", sim.serviceLevelPercentage()), "share of demand met over 28 days"),
                        stat("Total shortage", sim.totalShortageQuantity().stripTrailingZeros().toPlainString() + " units", "unmet demand in the replay"),
                        stat("Longest delay", String.format("%.1f days", sim.maxDelaySeconds() / 86400.0), "worst single delay"),
                        stat("Typical delay (P50 / P90)", String.format("%.1f / %.1f days", mc.medianDelayDays(), mc.p90DelayDays()), mc.replicationCount() + " random replications"),
                        stat("Chance of missing the service target", String.format("%.0f%%", mc.slaViolationProbability() * 100), "Monte Carlo"),
                        stat("Chance of recovering in time", String.format("%.0f%%", mc.recoveryWithinDeadlineProbability() * 100), "Monte Carlo"));
                List<String> warn = new java.util.ArrayList<>(sim.warnings());
                warn.addAll(mc.warnings());
                if (!warn.isEmpty()) {
                    Paragraph w = new Paragraph("Notes: " + String.join(" · ", warn));
                    w.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)").set("width", "100%");
                    results.add(w);
                }
            } catch (RuntimeException ex) {
                Notification.show("Simulation could not run: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        run.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        run.addClassName("sg-run-simulation");
        run.setEnabled(!list.isEmpty());
        results.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "10px").set("width", "100%");
        HorizontalLayout bar = new HorizontalLayout(incident, run);
        bar.setAlignItems(Alignment.CENTER);
        add(title, sub, bar, results);
    }

    private static Div stat(String label, String value, String note) {
        Div d = new Div();
        d.addClassName("sg-card");
        d.getStyle().set("flex", "1 1 150px");
        Span v = new Span(value);
        v.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "700").set("display", "block");
        Span l = new Span(label);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)").set("display", "block");
        Span n = new Span(note);
        n.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-tertiary-text-color)");
        d.add(v, l, n);
        return d;
    }
}
