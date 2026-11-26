package com.supplygraph.web.views.integration;

import com.supplygraph.adapter.integration.OutboxDispatcherWorker;
import com.supplygraph.application.connector.RuntimeOperationsService;
import com.supplygraph.application.connector.RuntimeOperationsService.Snapshot;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;

@Route(value = "runtime-operations", layout = MainLayout.class)
@PageTitle("Runtime Operations | ORBIX")
public class RuntimeOperationsDashboardView extends IntegrationViewBase {
    private final RuntimeOperationsService ops;
    private final ObjectProvider<OutboxDispatcherWorker> worker;
    private final FlexLayout tiles = new FlexLayout();
    private final Paragraph categories = new Paragraph();
    private final Paragraph workerLine = new Paragraph();

    public RuntimeOperationsDashboardView(RuntimeOperationsService ops, ObjectProvider<OutboxDispatcherWorker> worker) {
        super("Runtime Operations Dashboard", "Indicators computed from persisted executions and outbox rows. “unknown” means there is no recorded data to compute from.");
        this.ops = ops;
        this.worker = worker;
        tiles.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        body(new Button("Refresh", e -> refresh()), tiles, new H3("Integration error categories"), categories, new H3("Background worker"), workerLine);
        refresh();
    }

    private void refresh() {
        tiles.removeAll();
        Snapshot s = UiSupport.call(() -> ops.snapshot(UiSupport.workspace()), null);
        if (s == null) return;
        tile("Executions sampled", String.valueOf(s.sampledExecutions()));
        tile("Succeeded / failed", s.succeeded() + " / " + s.failed());
        tile("Import rejection rate", UiSupport.pct(s.importRejectionRate()));
        tile("Avg sync latency", UiSupport.dur(s.averageSyncLatency()));
        tile("Retry executions", String.valueOf(s.totalRetries()));
        tile("Outbox pending", String.valueOf(s.outboxCounts().getOrDefault(com.supplygraph.domain.model.outbox.OutboxStatus.PENDING, 0L)));
        tile("Dead letters", String.valueOf(s.outboxCounts().getOrDefault(com.supplygraph.domain.model.outbox.OutboxStatus.DEAD_LETTER, 0L)));
        tile("Event failure rate", UiSupport.pct(s.eventFailureRate()));
        tile("Avg delivery latency", UiSupport.dur(s.averageDeliveryLatency()));
        categories.setText(s.errorCategories().isEmpty() ? "No integration errors recorded." : s.errorCategories().toString());
        OutboxDispatcherWorker w = worker.getIfAvailable();
        workerLine.setText(w == null ? "Outbox worker is disabled (supplygraph.outbox.worker.enabled=false)."
                : "Running: " + w.isRunning() + " · delivered since start: " + w.deliveredTotal() + " · failed runs: " + w.failedRuns()
                + " · last successful run: " + (w.lastRunEpochMillis() == 0 ? "none yet" : UiSupport.ts(Instant.ofEpochMilli(w.lastRunEpochMillis()))));
    }

    private void tile(String label, String value) {
        VerticalLayout t = new VerticalLayout(new Paragraph(label), new H3(value));
        t.setPadding(true); t.setSpacing(false); t.setWidth("14em");
        t.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "var(--lumo-border-radius-m)").set("margin", "var(--lumo-space-xs)");
        tiles.add(t);
    }
}
