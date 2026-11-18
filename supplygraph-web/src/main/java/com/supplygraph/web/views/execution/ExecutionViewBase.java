package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.ExecutionWorkflow;
import com.supplygraph.domain.model.execution.TaskStatus;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/** Common frame for Phase 10 views: heading, principal line, workflow selector, uniform labelling of SIMULATED vs LIVE. */
abstract class ExecutionViewBase extends VerticalLayout {
    protected final ExecutionQueryService queries;

    protected ExecutionViewBase(ExecutionQueryService queries, String title, String description) {
        this.queries = queries;
        setSizeFull();
        setPadding(true);
        add(new H2(title), new Paragraph(description));
        Paragraph who = new Paragraph("Acting as: " + UiSupport.currentUser() + " · workspace " + UiSupport.workspace().value());
        who.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        add(who);
    }

    protected void body(Component... c) { add(c); }

    protected ComboBox<ExecutionWorkflow> workflowSelector(String label) {
        ComboBox<ExecutionWorkflow> box = new ComboBox<>(label);
        box.setItemLabelGenerator(ExecutionViewBase::label);
        box.setWidth("42em");
        box.setItems(loadWorkflows());
        return box;
    }

    protected List<ExecutionWorkflow> loadWorkflows() { return UiSupport.call(() -> queries.list(UiSupport.workspace(), 200), List.<ExecutionWorkflow>of()); }

    static String label(ExecutionWorkflow w) { return w.id() + " · " + w.status() + " · " + (w.simulated() ? "SIMULATED" : "LIVE") + " · plan " + w.planId(); }

    static String mode(ExecutionWorkflow w) { return w.simulated() ? "SIMULATED" : "LIVE"; }

    static boolean needsAttention(TaskStatus s) { return s == TaskStatus.FAILED || s == TaskStatus.RETRY_PENDING || s == TaskStatus.BLOCKED || s == TaskStatus.RECONCILIATION_REQUIRED; }
}
