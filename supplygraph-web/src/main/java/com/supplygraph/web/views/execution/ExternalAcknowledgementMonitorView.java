package com.supplygraph.web.views.execution;

import com.supplygraph.application.execution.ExecutionAcknowledgementService;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "execution-acknowledgements", layout = MainLayout.class)
@PageTitle("External Acknowledgement Monitor | SupplyGraph")
public class ExternalAcknowledgementMonitorView extends ExecutionViewBase {
    record Row(ExecutionWorkflow workflow, ExecutionTask task) {}

    private final ExecutionAcknowledgementService acks;
    private final Grid<Row> awaiting = new Grid<>();
    private final Grid<ExecutionEvidence> received = new Grid<>();
    private final Span empty = new Span("No tasks are waiting for an acknowledgement.");

    public ExternalAcknowledgementMonitorView(ExecutionQueryService queries, ExecutionAcknowledgementService acks) {
        super(queries, "External Acknowledgement Monitor", "Requests accepted by a target system but not yet confirmed complete. An acknowledgement is not completion; duplicates are ignored.");
        this.acks = acks;
        awaiting.addColumn(r -> r.workflow().id()).setHeader("Workflow").setAutoWidth(true);
        awaiting.addColumn(r -> r.task().id()).setHeader("Task").setAutoWidth(true);
        awaiting.addColumn(r -> r.task().type()).setHeader("Type").setAutoWidth(true);
        awaiting.addColumn(r -> r.task().externalReference() == null ? "—" : r.task().externalReference()).setHeader("Reference").setAutoWidth(true);
        awaiting.addColumn(r -> mode(r.workflow())).setHeader("Provenance").setAutoWidth(true);
        awaiting.addComponentColumn(r -> new Button("Record acknowledgement", e -> openDialog(r))).setHeader("Action");
        awaiting.setHeight("16em");
        received.addColumn(e -> UiSupport.ts(e.recordedAt())).setHeader("Recorded").setAutoWidth(true);
        received.addColumn(ExecutionEvidence::taskId).setHeader("Task").setAutoWidth(true);
        received.addColumn(e -> e.kind()).setHeader("Kind").setAutoWidth(true);
        received.addColumn(ExecutionEvidence::source).setHeader("Source").setAutoWidth(true);
        received.addColumn(e -> e.simulated() ? "SIMULATED" : "REAL").setHeader("Provenance").setAutoWidth(true);
        received.addColumn(e -> e.note() == null ? "" : e.note()).setHeader("Note").setAutoWidth(true);
        received.setHeight("16em");
        body(new Button("Refresh", e -> refresh()), empty, awaiting, new H3("Recent acknowledgements and evidence"), received);
        refresh();
    }

    private void openDialog(Row r) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Acknowledgement for " + r.task().id());
        TextField ref = new TextField("Reference");
        TextField source = new TextField("Source system");
        TextField note = new TextField("Note");
        ref.setValue(r.task().externalReference() == null ? "" : r.task().externalReference());
        source.setValue(r.workflow().simulated() ? "sandbox (simulated target system)" : "");
        Button ok = new Button("Task completed", e -> { if (UiSupport.run("Acknowledgement recorded", () -> acks.record(r.task().id(), ref.getValue(), true, source.getValue(), note.getValue()))) { d.close(); refresh(); } });
        ok.addThemeName("primary");
        Button fail = new Button("Task failed", e -> { if (UiSupport.run("Failure acknowledgement recorded", () -> acks.record(r.task().id(), ref.getValue(), false, source.getValue(), note.getValue()))) { d.close(); refresh(); } });
        d.add(new VerticalLayout(new Span(r.workflow().simulated() ? "This workflow is SIMULATED: the acknowledgement will be stored as simulated evidence and will NOT update the digital twin." : "LIVE workflow: a completion acknowledgement is forwarded to the monitoring pipeline."), ref, source, note));
        d.getFooter().add(new Button("Cancel", e -> d.close()), fail, ok);
        d.open();
    }

    private void refresh() {
        List<Row> rows = new ArrayList<>();
        List<ExecutionEvidence> evidence = new ArrayList<>();
        for (ExecutionWorkflow w : loadWorkflows()) {
            for (ExecutionTask t : w.tasks()) if (t.status() == TaskStatus.AWAITING_ACK) rows.add(new Row(w, t));
            UiSupport.call(() -> queries.evidence(w.id()), List.<ExecutionEvidence>of()).stream()
                    .filter(e -> e.kind() == ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT || e.kind() == ExecutionEvidence.Kind.ADAPTER_ACKNOWLEDGEMENT).forEach(evidence::add);
        }
        awaiting.setItems(rows);
        empty.setVisible(rows.isEmpty());
        awaiting.setVisible(!rows.isEmpty());
        received.setItems(evidence);
    }
}
