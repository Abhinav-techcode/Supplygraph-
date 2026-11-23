package com.supplygraph.web.views.execution;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.execution.ExecutionQueryService;
import com.supplygraph.application.execution.ExecutionReconciliationService;
import com.supplygraph.application.execution.ExecutionReconciliationService.Resolution;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.integration.UiSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.*;

@Route(value = "execution-reconciliation", layout = MainLayout.class)
@PageTitle("Reconciliation Workspace | SupplyGraph")
public class ReconciliationWorkspaceView extends ExecutionViewBase {
    record Row(ExecutionWorkflow workflow, ExecutionTask task) {}

    private final ExecutionReconciliationService reconciliation;
    private final Grid<Row> grid = new Grid<>();
    private final Span empty = new Span("Nothing needs reconciliation.");

    public ReconciliationWorkspaceView(ExecutionQueryService queries, ExecutionReconciliationService reconciliation) {
        super(queries, "Reconciliation Workspace", "Resolve tasks whose real-world outcome is unknown, or compare an observation with the expected outcome. The digital twin changes only from non-simulated, validated evidence.");
        this.reconciliation = reconciliation;
        grid.addColumn(r -> r.workflow().id()).setHeader("Workflow").setAutoWidth(true);
        grid.addColumn(r -> r.task().id()).setHeader("Task").setAutoWidth(true);
        grid.addColumn(r -> r.task().type()).setHeader("Type").setAutoWidth(true);
        grid.addColumn(r -> r.task().status()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(r -> r.task().expectedOutcome().isEmpty() ? "—" : r.task().expectedOutcome().toString()).setHeader("Expected").setAutoWidth(true);
        grid.addColumn(r -> r.task().lastError() == null ? "" : r.task().lastError()).setHeader("Why").setAutoWidth(true);
        grid.addColumn(r -> mode(r.workflow())).setHeader("Provenance").setAutoWidth(true);
        grid.addComponentColumn(r -> new Button("Reconcile…", e -> open(r))).setHeader("Action");
        grid.setSizeFull();
        body(new Button("Refresh", e -> refresh()), empty, grid);
        refresh();
    }

    private void open(Row r) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Reconcile " + r.task().id());
        ComboBox<Resolution> resolution = new ComboBox<>("Confirmed real-world outcome", Resolution.values());
        resolution.setItemLabelGenerator(x -> switch (x) {
            case CONFIRMED_COMPLETED -> "The action was completed";
            case CONFIRMED_NOT_EXECUTED -> "The action did NOT happen (allow a new attempt)";
            case CONFIRMED_FAILED -> "The action failed";
        });
        TextField observed = new TextField("Observed values (key=value, comma separated)");
        TextArea rationale = new TextArea("Rationale and source of confirmation (required)");
        observed.setWidth("30em"); rationale.setWidth("30em");
        Button apply = new Button("Record resolution", e -> {
            if (resolution.isEmpty()) { UiSupport.error(new ValidationException("Choose the confirmed outcome")); return; }
            if (UiSupport.run("Reconciled", () -> reconciliation.reconcile(r.task().id(), resolution.getValue(), rationale.getValue(), parse(observed.getValue())))) { d.close(); refresh(); }
        });
        Button observe = new Button("Compare observation with expected", e -> {
            if (UiSupport.run("Observation recorded", () -> reconciliation.recordObservation(r.task().id(), parse(observed.getValue()), "operator observation"))) { d.close(); refresh(); }
        });
        apply.addThemeName("primary");
        d.add(new VerticalLayout(new Span("Do not retry before confirming what actually happened in the target system."), resolution, observed, rationale));
        d.getFooter().add(new Button("Cancel", x -> d.close()), observe, apply);
        d.open();
    }

    static Map<String, String> parse(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return m;
        for (String part : text.split(",")) {
            int i = part.indexOf('=');
            if (i <= 0) throw new ValidationException("Observed values must look like key=value");
            m.put(part.substring(0, i).trim(), part.substring(i + 1).trim());
        }
        return m;
    }

    private void refresh() {
        List<Row> rows = new ArrayList<>();
        for (ExecutionWorkflow w : loadWorkflows()) for (ExecutionTask t : w.tasks()) if (t.status() == TaskStatus.RECONCILIATION_REQUIRED || t.status() == TaskStatus.AWAITING_ACK) rows.add(new Row(w, t));
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
