package com.supplygraph.web.views.integration;

import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "dead-letters", layout = MainLayout.class)
@PageTitle("Dead-Letter Management | ORBIX")
public class DeadLetterManagementView extends IntegrationViewBase {
    private final OutboxService outbox;
    private final Grid<OutboxEvent> grid = new Grid<>();
    private final Pre payload = new Pre("Select a dead-lettered event to inspect its payload and failure.");
    private final Span empty = new Span("No dead-lettered events. Delivery failures that exhaust retries or are permanent will appear here.");

    public DeadLetterManagementView(OutboxService outbox) {
        super("Dead-Letter Management", "Events that could not be delivered. Inspect the failure here; replay from the Event Replay Centre.");
        this.outbox = outbox;
        grid.addColumn(e -> UiSupport.ts(e.updatedAt())).setHeader("Dead-lettered").setAutoWidth(true);
        grid.addColumn(OutboxEvent::eventType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(OutboxEvent::aggregateId).setHeader("Aggregate").setAutoWidth(true);
        grid.addColumn(OutboxEvent::attempts).setHeader("Attempts").setAutoWidth(true);
        grid.addColumn(OutboxEvent::replayCount).setHeader("Replays").setAutoWidth(true);
        grid.addColumn(e -> e.lastError() == null ? "" : e.lastError()).setHeader("Failure").setAutoWidth(true);
        grid.addSelectionListener(s -> s.getFirstSelectedItem().ifPresent(e -> payload.setText(
                "id: " + e.id() + "\ncorrelation: " + e.correlationId() + "\nidempotencyKey: " + e.idempotencyKey() + "\nerror: " + e.lastError() + "\n\npayload:\n" + e.payload())));
        payload.getStyle().set("max-height", "16em").set("overflow", "auto");
        grid.setSizeFull();
        body(new Button("Refresh", e -> refresh()), empty, grid, payload);
        refresh();
    }

    private void refresh() {
        List<OutboxEvent> rows = UiSupport.call(() -> outbox.list(UiSupport.workspace(), OutboxStatus.DEAD_LETTER, 200), List.<OutboxEvent>of());
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
