package com.supplygraph.web.views;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.List;

@Route(value = "agent-history", layout = MainLayout.class)
@PageTitle("Agent Investigation History | SupplyGraph")
public class AgentInvestigationHistoryView extends VerticalLayout {

    public record AgentInvestigationRecord(
            String investigationId,
            String agentName,
            String role,
            String targetEntity,
            int toolsInvoked,
            int intentsGenerated,
            double confidenceScore,
            String outcome,
            Instant timestamp
    ) {}

    private final Grid<AgentInvestigationRecord> grid;
    private final TextField workspaceFilter;

    public AgentInvestigationHistoryView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Specialized Agent Investigation & Tool History");
        Paragraph desc = new Paragraph("Audited trace of specialized agent investigations under zero-trust tool execution boundaries.");

        workspaceFilter = new TextField("Workspace ID", "WS-AUTO-01");

        grid = new Grid<>();
        grid.setSizeFull();

        grid.addColumn(AgentInvestigationRecord::investigationId).setHeader("Investigation ID").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::agentName).setHeader("Agent Name").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::role).setHeader("Domain Role").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::targetEntity).setHeader("Target Entity").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::toolsInvoked).setHeader("Tools Invoked").setAutoWidth(true);
        grid.addColumn(AgentInvestigationRecord::intentsGenerated).setHeader("Intents Proposed").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f%%", r.confidenceScore() * 100)).setHeader("Confidence").setAutoWidth(true);
        grid.addComponentColumn(this::createOutcomeBadge).setHeader("Outcome").setAutoWidth(true);
        grid.addColumn(r -> r.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);

        grid.setItems(getSeedRecords());

        add(title, desc, new HorizontalLayout(workspaceFilter), grid);
    }

    private Span createOutcomeBadge(AgentInvestigationRecord record) {
        Span badge = new Span(record.outcome());
        if ("ARBITRATED_ALLOW".equalsIgnoreCase(record.outcome())) {
            badge.getElement().getThemeList().add("badge success small");
        } else if ("PRESERVED".equalsIgnoreCase(record.outcome())) {
            badge.getElement().getThemeList().add("badge small");
        } else {
            badge.getElement().getThemeList().add("badge warning small");
        }
        return badge;
    }

    private List<AgentInvestigationRecord> getSeedRecords() {
        return List.of(
                new AgentInvestigationRecord("INV-001", "ProcurementAgent", "TIER_2_PROCUREMENT", "FAB-MUNICH", 3, 2, 0.94, "ARBITRATED_ALLOW", Instant.now().minusSeconds(3600)),
                new AgentInvestigationRecord("INV-002", "LogisticsAgent", "FREIGHT_ROUTING", "LANE-MUNICH-STUTTGART", 2, 1, 0.88, "PRESERVED", Instant.now().minusSeconds(3500)),
                new AgentInvestigationRecord("INV-003", "InventoryAgent", "SAFETY_STOCK_ALLOCATION", "HUB-FRANKFURT", 4, 2, 0.91, "ARBITRATED_ALLOW", Instant.now().minusSeconds(3400)),
                new AgentInvestigationRecord("INV-004", "ProductionAgent", "LINE_CAPACITY_REBALANCE", "PLANT-STUTTGART", 3, 1, 0.85, "SELECTIVE_REPLAN", Instant.now().minusSeconds(1800))
        );
    }
}

