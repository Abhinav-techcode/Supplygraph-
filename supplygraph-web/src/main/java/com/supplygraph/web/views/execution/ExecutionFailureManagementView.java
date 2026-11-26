package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionAdvisoryService;
import com.supplygraph.application.execution.ExecutionOrchestrationService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "execution-failures", layout = MainLayout.class)
@PageTitle("Execution Failure Management | ORBIX")
public class ExecutionFailureManagementView extends ExecutionViewBase {
    record Row(ExecutionWorkflow workflow, ExecutionTask task) {}

    private final ExecutionOrchestrationService orchestration;
    private final ExecutionAdvisoryService advisory;
    private final Grid<Row> grid = new Grid<>();
    private final Paragraph explanation = new Paragraph("Select a task to see a deterministic explanation (advisory only; it cannot change anything).");
    private final Span empty = new Span("No failed, blocked, retry-pending or unknown-outcome tasks.");

    public ExecutionFailureManagementView(ExecutionQueryService queries, ExecutionOrchestrationService orchestration, ExecutionAdvisoryService advisory) {
        super(queries, "Execution Failure Management", "Failures by class. Retryable failures can be retried (bounded). Unknown outcomes must be reconciled, never retried.");
        this.orchestration = orchestration;
        this.advisory = advisory;
        grid.addColumn(r -> r.workflow().id()).setHeader("Workflow").setAutoWidth(true);
        grid.addColumn(r -> r.task().id()).setHeader("Task").setAutoWidth(true);
        grid.addColumn(r -> r.task().type()).setHeader("Type").setAutoWidth(true);
        grid.addColumn(r -> classify(r.task().status())).setHeader("Class").setAutoWidth(true);
        grid.addColumn(r -> r.task().attempts() + "/" + r.task().maxAttempts()).setHeader("Attempts").setAutoWidth(true);
        grid.addColumn(r -> r.task().lastError() == null ? "" : r.task().lastError()).setHeader("Error").setAutoWidth(true);
        grid.addComponentColumn(r -> {
            Button retry = new Button("Retry", e -> { if (UiSupport.run("Retry executed", () -> orchestration.retryTask(r.workflow().id(), r.task().id()))) refresh(); });
            retry.setEnabled(r.task().status() == TaskStatus.RETRY_PENDING);
            return retry;
        }).setHeader("Action");
        grid.addSelectionListener(s -> s.getFirstSelectedItem().ifPresent(r -> {
            var a = UiSupport.call(() -> advisory.explainFailure(r.workflow().id(), r.task().id()), null);
            if (a != null) explanation.setText("[" + a.role().displayName() + " · advisory, not actionable] " + a.text() + " Evidence: " + String.join(" | ", a.evidence()));
        }));
        grid.setSizeFull();
        body(new HorizontalLayout(new Button("Refresh", e -> refresh())), empty, grid, explanation);
        refresh();
    }

    static String classify(TaskStatus s) {
        return switch (s) {
            case RETRY_PENDING -> "RETRYABLE"; case FAILED -> "PERMANENT"; case RECONCILIATION_REQUIRED -> "UNKNOWN OUTCOME"; case BLOCKED -> "BLOCKED (dependency)"; default -> s.name();
        };
    }

    private void refresh() {
        List<Row> rows = new ArrayList<>();
        for (ExecutionWorkflow w : loadWorkflows()) for (ExecutionTask t : w.tasks()) if (needsAttention(t.status())) rows.add(new Row(w, t));
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
