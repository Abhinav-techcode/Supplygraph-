package com.supplygraph.web.views;

import com.supplygraph.application.integration.EnterpriseDataImportService;
import com.supplygraph.domain.model.integration.ImportJob;
import com.supplygraph.domain.model.integration.ImportJobId;
import com.supplygraph.domain.model.integration.ImportRecordFailure;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "import-preview", layout = MainLayout.class)
@PageTitle("Import Validation & Preview | SupplyGraph")
public class ImportValidationPreviewView extends VerticalLayout {

    private final EnterpriseDataImportService importService;
    private final TextField jobIdField;
    private final Span statusBadge;
    private final Paragraph summaryText;
    private final Grid<ImportRecordFailure> failureGrid;

    public ImportValidationPreviewView(EnterpriseDataImportService importService) {
        this.importService = importService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Import Schema Validation & Error Remediation Explorer");
        Paragraph desc = new Paragraph("Detailed inspection of validation failures, rejected values, and corrective remediation recommendations.");

        jobIdField = new TextField("Import Job ID");
        jobIdField.setPlaceholder("e.g. IMP-A1B2C3D4");
        jobIdField.setWidth("300px");

        Button inspectBtn = new Button("Inspect Job", e -> loadJob(jobIdField.getValue()));

        HorizontalLayout searchRow = new HorizontalLayout(jobIdField, inspectBtn);
        searchRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        statusBadge = new Span("Awaiting Selection");
        statusBadge.getElement().getThemeList().add("badge");

        summaryText = new Paragraph("Enter an import job identifier above to inspect validation diagnostics.");

        failureGrid = new Grid<>();
        failureGrid.setSizeFull();
        failureGrid.addColumn(ImportRecordFailure::rowIndex).setHeader("Row").setWidth("70px");
        failureGrid.addColumn(ImportRecordFailure::field).setHeader("Field").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::rejectedValue).setHeader("Rejected Value").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::errorCode).setHeader("Error Code").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::message).setHeader("Validation Message").setAutoWidth(true);
        failureGrid.addColumn(ImportRecordFailure::suggestedCorrection).setHeader("Recommended Correction").setAutoWidth(true);

        add(title, desc, searchRow, statusBadge, summaryText, failureGrid);

        // Preload first available job if any exists
        var jobs = importService.listImportJobs("WS-AUTO-01");
        if (!jobs.isEmpty()) {
            jobIdField.setValue(jobs.get(0).id().value());
            loadJob(jobs.get(0).id().value());
        }
    }

    private void loadJob(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            Notification.show("Please enter a valid Job ID", 3000, Notification.Position.MIDDLE);
            return;
        }

        Optional<ImportJob> opt = importService.getImportJob(ImportJobId.of(idStr.trim()));
        if (opt.isEmpty()) {
            Notification.show("Import job not found: " + idStr, 3000, Notification.Position.MIDDLE);
            return;
        }

        ImportJob job = opt.get();
        statusBadge.setText("Status: " + job.status().name());
        statusBadge.getElement().getThemeList().clear();
        statusBadge.getElement().getThemeList().add(job.rejectedRecords() == 0 ? "badge success" : "badge error");

        summaryText.setText("Dataset: " + job.filename() + " (" + job.category() + ") | Processed: " + job.totalRecords() + " records | Accepted: " + job.acceptedRecords() + " | Rejected: " + job.rejectedRecords() + " | Checksum: " + job.fileChecksum());

        failureGrid.setItems(job.failures());
    }
}

