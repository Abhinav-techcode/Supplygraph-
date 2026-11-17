package com.supplygraph.web.views;

import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "events", layout = MainLayout.class)
@PageTitle("Operational Event Monitor | SupplyGraph")
public class OperationalEventMonitorView extends VerticalLayout {

    private final OperationalEventIngestionService ingestionService;
    private final Grid<OperationalEvent> eventGrid;
    private final TextField workspaceFilter;
    private final ComboBox<EventType> typeFilter;

    public OperationalEventMonitorView(OperationalEventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Operational Event Monitor");
        Paragraph desc = new Paragraph("Continuous stream of operational telemetry, internal milestones, and external software events.");

        workspaceFilter = new TextField("Workspace ID");
        workspaceFilter.setValue("WS-AUTO-01");
        workspaceFilter.addValueChangeListener(e -> refreshEvents());

        typeFilter = new ComboBox<>("Event Type");
        typeFilter.setItems(EventType.values());
        typeFilter.setItemLabelGenerator(EventType::name);
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(e -> refreshEvents());

        HorizontalLayout filters = new HorizontalLayout(workspaceFilter, typeFilter);
        filters.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        eventGrid = new Grid<>();
        eventGrid.setSizeFull();

        eventGrid.addColumn(e -> e.getEventId().value()).setHeader("Event ID").setAutoWidth(true);
        eventGrid.addColumn(OperationalEvent::getEventType).setHeader("Type").setAutoWidth(true);
        eventGrid.addComponentColumn(this::createStatusBadge).setHeader("Status").setAutoWidth(true);
        eventGrid.addColumn(OperationalEvent::getSource).setHeader("Source").setAutoWidth(true);
        eventGrid.addColumn(OperationalEvent::getAffectedEntityId).setHeader("Affected Entity").setAutoWidth(true);
        eventGrid.addColumn(OperationalEvent::getAffectedEntityType).setHeader("Entity Type").setAutoWidth(true);
        eventGrid.addColumn(e -> e.getEventTimestamp().toString()).setHeader("Timestamp").setAutoWidth(true);

        eventGrid.addItemClickListener(e -> showEventDetails(e.getItem()));

        add(title, desc, filters, eventGrid);
        refreshEvents();
    }

    private void refreshEvents() {
        String ws = workspaceFilter.getValue();
        List<OperationalEvent> events;
        if (ws != null && !ws.isBlank()) {
            events = ingestionService.getWorkspaceEvents(WorkspaceId.of(ws.trim()), 100);
        } else {
            events = ingestionService.getRecentEvents(100);
        }

        if (typeFilter.getValue() != null) {
            events = events.stream().filter(e -> e.getEventType() == typeFilter.getValue()).toList();
        }

        eventGrid.setItems(events);
    }

    private Span createStatusBadge(OperationalEvent event) {
        Span badge = new Span(event.getStatus().name());
        switch (event.getStatus()) {
            case PROCESSED -> badge.getElement().getThemeList().add("badge success");
            case VALIDATED -> badge.getElement().getThemeList().add("badge");
            case INGESTED -> badge.getElement().getThemeList().add("badge contrast");
            case REJECTED, DUPLICATE -> badge.getElement().getThemeList().add("badge error");
        }
        return badge;
    }

    private void showEventDetails(OperationalEvent event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Event Details: " + event.getEventId().value());

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("Status: " + event.getStatus() + " (Reason: " + event.getRejectionReason() + ")"));
        layout.add(new Paragraph("Provenance: " + event.getProvenance().sourceSystem() + " by " + event.getProvenance().sourceUser()));
        layout.add(new Paragraph("Idempotency Key: " + event.getIdempotencyKey()));

        Pre pre = new Pre("Payload:\n" + event.getPayload().toString());
        pre.getStyle().set("background", "var(--lumo-contrast-5pct)").set("padding", "var(--lumo-space-s)");
        layout.add(pre);

        dialog.add(layout);
        dialog.open();
    }
}

