package com.supplygraph.web.views.integration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "event-replay", layout = MainLayout.class)
@PageTitle("Event Replay Centre | ORBIX")
public class EventReplayCentreView extends IntegrationViewBase {
    private final OutboxService outbox;
    private final ComboBox<OutboxEvent> selector = new ComboBox<>("Dead-lettered event");
    private final TextArea rationale = new TextArea("Replay rationale (required, audited)");
    private final Paragraph status = new Paragraph("Replay re-queues the event with its original idempotency key, so consumers deduplicate any repeated effect.");

    public EventReplayCentreView(OutboxService outbox) {
        super("Event Replay Centre", "Authorized, audited replay of dead-lettered events. Requires the EVENT_REPLAY permission in the event's workspace.");
        this.outbox = outbox;
        selector.setItemLabelGenerator(e -> e.eventType() + " · " + e.aggregateId() + " · " + e.lastError());
        selector.setWidth("40em");
        rationale.setWidth("40em");
        Button replay = new Button("Replay event", e -> {
            OutboxEvent ev = selector.getValue();
            if (ev == null) { UiSupport.error(new ValidationException("Select an event")); return; }
            if (UiSupport.run("Event re-queued for delivery", () -> outbox.replay(ev.id(), rationale.getValue()))) { rationale.clear(); load(); }
        });
        replay.addThemeName("primary");
        body(new HorizontalLayout(selector), rationale, replay, status);
        load();
    }

    private void load() {
        selector.setItems(UiSupport.call(() -> outbox.list(UiSupport.workspace(), OutboxStatus.DEAD_LETTER, 200), List.<OutboxEvent>of()));
        selector.clear();
    }
}
