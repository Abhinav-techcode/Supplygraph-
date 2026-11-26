package com.supplygraph.web.views;

import com.supplygraph.application.agent.AgentInvestigationRecord;
import com.supplygraph.application.port.out.AgentInvestigationRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "agent-history", layout = MainLayout.class)
@PageTitle("Agent Investigation History | ORBIX")
public class AgentInvestigationHistoryView extends VerticalLayout {

    private final AgentInvestigationRepositoryPort investigationRepository;
    private final Grid<AgentInvestigationRecord> grid;
    private final TextField workspaceFilter;
    private final Span statusMessage;

    @Autowired
    public AgentInvestigationHistoryView(@Autowired(required = false) AgentInvestigationRepositoryPort investigationRepository) {
        this.investigationRepository = investigationRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Specialized Agent Investigation & Tool History");
        Paragraph desc = new Paragraph("Audited trace of specialized agent investigations under zero-trust tool execution boundaries.");

        workspaceFilter = new TextField("Workspace ID", "WS-AUTO-01", "");
        Button refreshBtn = new Button("Refresh", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshBtn.addClickListener(e -> refreshData());

        statusMessage = new Span();
        statusMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");

        grid = new Grid<>();
        grid.setSizeFull();

        grid.addColumn(AgentInvestigationRecord::investigationId).setHeader("Investigation ID").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::agentName).setHeader("Agent Name").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::role).setHeader("Domain Role").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::targetEntity).setHeader("Target Entity").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::toolsInvoked).setHeader("Tools Invoked").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::intentsGenerated).setHeader("Intents Proposed").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f%%", r.confidenceScore() * 100)).setHeader("Data completeness").setAutoWidth(true);
        grid.addComponentColumn(this::createOutcomeBadge).setHeader("Outcome").setAutoWidth(true);
        grid.addColumn(r -> r.timestamp() != null ? r.timestamp().toString() : "-").setHeader("Timestamp").setAutoWidth(true);

        workspaceFilter.addValueChangeListener(e -> refreshData());

        HorizontalLayout controls = new HorizontalLayout(workspaceFilter, refreshBtn);
        controls.setAlignItems(Alignment.BASELINE);

        add(title, desc, controls, statusMessage, grid);
        refreshData();
    }

    private void refreshData() {
        if (investigationRepository == null) {
            grid.setItems(List.of());
            statusMessage.setText("Agent investigation repository is not configured in this environment.");
            return;
        }

        String wsVal = workspaceFilter.getValue();
        List<AgentInvestigationRecord> records;
        if (wsVal == null || wsVal.isBlank()) {
            records = investigationRepository.findAll();
        } else {
            records = investigationRepository.findByWorkspaceId(new WorkspaceId(wsVal.trim()));
        }

        grid.setItems(records);
        if (records.isEmpty()) {
            statusMessage.setText("No agent investigations have been recorded for workspace '" + wsVal + "'. Run an incident investigation to trigger specialist agents.");
        } else {
            statusMessage.setText(records.size() + " investigation trace(s) found for workspace '" + wsVal + "'.");
        }
    }

    private Span createOutcomeBadge(AgentInvestigationRecord record) {
        Span badge = new Span(record.outcome());
        if ("ARBITRATED_ALLOW".equalsIgnoreCase(record.outcome()) || "COMPLETED".equalsIgnoreCase(record.outcome())) {
            badge.getElement().getThemeList().add("badge success small");
        } else if ("PRESERVED".equalsIgnoreCase(record.outcome())) {
            badge.getElement().getThemeList().add("badge small");
        } else {
            badge.getElement().getThemeList().add("badge warning small");
        }
        return badge;
    }
}
