package com.supplygraph.web.views;

import com.supplygraph.application.service.AdaptiveReplanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.replanning.ReplanningRun;
import com.supplygraph.domain.model.replanning.ReplanningStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "adaptive-replanning", layout = MainLayout.class)
@PageTitle("Adaptive Replanning Monitor | SupplyGraph")
public class AdaptiveReplanningMonitorView extends VerticalLayout {

    private final AdaptiveReplanningService replanningService;
    private final Grid<ReplanningRun> grid;
    private final TextField workspaceFilter;

    public AdaptiveReplanningMonitorView(AdaptiveReplanningService replanningService) {
        this.replanningService = replanningService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Adaptive Agentic Replanning Monitor");
        Paragraph desc = new Paragraph("Tracks multi-agent selective replanning cycles, ConcurGuard re-arbitration, and human re-approval requirements.");

        workspaceFilter = new TextField("Workspace ID", "WS-AUTO-01", "");
        workspaceFilter.addValueChangeListener(e -> refreshGrid());

        grid = new Grid<>();
        grid.setSizeFull();

        grid.addColumn(r -> r.getId().value()).setHeader("Run ID").setAutoWidth(true);
        grid.addComponentColumn(this::createStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addColumn(r -> String.join(", ", r.getAffectedAgentIds())).setHeader("Affected Agents").setAutoWidth(true);
        grid.addColumn(r -> r.getPreservedActionIds().size() + " actions").setHeader("Preserved Scope").setAutoWidth(true);
        grid.addColumn(r -> r.getReconsideredActionIds().size() + " actions").setHeader("Reconsidered Scope").setAutoWidth(true);
        grid.addColumn(r -> r.getRevisedPlanId() != null ? r.getRevisedPlanId().value() : "In Progress").setHeader("Revised Plan ID").setAutoWidth(true);
        grid.addColumn(r -> r.getStartedAt().toString()).setHeader("Started At").setAutoWidth(true);

        grid.addItemClickListener(e -> showAuditTrail(e.getItem()));

        add(title, desc, new HorizontalLayout(workspaceFilter), grid);
        refreshGrid();
    }

    private void refreshGrid() {
        String ws = workspaceFilter.getValue();
        if (ws != null && !ws.isBlank()) {
            grid.setItems(replanningService.getRuns(WorkspaceId.of(ws.trim())));
        } else {
            grid.setItems(List.of());
        }
    }

    private Span createStatusBadge(ReplanningRun run) {
        Span badge = new Span(run.getStatus().name());
        switch (run.getStatus()) {
            case READY_FOR_REVIEW -> badge.getElement().getThemeList().add("badge success");
            case REPLANNING, VALIDATING, SIMULATING -> badge.getElement().getThemeList().add("badge primary");
            case DETECTED, ASSESSING -> badge.getElement().getThemeList().add("badge warning");
            case FAILED, CANCELLED -> badge.getElement().getThemeList().add("badge error");
            case SUPERSEDED, NO_ACTION_REQUIRED -> badge.getElement().getThemeList().add("badge contrast");
        }
        return badge;
    }

    private void showAuditTrail(ReplanningRun run) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Replanning Run Audit: " + run.getId().value());

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("Reason: " + run.getReason()));
        layout.add(new Paragraph("Retries: " + run.getRetryCount() + " of " + run.getMaxRetries()));

        Pre pre = new Pre(String.join("\n", run.getAuditTrail()));
        pre.getStyle().set("background", "var(--lumo-contrast-5pct)").set("padding", "var(--lumo-space-s)");
        layout.add(pre);

        dialog.add(layout);
        dialog.open();
    }
}

