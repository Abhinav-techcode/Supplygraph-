package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionOrchestrationService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "execution-monitor", layout = MainLayout.class)
@PageTitle("Live Execution Monitor | SupplyGraph")
public class LiveExecutionMonitorView extends ExecutionViewBase {
    private final ExecutionOrchestrationService orchestration;
    private final ComboBox<ExecutionWorkflow> selector = workflowSelector("Workflow");
    private final Grid<ExecutionTask> tasks = new Grid<>();
    private final Grid<ExecutionAttempt> attempts = new Grid<>();
    private final Paragraph status = new Paragraph("Select a workflow.");
    private final TextField cancelReason = new TextField("Cancellation reason");

    public LiveExecutionMonitorView(ExecutionQueryService queries, ExecutionOrchestrationService orchestration) {
        super(queries, "Live Execution Monitor", "Task and attempt state straight from the database (auto-refresh every 5 s). Only READY workflows can be started; unknown outcomes are never retried here.");
        this.orchestration = orchestration;
        selector.addValueChangeListener(e -> refresh());
        tasks.addColumn(ExecutionTask::id).setHeader("Task").setAutoWidth(true);
        tasks.addColumn(ExecutionTask::type).setHeader("Type").setAutoWidth(true);
        tasks.addColumn(ExecutionTask::status).setHeader("Status").setAutoWidth(true);
        tasks.addColumn(t -> t.attempts() + "/" + t.maxAttempts()).setHeader("Attempts").setAutoWidth(true);
        tasks.addColumn(t -> t.externalReference() == null ? "" : t.externalReference()).setHeader("External ref").setAutoWidth(true);
        tasks.addColumn(t -> t.lastError() == null ? "" : t.lastError()).setHeader("Last error / note").setAutoWidth(true);
        tasks.addComponentColumn(t -> {
            Button retry = new Button("Retry", e -> { if (UiSupport.run("Retry executed", () -> orchestration.retryTask(selector.getValue().id(), t.id()))) refresh(); });
            retry.setEnabled(t.status() == TaskStatus.RETRY_PENDING);
            return retry;
        }).setHeader("Action");
        tasks.setHeight("16em");
        attempts.addColumn(a -> UiSupport.ts(a.startedAt())).setHeader("Started").setAutoWidth(true);
        attempts.addColumn(ExecutionAttempt::taskId).setHeader("Task").setAutoWidth(true);
        attempts.addColumn(ExecutionAttempt::attemptNo).setHeader("#").setAutoWidth(true);
        attempts.addColumn(ExecutionAttempt::adapterId).setHeader("Adapter").setAutoWidth(true);
        attempts.addColumn(a -> a.simulated() ? "SIMULATED" : "LIVE").setHeader("Provenance").setAutoWidth(true);
        attempts.addColumn(ExecutionAttempt::outcome).setHeader("Outcome").setAutoWidth(true);
        attempts.addColumn(a -> a.message() == null ? "" : a.message()).setHeader("Message").setAutoWidth(true);
        attempts.setHeight("14em");
        Button start = new Button("Start execution", e -> withSelection(w -> { if (UiSupport.run("Execution started", () -> orchestration.start(w.id()))) refresh(); }));
        start.addThemeName("primary");
        Button cancel = new Button("Cancel workflow", e -> withSelection(w -> { if (UiSupport.run("Workflow cancelled", () -> orchestration.cancel(w.id(), cancelReason.getValue()))) refresh(); }));
        cancel.addThemeName("error");
        body(new HorizontalLayout(selector, start, new Button("Refresh", e -> refresh())), status, tasks, new HorizontalLayout(cancelReason, cancel), attempts);
    }

    private void withSelection(java.util.function.Consumer<ExecutionWorkflow> c) {
        if (selector.getValue() == null) { UiSupport.error(new IllegalArgumentException("Select a workflow first")); return; }
        c.accept(selector.getValue());
    }

    private void refresh() {
        if (selector.getValue() == null) return;
        ExecutionWorkflow w = UiSupport.call(() -> queries.get(selector.getValue().id()), null);
        if (w == null) return;
        status.setText("Workflow " + w.id() + " is " + w.status() + " · mode " + mode(w) + (w.statusReason() != null ? " · " + w.statusReason() : "")
                + (w.simulated() ? " · acknowledgements here are SIMULATED, not real-world confirmations" : ""));
        tasks.setItems(w.tasks());
        attempts.setItems(UiSupport.call(() -> queries.attempts(w.id()), List.<ExecutionAttempt>of()));
    }

    @Override protected void onAttach(AttachEvent e) {
        super.onAttach(e);
        e.getUI().setPollInterval(5000);
        e.getUI().addPollListener(p -> refresh());
    }

    @Override protected void onDetach(DetachEvent e) {
        e.getUI().setPollInterval(-1);
        super.onDetach(e);
    }
}
