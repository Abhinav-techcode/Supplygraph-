package com.supplygraph.web.components;

import com.supplygraph.adapter.ai.IncidentInvestigationService;
import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.application.agent.AgentLearningService;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.agent.AgentTrackRecord;
import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.*;

/** Shows the eight specialist agents as a live team: what each does, what it last proposed, how confident it was and how reliable it has proven. */
public class AgentActivityPanel extends VerticalLayout {
    private final WorkspaceId ws;
    private final IncidentInvestigationService investigations;
    private final AgentLearningService learning;
    private final AgentInvestigationRepositoryPort repo;
    private final Grid<AgentRole> grid = new Grid<>();
    private final Span status = new Span();

    public AgentActivityPanel(WorkspaceId ws, DisruptionApplicationService disruptions, IncidentInvestigationService investigations,
                              AgentLearningService learning, AgentInvestigationRepositoryPort repo) {
        this.ws = ws;
        this.investigations = investigations;
        this.learning = learning;
        this.repo = repo;
        setPadding(false);
        setSpacing(true);
        addClassName("sg-agents-panel");
        H3 title = new H3("Specialist agents");
        title.getStyle().set("margin", "0");
        Paragraph sub = new Paragraph("Eight rule-based specialists (plain Java analysis, no language model) read the digital twin and propose actions with evidence. The percentage is data completeness, not a probability. People decide; the track record counts accepted decisions.");
        sub.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        ComboBox<Disruption> incident = new ComboBox<>();
        incident.setPlaceholder("Choose an incident");
        incident.setWidth("280px");
        List<Disruption> list = disruptions.listDisruptions(ws);
        incident.setItems(list);
        incident.setItemLabelGenerator(Disruption::title);
        list.stream().findFirst().ifPresent(incident::setValue);
        Button run = new Button("Run all 8 agents", VaadinIcon.PLAY.create(), e -> {
            if (incident.getValue() == null) { Notification.show("Choose an incident first.", 2500, Notification.Position.MIDDLE); return; }
            try {
                var r = investigations.investigate(ws, incident.getValue());
                status.setText("Ran at " + java.time.LocalTime.now().withNano(0) + ": " + r.proposal().approvedIntents().size() + " action(s) accepted by ConcurGuard, "
                        + r.proposal().rejectedIntents().size() + " held for a human.");
                refresh();
            } catch (RuntimeException ex) {
                Notification.show("Agents could not run: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        run.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        run.addClassName("sg-run-agents");
        run.setEnabled(!list.isEmpty());
        HorizontalLayout bar = new HorizontalLayout(incident, run);
        bar.setAlignItems(Alignment.CENTER);
        status.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        grid.setAllRowsVisible(true);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_COMPACT, com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER);
        grid.addColumn(AgentRole::displayName).setHeader("Agent").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> latest(r).map(x -> x.intentsGenerated() == 0 ? "Advice only" : x.intentsGenerated() + " action on " + x.targetEntity()).orElse("Not run yet")).setHeader("Last proposal").setAutoWidth(true);
        grid.addColumn(r -> latest(r).map(x -> Math.round(x.confidenceScore() * 100) + "%").orElse("-")).setHeader("Data completeness").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> learning.trackRecords(ws).getOrDefault(r.name(), AgentTrackRecord.empty(r.name())).summary()).setHeader("Track record").setAutoWidth(true);
        add(title, sub, bar, status, grid);
        refresh();
    }

    private Optional<AgentInvestigationRecord> latest(AgentRole role) {
        return repo.findByWorkspaceId(ws).stream().filter(x -> x.role().equals(role.name())).max(Comparator.comparing(AgentInvestigationRecord::timestamp));
    }

    private void refresh() { grid.setItems(Arrays.asList(AgentRole.values())); grid.getDataProvider().refreshAll(); }
}
