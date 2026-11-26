package com.supplygraph.web.views;

import com.supplygraph.application.integration.EnterpriseDataImportService;
import com.supplygraph.domain.model.integration.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Route(value = "data-integration", layout = MainLayout.class)
@PageTitle("Data Integration Centre | ORBIX")
public class DataIntegrationCentreView extends VerticalLayout {

    private final EnterpriseDataImportService importService;

    private final ComboBox<DataCategory> categorySelect;
    private final ComboBox<ImportSourceType> sourceSelect;
    private final TextField filenameField;
    private final TextArea payloadArea;
    private final VerticalLayout previewCard;
    private final Grid<ImportRecordFailure> failureGrid;
    private final Button executeBtn;

    private ImportJob currentJob;

    public DataIntegrationCentreView(EnterpriseDataImportService importService) {
        this.importService = importService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Enterprise Data Integration & Ingestion Centre");
        Paragraph desc = new Paragraph("Ingest structured supply chain datasets (CSV or JSON) across suppliers, materials, inventory, and logistics with validation, deduplication, and provenance tracking.");

        categorySelect = new ComboBox<>("Data Category", DataCategory.values());
        categorySelect.setItemLabelGenerator(DataCategory::displayName);
        categorySelect.setValue(DataCategory.SUPPLIER);
        categorySelect.setWidth("280px");

        sourceSelect = new ComboBox<>("Input Source Format", ImportSourceType.CSV, ImportSourceType.JSON);
        sourceSelect.setItemLabelGenerator(ImportSourceType::description);
        sourceSelect.setValue(ImportSourceType.CSV);
        sourceSelect.setWidth("280px");

        filenameField = new TextField("Source File Name", "supplier_master_q4.csv");
        filenameField.setWidth("280px");

        HorizontalLayout configRow = new HorizontalLayout(categorySelect, sourceSelect, filenameField);

        payloadArea = new TextArea("Data Payload (CSV / JSON Text)");
        payloadArea.setWidthFull();
        payloadArea.setHeight("200px");
        loadSampleData(DataCategory.SUPPLIER, ImportSourceType.CSV);

        categorySelect.addValueChangeListener(e -> loadSampleData(e.getValue(), sourceSelect.getValue()));
        sourceSelect.addValueChangeListener(e -> loadSampleData(categorySelect.getValue(), e.getValue()));

        Button sampleBtn = new Button("Load Sample Data", e -> loadSampleData(categorySelect.getValue(), sourceSelect.getValue()));
        Button validateBtn = new Button("Validate & Preview", e -> handleValidate());
        validateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        executeBtn = new Button("Execute Authoritative Ingestion", e -> handleExecute());
        executeBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        executeBtn.setEnabled(false);

        HorizontalLayout actionRow = new HorizontalLayout(sampleBtn, validateBtn, executeBtn);

        // Validation Preview Card
        previewCard = new VerticalLayout();
        previewCard.setWidthFull();
        previewCard.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("background", "var(--lumo-base-color)");
        previewCard.setVisible(false);

        failureGrid = new Grid<>();
        failureGrid.setHeight("180px");
        failureGrid.addColumn(ImportRecordFailure::rowIndex).setHeader("Row").setWidth("70px");
        failureGrid.addColumn(ImportRecordFailure::field).setHeader("Field").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::rejectedValue).setHeader("Rejected Value").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::errorCode).setHeader("Error Code").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::message).setHeader("Validation Message").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::suggestedCorrection).setHeader("Remediation").setAutoWidth(true);

        previewCard.add(new H3("Validation Preview & Schema Results"), failureGrid);

        add(title, desc, configRow, payloadArea, actionRow, previewCard);
    }

    private void handleValidate() {
        String text = payloadArea.getValue();
        if (text == null || text.isBlank()) {
            Notification.show("Please enter data payload to validate", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            currentJob = importService.parseAndValidate(
                    bytes,
                    sourceSelect.getValue(),
                    categorySelect.getValue(),
                    filenameField.getValue(),
                    "WS-AUTO-01",
                    "USR-SCM-01"
            );

            previewCard.setVisible(true);
            failureGrid.setItems(currentJob.failures());

            if (currentJob.status() == ImportStatus.VALIDATED) {
                executeBtn.setEnabled(true);
                Notification.show("Validation Successful: " + currentJob.acceptedRecords() + " valid records ready for ingestion", 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                executeBtn.setEnabled(false);
                Notification.show("Validation Rejected: " + currentJob.rejectedRecords() + " errors found", 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception ex) {
            Notification.show("Validation failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleExecute() {
        if (currentJob == null) return;
        try {
            ImportJob completed = importService.executeImport(currentJob.id(), "WS-AUTO-01", "USR-SCM-01");
            executeBtn.setEnabled(false);
            Notification.show("Data Ingested Successfully! Ingested " + completed.acceptedRecords() + " records into Digital Twin", 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Execution error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadSampleData(DataCategory category, ImportSourceType source) {
        if (category == null || source == null) return;

        if (source == ImportSourceType.CSV) {
            switch (category) {
                case SUPPLIER -> payloadArea.setValue("""
                        supplier_id,name,location,lead_time
                        SUP-FAB-01,Munich Semiconductor Fab,DEU,60
                        SUP-BAT-02,Gothenburg Cell Facility,SWE,45
                        SUP-CHEM-03,Lyon Specialty Chemical,FRA,28
                        """);
                case MATERIAL -> payloadArea.setValue("""
                        material_id,name,category,unit,criticality
                        MAT-MCU-32,Automotive 32-bit MCU,COMPONENT,PIECE,VERY_HIGH
                        MAT-LITH-01,Battery-Grade Lithium Hydroxide,RAW_MATERIAL,KG,VERY_HIGH
                        MAT-SEMI-EUV,EUV Photoresist Precursor,CHEMICAL,LITRE,HIGH
                        """);
                case INVENTORY -> payloadArea.setValue("""
                        warehouse_id,material_id,quantity,unit
                        WH-FRANKFURT,MAT-MCU-32,2500,PIECE
                        WH-STUTTGART,MAT-LITH-01,15000,KG
                        WH-MUNICH,MAT-SEMI-EUV,350,LITRE
                        """);
                case PRODUCTION -> payloadArea.setValue("""
                        site_id,line_id,product_id,daily_capacity
                        SITE-STUTTGART,LINE-AUTO-01,PROD-EV-PACK,120
                        SITE-MUNICH,LINE-SM-02,PROD-MCU-MODULE,500
                        """);
                case LOGISTICS -> payloadArea.setValue("""
                        origin_id,destination_id,lane_id,lead_time
                        SUP-FAB-01,WH-FRANKFURT,LANE-ROAD-01,2
                        SUP-BAT-02,WH-STUTTGART,LANE-ROAD-02,3
                        """);
                case DEMAND -> payloadArea.setValue("""
                        demand_id,product_id,quantity,priority
                        DEM-2026-001,PROD-EV-PACK,350,HIGH
                        DEM-2026-002,PROD-MCU-MODULE,1200,MEDIUM
                        """);
            }
        } else {
            payloadArea.setValue("""
                    [
                      {
                        "supplier_id": "SUP-FAB-01",
                        "name": "Munich Semiconductor Fab",
                        "location": "DEU",
                        "lead_time": "60"
                      },
                      {
                        "supplier_id": "SUP-BAT-02",
                        "name": "Gothenburg Cell Facility",
                        "location": "SWE",
                        "lead_time": "45"
                      }
                    ]
                    """);
        }
    }
}

