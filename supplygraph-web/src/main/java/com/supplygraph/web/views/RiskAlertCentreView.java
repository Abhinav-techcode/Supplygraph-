package com.supplygraph.web.views;

import com.supplygraph.application.service.OperationalMonitoringService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.monitoring.MonitoringSeverity;
import com.supplygraph.domain.model.monitoring.RiskAlert;
import com.supplygraph.domain.model.monitoring.RiskAlertStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "risk-alerts", layout = MainLayout.class)
@PageTitle("Risk & Alert Centre | SupplyGraph")
public class RiskAlertCentreView extends VerticalLayout {

    private final OperationalMonitoringService monitoringService;
    private final Grid<RiskAlert> alertGrid;
    private final TextField workspaceFilter;

    public RiskAlertCentreView(OperationalMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Operational Risk & Alert Centre");
        Paragraph desc = new Paragraph("Correlated, deduplicated risk findings retaining factual evidence, trigger event provenance, and digital twin version.");

        workspaceFilter = new TextField("Workspace ID", "WS-AUTO-01");
        workspaceFilter.addValueChangeListener(e -> refreshAlerts());

        Button refreshBtn = new Button("Refresh Alerts", e -> refreshAlerts());

        HorizontalLayout headerRow = new HorizontalLayout(workspaceFilter, refreshBtn);
        headerRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        alertGrid = new Grid<>();
        alertGrid.setSizeFull();

        alertGrid.addColumn(a -> a.getId().value()).setHeader("Alert ID").setAutoWidth(true);
        alertGrid.addComponentColumn(this::createSeverityBadge).setHeader("Severity").setAutoWidth(true);
        alertGrid.addColumn(RiskAlert::getTitle).setHeader("Title").setAutoWidth(true);
        alertGrid.addColumn(RiskAlert::getAffectedEntityId).setHeader("Affected Entity").setAutoWidth(true);
        alertGrid.addColumn(a -> "v" + a.getSourceStateVersion().value()).setHeader("State Version").setAutoWidth(true);
        alertGrid.addComponentColumn(this::createStatusBadge).setHeader("Status").setAutoWidth(true);
        alertGrid.addColumn(a -> a.getDetectedAt().toString()).setHeader("Detected At").setAutoWidth(true);

        alertGrid.addComponentColumn(alert -> {
            HorizontalLayout actions = new HorizontalLayout();
            if (alert.getStatus() == RiskAlertStatus.ACTIVE) {
                Button ackBtn = new Button("Acknowledge", e -> {
                    monitoringService.acknowledgeAlert(alert.getId());
                    Notification.show("Alert acknowledged: " + alert.getId().value(), 2000, Notification.Position.BOTTOM_END);
                    refreshAlerts();
                });
                ackBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

                Button resBtn = new Button("Resolve", e -> {
                    monitoringService.resolveAlert(alert.getId());
                    Notification.show("Alert resolved: " + alert.getId().value(), 2000, Notification.Position.BOTTOM_END);
                    refreshAlerts();
                });
                resBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

                actions.add(ackBtn, resBtn);
            }
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        alertGrid.addItemClickListener(e -> showEvidenceDialog(e.getItem()));

        add(title, desc, headerRow, alertGrid);
        refreshAlerts();
    }

    private void refreshAlerts() {
        String ws = workspaceFilter.getValue();
        if (ws != null && !ws.isBlank()) {
            alertGrid.setItems(monitoringService.getAllAlerts(WorkspaceId.of(ws.trim())));
        } else {
            alertGrid.setItems(List.of());
        }
    }

    private Span createSeverityBadge(RiskAlert alert) {
        Span badge = new Span(alert.getSeverity().name());
        switch (alert.getSeverity()) {
            case CRITICAL -> badge.getElement().getThemeList().add("badge error primary");
            case HIGH -> badge.getElement().getThemeList().add("badge error");
            case MEDIUM -> badge.getElement().getThemeList().add("badge warning");
            case LOW, INFO -> badge.getElement().getThemeList().add("badge contrast");
        }
        return badge;
    }

    private Span createStatusBadge(RiskAlert alert) {
        Span badge = new Span(alert.getStatus().name());
        switch (alert.getStatus()) {
            case ACTIVE -> badge.getElement().getThemeList().add("badge error small");
            case ACKNOWLEDGED -> badge.getElement().getThemeList().add("badge warning small");
            case RESOLVED -> badge.getElement().getThemeList().add("badge success small");
            case SUPPRESSED -> badge.getElement().getThemeList().add("badge contrast small");
        }
        return badge;
    }

    private void showEvidenceDialog(RiskAlert alert) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Risk Finding Evidence: " + alert.getId().value());

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("Description: " + alert.getDescription()));
        layout.add(new Paragraph("Rule: " + alert.getRuleId().value() + " | Suppression Key: " + alert.getSuppressionKey()));

        Pre pre = new Pre("Factual Evidence:\n" + alert.getEvidence().toString());
        pre.getStyle().set("background", "var(--lumo-contrast-5pct)").set("padding", "var(--lumo-space-s)");
        layout.add(pre);

        dialog.add(layout);
        dialog.open();
    }
}

