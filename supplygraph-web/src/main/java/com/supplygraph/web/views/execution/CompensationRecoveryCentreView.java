package com.supplygraph.web.views.execution;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.execution.ExecutionCompensationService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "execution-compensation", layout = MainLayout.class)
@PageTitle("Compensation and Recovery Centre | ORBIX")
public class CompensationRecoveryCentreView extends ExecutionViewBase {
    record Row(ExecutionWorkflow workflow, ExecutionTask task) {}

    private final ExecutionCompensationService compensation;
    private final Grid<Row> grid = new Grid<>();
    private final TextArea rationale = new TextArea("Rationale (required, audited)");
    private final Span empty = new Span("No workflows need compensation decisions.");

    public CompensationRecoveryCentreView(ExecutionQueryService queries, ExecutionCompensationService compensation) {
        super(queries, "Compensation and Recovery Centre", "Completed tasks of failed or partial workflows. Compensation is offered only where a valid business compensation exists; irreversible actions are shown as such.");
        this.compensation = compensation;
        rationale.setWidth("40em");
        grid.addColumn(r -> r.workflow().id()).setHeader("Workflow").setAutoWidth(true);
        grid.addColumn(r -> r.workflow().status()).setHeader("Workflow status").setAutoWidth(true);
        grid.addColumn(r -> r.task().id()).setHeader("Task").setAutoWidth(true);
        grid.addColumn(r -> r.task().type()).setHeader("Type").setAutoWidth(true);
        grid.addColumn(r -> r.task().type().compensable() ? r.task().type().compensation() : "No valid compensation: " + (r.task().type().compensation() == null ? "irreversible action" : r.task().type().compensation())).setHeader("Compensation").setAutoWidth(true);
        grid.addComponentColumn(r -> {
            Button b = new Button("Compensate", e -> { if (UiSupport.run("Task compensated", () -> compensation.compensate(r.task().id(), rationale.getValue()))) refresh(); });
            b.setEnabled(r.task().type().compensable());
            return b;
        }).setHeader("Action");
        grid.setSizeFull();
        Button accept = new Button("Accept partial result for selected workflow", e -> grid.getSelectedItems().stream().findFirst().ifPresentOrElse(
                r -> { if (UiSupport.run("Partial result accepted", () -> compensation.acceptPartial(r.workflow().id(), rationale.getValue()))) refresh(); },
                () -> UiSupport.error(new ValidationException("Select a row first"))));
        body(rationale, new HorizontalLayout(accept, new Button("Refresh", e -> refresh())), empty, grid);
        refresh();
    }

    private void refresh() {
        List<Row> rows = new ArrayList<>();
        for (ExecutionWorkflow w : loadWorkflows()) {
            if (w.status() != WorkflowStatus.COMPENSATION_REQUIRED && w.status() != WorkflowStatus.PARTIALLY_COMPLETED && w.status() != WorkflowStatus.FAILED) continue;
            for (ExecutionTask t : w.tasks()) if (t.status() == TaskStatus.COMPLETED) rows.add(new Row(w, t));
        }
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
