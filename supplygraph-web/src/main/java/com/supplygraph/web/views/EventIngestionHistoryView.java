package com.supplygraph.web.views;

import com.supplygraph.application.service.OperationalEventIngestionService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventProvenance;
import com.supplygraph.domain.model.event.EventSource;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Route(value = "event-ingestion", layout = MainLayout.class)
@PageTitle("Event Ingestion History | SupplyGraph")
public class EventIngestionHistoryView extends VerticalLayout {

    private final OperationalEventIngestionService ingestionService;
    private final Grid<OperationalEvent> historyGrid;

    public EventIngestionHistoryView(OperationalEventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Event Ingestion Hub & Batch Importer");
        Paragraph desc = new Paragraph("Ingest operational events via CSV batch payloads, external system telemetry, or manual operator submission.");

        Button importCsvBtn = new Button("Import CSV Batch", e -> openCsvDialog());
        importCsvBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button manualSubmitBtn = new Button("Simulate Manual Event", e -> openManualEventDialog());

        HorizontalLayout actionRow = new HorizontalLayout(importCsvBtn, manualSubmitBtn);

        historyGrid = new Grid<>();
        historyGrid.setSizeFull();

        historyGrid.addColumn(e -> e.getEventId().value()).setHeader("Event ID").setAutoWidth(true);
        historyGrid.addColumn(OperationalEvent::getEventType).setHeader("Event Type").setAutoWidth(true);
        historyGrid.addColumn(OperationalEvent::getSource).setHeader("Source Mode").setAutoWidth(true);
        historyGrid.addColumn(OperationalEvent::getStatus).setHeader("Ingestion Status").setAutoWidth(true);
        historyGrid.addColumn(e -> e.getProvenance().sourceSystem() + " (" + e.getProvenance().sourceUser() + ")").setHeader("Provenance").setAutoWidth(true);
        historyGrid.addColumn(OperationalEvent::getRejectionReason).setHeader("Audit Reason").setAutoWidth(true);
        historyGrid.addColumn(e -> e.getReceivedTimestamp().toString()).setHeader("Received At").setAutoWidth(true);

        add(title, desc, actionRow, historyGrid);
        refreshGrid();
    }

    private void refreshGrid() {
        historyGrid.setItems(ingestionService.getRecentEvents(100));
    }

    private void openCsvDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import CSV Operational Events");

        TextField wsField = new TextField("Workspace ID", "WS-AUTO-01", "");
        TextArea csvArea = new TextArea("CSV Payload Content");
        csvArea.setWidthFull();
        csvArea.setMinHeight("200px");
        csvArea.setValue("""
                eventType,affectedEntityId,affectedEntityType,idempotencyKey,payload
                SUPPLIER_DELIVERY_DELAYED,FAB-MUNICH,NODE,CSV-IDEMP-01,delayHours=48;carrier=DHL
                PRODUCTION_CAPACITY_CHANGED,PLANT-STUTTGART,NODE,CSV-IDEMP-02,capacityDropPercentage=25
                INVENTORY_LEVEL_CHANGED,HUB-FRANKFURT,NODE,CSV-IDEMP-03,onHandQuantity=850
                """);

        Button submitBtn = new Button("Ingest Batch", e -> {
            try {
                List<OperationalEvent> ingested = ingestionService.ingestCsv(
                        csvArea.getValue(), WorkspaceId.of(wsField.getValue().trim()), "operator_ui"
                );
                Notification.show("Successfully processed " + ingested.size() + " CSV events", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("CSV Ingestion Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new VerticalLayout(wsField, csvArea, submitBtn));
        dialog.open();
    }

    private void openManualEventDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Simulate Manual Operational Event");

        TextField wsField = new TextField("Workspace ID", "WS-AUTO-01", "");
        ComboBox<EventType> typeCombo = new ComboBox<>("Event Type", EventType.values());
        typeCombo.setValue(EventType.SUPPLIER_DELIVERY_DELAYED);

        TextField entityIdField = new TextField("Affected Entity ID", "FAB-MUNICH");
        TextField entityTypeField = new TextField("Affected Entity Type", "NODE");
        TextField keyField = new TextField("Payload Key", "delayHours");
        TextField valField = new TextField("Payload Value", "36");

        Button submitBtn = new Button("Publish Event", e -> {
            OperationalEvent event = OperationalEvent.createIngested(
                    typeCombo.getValue(),
                    EventSource.MANUAL_OPERATOR,
                    Instant.now(),
                    entityIdField.getValue().trim(),
                    entityTypeField.getValue().trim(),
                    WorkspaceId.of(wsField.getValue().trim()),
                    Map.of(keyField.getValue().trim(), valField.getValue().trim()),
                    EventProvenance.internal("operator_ui"),
                    "MANUAL-" + UUID.randomUUID()
            );
            ingestionService.ingestEvent(event);
            Notification.show("Event submitted: " + event.getEventId().value(), 3000, Notification.Position.TOP_CENTER);
            dialog.close();
            refreshGrid();
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new VerticalLayout(wsField, typeCombo, entityIdField, entityTypeField, keyField, valField, submitBtn));
        dialog.open();
    }
}

