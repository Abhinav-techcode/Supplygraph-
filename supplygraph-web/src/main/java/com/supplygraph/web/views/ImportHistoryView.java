package com.supplygraph.web.views;

import com.supplygraph.application.integration.EnterpriseDataImportService;
import com.supplygraph.domain.model.integration.DataCategory;
import com.supplygraph.domain.model.integration.ImportJob;
import com.supplygraph.domain.model.integration.ImportStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "import-history", layout = MainLayout.class)
@PageTitle("Import History & Audit Log | ORBIX")
public class ImportHistoryView extends VerticalLayout {

    private final EnterpriseDataImportService importService;
    private final Grid<ImportJob> jobGrid;
    private final TextField searchField;
    private final ComboBox<ImportStatus> statusFilter;

    public ImportHistoryView(EnterpriseDataImportService importService) {
        this.importService = importService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Enterprise Data Import History & Batch Audit");
        Paragraph desc = new Paragraph("Full immutable audit record of external dataset ingestion batches, checksums, and rejected failure counts.");

        searchField = new TextField();
        searchField.setPlaceholder("Filter by filename or ID...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshData());

        statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder("Filter by Status");
        statusFilter.setItems(ImportStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> refreshData());

        Button refreshBtn = new Button("Refresh", e -> refreshData());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter, refreshBtn);
        toolbar.setWidthFull();

        jobGrid = new Grid<>();
        jobGrid.setSizeFull();

        jobGrid.addColumn(j -> j.id().value()).setHeader("Job ID").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::category).setHeader("Category").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::sourceType).setHeader("Format").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::filename).setHeader("Filename").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::totalRecords).setHeader("Total").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::acceptedRecords).setHeader("Accepted").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::rejectedRecords).setHeader("Rejected").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::status).setHeader("Status").setAutoWidth(true);
        jobGrid.addColumn(ImportJob::importedBy).setHeader("Operator").setAutoWidth(true);
        jobGrid.addColumn(j -> j.createdAt().toString().substring(0, 19)).setHeader("Timestamp").setAutoWidth(true);

        add(title, desc, toolbar, jobGrid);
        refreshData();
    }

    private void refreshData() {
        List<ImportJob> jobs = importService.listImportJobs("WS-AUTO-01");
        String term = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        ImportStatus filter = statusFilter.getValue();

        List<ImportJob> filtered = jobs.stream()
                .filter(j -> term.isEmpty() || j.id().value().toLowerCase().contains(term) || j.filename().toLowerCase().contains(term))
                .filter(j -> filter == null || j.status() == filter)
                .toList();

        jobGrid.setItems(filtered);
    }
}

