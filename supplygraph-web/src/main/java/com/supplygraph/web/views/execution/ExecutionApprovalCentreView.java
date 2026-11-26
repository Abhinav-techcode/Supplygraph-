package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionApprovalService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.execution.WorkflowApproval;
import com.supplygraph.domain.model.execution.WorkflowStatus;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "execution-approvals", layout = MainLayout.class)
@PageTitle("Execution Approval Centre | ORBIX")
public class ExecutionApprovalCentreView extends ExecutionViewBase {
    private final ExecutionApprovalService approvals;
    private final Grid<ExecutionWorkflow> grid = new Grid<>();
    private final TextArea rationale = new TextArea("Rationale (required, audited)");
    private final Span empty = new Span("No workflows are awaiting approval.");

    public ExecutionApprovalCentreView(ExecutionQueryService queries, ExecutionApprovalService approvals) {
        super(queries, "Execution Approval Centre", "Separation of duties is enforced on the server: you cannot approve a workflow you created, and sensitive tasks need a second, different approver.");
        this.approvals = approvals;
        grid.addColumn(ExecutionWorkflow::id).setHeader("Workflow").setAutoWidth(true);
        grid.addColumn(ExecutionViewBase::mode).setHeader("Mode").setAutoWidth(true);
        grid.addColumn(w -> w.tasks().size()).setHeader("Tasks").setAutoWidth(true);
        grid.addColumn(w -> w.hasSensitiveTasks() ? "yes" : "no").setHeader("Sensitive tasks").setAutoWidth(true);
        grid.addColumn(ExecutionWorkflow::createdBy).setHeader("Created by").setAutoWidth(true);
        grid.addColumn(w -> w.approvals().isEmpty() ? "none" : w.approvals().stream().map(a -> a.scope() + ":" + a.userId()).reduce((a, b) -> a + ", " + b).orElse("")).setHeader("Approvals").setAutoWidth(true);
        grid.addComponentColumn(w -> {
            Button approve = new Button("Approve", e -> act(() -> approvals.approve(w.id(), WorkflowApproval.Scope.WORKFLOW, rationale.getValue()), "Workflow approval recorded"));
            approve.setEnabled(!w.hasApproval(WorkflowApproval.Scope.WORKFLOW));
            Button second = new Button("Approve sensitive tasks", e -> act(() -> approvals.approve(w.id(), WorkflowApproval.Scope.SENSITIVE_TASKS, rationale.getValue()), "Sensitive-task approval recorded"));
            second.setVisible(w.hasSensitiveTasks());
            second.setEnabled(!w.hasApproval(WorkflowApproval.Scope.SENSITIVE_TASKS));
            Button reject = new Button("Reject", e -> act(() -> approvals.reject(w.id(), rationale.getValue()), "Workflow rejected"));
            reject.addThemeName("error");
            return new HorizontalLayout(approve, second, reject);
        }).setHeader("Decision");
        grid.setSizeFull();
        rationale.setWidth("40em");
        body(rationale, empty, grid, new Button("Refresh", e -> refresh()));
        refresh();
    }

    private void act(Runnable action, String ok) {
        if (UiSupport.run(ok, action)) { rationale.clear(); refresh(); }
    }

    private void refresh() {
        List<ExecutionWorkflow> pending = loadWorkflows().stream().filter(w -> w.status() == WorkflowStatus.PENDING_APPROVAL).toList();
        grid.setItems(pending);
        empty.setVisible(pending.isEmpty());
        grid.setVisible(!pending.isEmpty());
    }
}
