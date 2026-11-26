package com.supplygraph.web.views.integration;

import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;

@Route(value = "event-delivery", layout = MainLayout.class)
@PageTitle("Event Delivery Monitor | ORBIX")
public class EventDeliveryMonitorView extends IntegrationViewBase {
    private final OutboxService outbox;
    private final Grid<OutboxEvent> grid = new Grid<>();
    private final ComboBox<OutboxStatus> status = new ComboBox<>("Status", OutboxStatus.values());
    private final Span counts = new Span();
    private final Span empty = new Span("No integration events recorded yet.");

    public EventDeliveryMonitorView(OutboxService outbox) {
        super("Event Delivery Monitor", "Durable outbox state: pending, in-flight, delivered and dead-lettered integration events.");
        this.outbox = outbox;
        status.setClearButtonVisible(true);
        status.addValueChangeListener(e -> refresh());
        grid.addColumn(e -> UiSupport.ts(e.createdAt())).setHeader("Created").setAutoWidth(true);
        grid.addColumn(OutboxEvent::eventType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(OutboxEvent::aggregateId).setHeader("Aggregate").setAutoWidth(true);
        grid.addColumn(OutboxEvent::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(OutboxEvent::attempts).setHeader("Attempts").setAutoWidth(true);
        grid.addColumn(e -> UiSupport.ts(e.nextAttemptAt())).setHeader("Next attempt").setAutoWidth(true);
        grid.addColumn(OutboxEvent::correlationId).setHeader("Correlation").setAutoWidth(true);
        grid.addColumn(e -> e.lastError() == null ? "" : e.lastError()).setHeader("Last error").setAutoWidth(true);
        grid.setSizeFull();
        body(new HorizontalLayout(status, new Button("Refresh", e -> refresh())), counts, empty, grid);
        refresh();
    }

    private void refresh() {
        Map<OutboxStatus, Long> c = UiSupport.call(() -> outbox.counts(UiSupport.workspace()), Map.<OutboxStatus, Long>of());
        counts.setText(c.isEmpty() ? "" : "Pending " + c.get(OutboxStatus.PENDING) + " · In-flight " + c.get(OutboxStatus.IN_FLIGHT)
                + " · Delivered " + c.get(OutboxStatus.DELIVERED) + " · Dead-letter " + c.get(OutboxStatus.DEAD_LETTER));
        List<OutboxEvent> rows = UiSupport.call(() -> outbox.list(UiSupport.workspace(), status.getValue(), 200), List.<OutboxEvent>of());
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
