package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ClosedLoopService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "closed-loop", layout = MainLayout.class)
@PageTitle("Closed-Loop Intelligence Dashboard | SupplyGraph")
public class ClosedLoopIntelligenceDashboardView extends ExecutionViewBase {
    private final ClosedLoopService closedLoop;
    private final Paragraph counts = new Paragraph();
    private final Paragraph latency = new Paragraph();
    private final Grid<ReassessmentRequest> requests = new Grid<>();
    private final TextArea rationale = new TextArea("Decision rationale (required, audited)");
    private final Span empty = new Span("No reassessment proposals.");
    private final Checkbox approverDiffers = new Checkbox("Approver must differ from creator");
    private final Checkbox executorDiffers = new Checkbox("Executor must differ from approver");
    private final Checkbox sensitiveSecond = new Checkbox("Sensitive tasks need a second approver");
    private final Checkbox allowLive = new Checkbox("Allow LIVE execution adapters (off = SIMULATED only)");
    private final NumberField maxAttempts = new NumberField("Max task attempts");
    private final NumberField maxDepth = new NumberField("Max replanning depth");
    private final NumberField threshold = new NumberField("Deviation threshold %");

    public ClosedLoopIntelligenceDashboardView(ExecutionQueryService queries, ClosedLoopService closedLoop) {
        super(queries, "Closed-Loop Intelligence Dashboard", "Execution outcomes, governed reassessment proposals and the workspace execution policy. Figures are computed from persisted workflows only.");
        this.closedLoop = closedLoop;
        rationale.setWidth("40em");
        requests.addColumn(r -> UiSupport.ts(r.createdAt())).setHeader("Proposed").setAutoWidth(true);
        requests.addColumn(ReassessmentRequest::workflowId).setHeader("Workflow").setAutoWidth(true);
        requests.addColumn(ReassessmentRequest::depth).setHeader("Depth").setAutoWidth(true);
        requests.addColumn(r -> String.format("%.1f%%", r.deviationPercent())).setHeader("Deviation").setAutoWidth(true);
        requests.addColumn(ReassessmentRequest::reason).setHeader("Reason").setAutoWidth(true);
        requests.addColumn(ReassessmentRequest::status).setHeader("Status").setAutoWidth(true);
        requests.addColumn(r -> r.decidedBy() == null ? "" : r.decidedBy()).setHeader("Decided by").setAutoWidth(true);
        requests.addComponentColumn(r -> {
            Button approve = new Button("Approve", e -> decide(r, true));
            Button decline = new Button("Decline", e -> decide(r, false));
            approve.setEnabled(r.status() == ReassessmentRequest.Status.PROPOSED);
            decline.setEnabled(r.status() == ReassessmentRequest.Status.PROPOSED);
            return new HorizontalLayout(approve, decline);
        }).setHeader("Human decision");
        requests.setHeight("16em");
        Button save = new Button("Save policy", e -> UiSupport.run("Execution policy saved", () -> queries.savePolicy(new ExecutionPolicy(UiSupport.workspace(), approverDiffers.getValue(), executorDiffers.getValue(),
                sensitiveSecond.getValue(), allowLive.getValue(), maxAttempts.getValue().intValue(), maxDepth.getValue().intValue(), threshold.getValue()))));
        body(new Button("Refresh", e -> refresh()), counts, latency, new H3("Reassessment proposals (humans decide; nothing re-plans or executes automatically)"), rationale, empty, requests,
                new H3("Execution policy (workspace administrators only)"), new HorizontalLayout(approverDiffers, executorDiffers, sensitiveSecond, allowLive), new HorizontalLayout(maxAttempts, maxDepth, threshold, save));
        refresh();
    }

    private void decide(ReassessmentRequest r, boolean approve) {
        if (UiSupport.run(approve ? "Reassessment approved; continue in the Recovery Decision Centre" : "Reassessment declined", () -> closedLoop.decide(r.id(), approve, rationale.getValue()))) { rationale.clear(); refresh(); }
    }

    private void refresh() {
        List<ExecutionWorkflow> wf = loadWorkflows();
        Map<WorkflowStatus, Long> byStatus = wf.stream().collect(Collectors.groupingBy(ExecutionWorkflow::status, () -> new EnumMap<>(WorkflowStatus.class), Collectors.counting()));
        long simulated = wf.stream().filter(ExecutionWorkflow::simulated).count();
        counts.setText(wf.isEmpty() ? "No workflows yet." : "Workflows by status: " + byStatus + " · SIMULATED " + simulated + " / LIVE " + (wf.size() - simulated));
        List<Duration> done = wf.stream().filter(w -> w.status() == WorkflowStatus.COMPLETED).map(w -> Duration.between(w.createdAt(), w.updatedAt())).toList();
        latency.setText(done.isEmpty() ? "Average workflow creation-to-completion time: unknown (no completed workflows)."
                : "Average workflow creation-to-completion time: " + Duration.ofMillis((long) done.stream().mapToLong(Duration::toMillis).average().orElse(0)).toSeconds() + " s over " + done.size() + " completed workflow(s) (includes human approval wait; sandbox workflows are SIMULATED).");
        List<ReassessmentRequest> reqs = UiSupport.call(() -> closedLoop.requests(UiSupport.workspace(), 100), List.<ReassessmentRequest>of());
        requests.setItems(reqs);
        empty.setVisible(reqs.isEmpty());
        requests.setVisible(!reqs.isEmpty());
        ExecutionPolicy p = UiSupport.call(() -> queries.policy(UiSupport.workspace()), ExecutionPolicy.defaults(UiSupport.workspace()));
        approverDiffers.setValue(p.approverMustDifferFromCreator()); executorDiffers.setValue(p.executorMustDifferFromApprover());
        sensitiveSecond.setValue(p.sensitiveTasksNeedSecondApproval()); allowLive.setValue(p.allowLiveExecution());
        maxAttempts.setValue((double) p.maxTaskAttempts()); maxDepth.setValue((double) p.maxReplanDepth()); threshold.setValue(p.deviationThresholdPercent());
    }
}
