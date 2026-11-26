package com.supplygraph.web.views;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.quality.DataQualityEngineService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.quality.DataQualityFinding;
import com.supplygraph.domain.model.quality.DataQualityReport;
import com.supplygraph.domain.model.quality.QualityDimension;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "data-quality", layout = MainLayout.class)
@PageTitle("Data Quality Dashboard | ORBIX")
public class DataQualityDashboardView extends VerticalLayout {

    private final DataQualityEngineService qualityService;
    private final DigitalTwinRepositoryPort digitalTwinRepository;

    private final H1 overallScoreHeading;
    private final Paragraph summaryStats;
    private final HorizontalLayout dimensionRow;
    private final Grid<DataQualityFinding> findingsGrid;

    public DataQualityDashboardView(
            DataQualityEngineService qualityService,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.qualityService = qualityService;
        this.digitalTwinRepository = digitalTwinRepository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Continuous Data Quality & Governance Dashboard");
        Paragraph desc = new Paragraph("Deterministic assessment of digital twin datasets across 8 enterprise quality dimensions: completeness, validity, consistency, uniqueness, referential integrity, temporal validity, unit consistency, and provenance.");

        Button auditBtn = new Button("Run Comprehensive Quality Audit", e -> runAudit());
        auditBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Overall Score Widget
        VerticalLayout scoreCard = new VerticalLayout();
        scoreCard.setWidth("320px");
        scoreCard.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("background", "var(--lumo-base-color)");

        H4 scoreLabel = new H4("Composite Quality Score");
        scoreLabel.getStyle().set("margin", "0");
        overallScoreHeading = new H1("--/100");
        overallScoreHeading.getStyle().set("margin", "4px 0").set("color", "var(--lumo-primary-color)");
        summaryStats = new Paragraph("Entities: 0 | Clean: 0 | Flawed: 0");

        scoreCard.add(scoreLabel, overallScoreHeading, summaryStats);

        dimensionRow = new HorizontalLayout();
        dimensionRow.setWidthFull();

        HorizontalLayout topMetrics = new HorizontalLayout(scoreCard, auditBtn);
        topMetrics.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        findingsGrid = new Grid<>();
        findingsGrid.setSizeFull();
        findingsGrid.addColumn(DataQualityFinding::ruleId).setHeader("Rule ID").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::dimension).setHeader("Dimension").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::entityId).setHeader("Entity ID").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::field).setHeader("Field").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::actualValue).setHeader("Observed Value").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::severity).setHeader("Severity").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::message).setHeader("Issue Description").setAutoWidth(true);
        findingsGrid.addColumn(DataQualityFinding::suggestedRemediation).setHeader("Remediation").setAutoWidth(true);

        add(title, desc, topMetrics, new H4("Evaluated Quality Dimensions"), dimensionRow, new H4("Quality Findings & Anomalies"), findingsGrid);

        loadLatest();
    }

    private void loadLatest() {
        Optional<DataQualityReport> opt = qualityService.getLatestReport("WS-AUTO-01");
        opt.ifPresent(this::renderReport);
    }

    private void runAudit() {
        Optional<DigitalTwinState> stateOpt = digitalTwinRepository.findByWorkspaceId(WorkspaceId.of("WS-AUTO-01"));
        if (stateOpt.isEmpty()) {
            Notification.show("Digital twin state not initialized", 3000, Notification.Position.MIDDLE);
            return;
        }

        DataQualityReport report = qualityService.evaluateWorkspaceQuality("WS-AUTO-01", stateOpt.get());
        renderReport(report);
        Notification.show("Audit Complete: Score " + String.format("%.1f", report.overallScore()) + "/100", 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void renderReport(DataQualityReport report) {
        overallScoreHeading.setText(String.format("%.1f", report.overallScore()) + "/100");
        overallScoreHeading.getStyle().set("color", report.overallScore() >= 80.0 ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");

        summaryStats.setText("Entities Evaluated: " + report.totalEntitiesEvaluated() +
                " | Clean: " + report.cleanRecordsCount() +
                " | Anomalies: " + report.flawedRecordsCount() +
                " | Critical: " + report.criticalFindingCount());

        dimensionRow.removeAll();
        for (QualityDimension dim : QualityDimension.values()) {
            Double score = report.dimensionScores().getOrDefault(dim, 100.0);
            VerticalLayout dimCard = new VerticalLayout();
            dimCard.setWidth("160px");
            dimCard.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "6px")
                    .set("padding", "8px")
                    .set("background", "var(--lumo-contrast-5pct)");

            Span name = new Span(dim.displayName());
            name.getStyle().set("font-size", "11px").set("font-weight", "600");
            H3 scoreVal = new H3(String.format("%.0f", score) + "%");
            scoreVal.getStyle().set("margin", "2px 0").set("color", score >= 80 ? "green" : "red");

            dimCard.add(name, scoreVal);
            dimensionRow.add(dimCard);
        }

        findingsGrid.setItems(report.findings());
    }
}

