package com.supplygraph.web.views.execution;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.execution.ExecutionEvent;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Set;

@Route(value = "execution-timeline", layout = MainLayout.class)
@PageTitle("Execution Audit Timeline | ORBIX")
public class ExecutionAuditTimelineView extends ExecutionViewBase {
    private static final Set<String> ACTIONS = Set.of("WORKFLOW_CREATED", "WORKFLOW_BLOCKED", "WORKFLOW_APPROVED", "WORKFLOW_REJECTED", "WORKFLOW_STARTED", "WORKFLOW_CANCELLED",
            "TASK_RETRIED", "TASK_RECONCILED", "TASK_COMPENSATED", "REASSESSMENT_PROPOSED", "REASSESSMENT_DECIDED");
    private final AuditGovernanceService audit;
    private final ComboBox<ExecutionWorkflow> selector = workflowSelector("Workflow (empty = whole workspace)");
    private final Grid<ExecutionEvent> timeline = new Grid<>();
    private final Grid<AuditLogEntry> signed = new Grid<>();
    private final Span empty = new Span("No execution events recorded yet.");

    public ExecutionAuditTimelineView(ExecutionQueryService queries, AuditGovernanceService audit) {
        super(queries, "Execution Audit Timeline", "Append-only timeline with correlation identifiers, and the signed audit entries for consequential transitions. SIMULATED entries are labelled.");
        this.audit = audit;
        selector.setClearButtonVisible(true);
        selector.addValueChangeListener(e -> refresh());
        timeline.addColumn(e -> UiSupport.ts(e.at())).setHeader("When").setAutoWidth(true);
        timeline.addColumn(ExecutionEvent::workflowId).setHeader("Workflow").setAutoWidth(true);
        timeline.addColumn(e -> e.taskId() == null ? "" : e.taskId()).setHeader("Task").setAutoWidth(true);
        timeline.addColumn(ExecutionEvent::type).setHeader("Event").setAutoWidth(true);
        timeline.addColumn(ExecutionEvent::actor).setHeader("Actor").setAutoWidth(true);
        timeline.addColumn(e -> e.simulated() ? "SIMULATED" : "LIVE").setHeader("Provenance").setAutoWidth(true);
        timeline.addColumn(ExecutionEvent::correlationId).setHeader("Correlation").setAutoWidth(true);
        timeline.addColumn(ExecutionEvent::detail).setHeader("Detail").setAutoWidth(true);
        timeline.setHeight("24em");
        signed.addColumn(e -> UiSupport.ts(e.timestamp())).setHeader("When").setAutoWidth(true);
        signed.addColumn(e -> e.userId().value()).setHeader("User").setAutoWidth(true);
        signed.addColumn(AuditLogEntry::action).setHeader("Action").setAutoWidth(true);
        signed.addColumn(AuditLogEntry::targetEntityId).setHeader("Target").setAutoWidth(true);
        signed.addColumn(AuditLogEntry::rationale).setHeader("Rationale").setAutoWidth(true);
        signed.addColumn(e -> e.isSignatureValid() ? "valid" : "INVALID").setHeader("Signature").setAutoWidth(true);
        signed.setHeight("18em");
        body(selector, empty, timeline, new H3("Signed audit entries"), signed);
        refresh();
    }

    private void refresh() {
        ExecutionWorkflow w = selector.getValue();
        List<ExecutionEvent> events = w == null ? UiSupport.call(() -> queries.workspaceTimeline(UiSupport.workspace(), 300), List.<ExecutionEvent>of())
                : UiSupport.call(() -> queries.timeline(w.id()), List.<ExecutionEvent>of());
        timeline.setItems(events);
        empty.setVisible(events.isEmpty());
        timeline.setVisible(!events.isEmpty());
        signed.setItems(UiSupport.call(() -> audit.getAuditTrail(UiSupport.workspace()), List.<AuditLogEntry>of()).stream().filter(e -> ACTIONS.contains(e.action())).toList());
    }
}
