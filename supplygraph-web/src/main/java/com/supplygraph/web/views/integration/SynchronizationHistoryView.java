package com.supplygraph.web.views.integration;

import com.supplygraph.application.connector.ConnectorExecutionService;
import com.supplygraph.domain.model.connector.ConnectorExecution;
import com.supplygraph.domain.model.connector.ExecutionStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "sync-history", layout = MainLayout.class)
@PageTitle("Synchronization History | SupplyGraph")
public class SynchronizationHistoryView extends IntegrationViewBase {
    private final ConnectorExecutionService executions;
    private final Grid<ConnectorExecution> grid = new Grid<>();
    private final ComboBox<ExecutionStatus> status = new ComboBox<>("Status", ExecutionStatus.values());
    private final Span empty = new Span("No connector executions recorded yet.");
    private final Paragraph detail = new Paragraph("Select an execution to inspect its failure details.");

    public SynchronizationHistoryView(ConnectorExecutionService executions) {
        super("Synchronization History", "Every connector execution with counts, error category, retry lineage and provenance (SIMULATED vs LIVE).");
        this.executions = executions;
        status.setClearButtonVisible(true);
        status.addValueChangeListener(e -> refresh());
        grid.addColumn(x -> UiSupport.ts(x.startedAt())).setHeader("Started").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::operation).setHeader("Operation").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::trigger).setHeader("Trigger").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(x -> x.simulated() ? "SIMULATED" : "LIVE").setHeader("Provenance").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::recordsReceived).setHeader("Received").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::recordsProcessed).setHeader("Processed").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::recordsRejected).setHeader("Rejected").setAutoWidth(true);
        grid.addColumn(ConnectorExecution::attempt).setHeader("Attempt").setAutoWidth(true);
        grid.addColumn(x -> x.errorCategory() == null ? "" : x.errorCategory().name()).setHeader("Error").setAutoWidth(true);
        grid.addComponentColumn(x -> {
            Button retry = new Button("Retry");
            retry.setEnabled(x.status() == ExecutionStatus.FAILED_RETRYABLE);
            retry.addClickListener(e -> { if (UiSupport.run("Retry executed", () -> executions.retry(x.id()))) refresh(); });
            return retry;
        }).setHeader("Action");
        grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(x -> detail.setText("Execution " + x.id() + " · correlation " + x.correlationId()
                + " · requested by " + x.requestedBy() + (x.retryOf() != null ? " · retry of " + x.retryOf() : "")
                + (x.errorMessage() != null ? " · " + x.errorMessage() : " · no error"))));
        grid.setSizeFull();
        body(new HorizontalLayout(status, new Button("Refresh", e -> refresh())), empty, grid, detail);
        refresh();
    }

    private void refresh() {
        List<ConnectorExecution> rows = UiSupport.call(() -> executions.workspaceHistory(UiSupport.workspace(), 200), List.<ConnectorExecution>of()).stream()
                .filter(x -> status.getValue() == null || x.status() == status.getValue()).toList();
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
