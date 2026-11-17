package com.supplygraph.web.views;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "audit-log", layout = MainLayout.class)
@PageTitle("Audit History & Governance | SupplyGraph")
public class AuditGovernanceView extends VerticalLayout {

    private final AuditGovernanceService auditService;
    private final Grid<AuditLogEntry> auditGrid;
    private final ComboBox<String> actionFilter;

    public AuditGovernanceView(AuditGovernanceService auditService) {
        this.auditService = auditService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Immutable Audit Trail & Governance Sign-Off");
        Paragraph desc = new Paragraph("Append-only regulatory audit log with cryptographic SHA-256 tamper-evident signatures verifying human approvals and consequential twin mutations.");

        actionFilter = new ComboBox<>("Action Filter");
        actionFilter.setItems("ALL", "PLAN_APPROVED", "PLAN_REJECTED", "DISRUPTION_CREATED", "DISRUPTION_RESOLVED", "DIGITAL_TWIN_ADVANCED", "REPLANNING_TRIGGERED", "USER_ROLE_CHANGED", "MONITORING_RULE_MODIFIED");
        actionFilter.setValue("ALL");
        actionFilter.addValueChangeListener(e -> refreshLogs());

        Button refreshBtn = new Button("Refresh Logs", ev -> refreshLogs());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(actionFilter, refreshBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        auditGrid = new Grid<>();
        auditGrid.setSizeFull();

        auditGrid.addColumn(e -> e.id().value()).setHeader("Audit ID").setAutoWidth(true);
        auditGrid.addColumn(e -> e.userId().value()).setHeader("Actor / User").setAutoWidth(true);
        auditGrid.addColumn(e -> e.workspaceId().value()).setHeader("Workspace").setAutoWidth(true);

        auditGrid.addComponentColumn(entry -> {
            Span badge = new Span(entry.action());
            String theme = entry.action().contains("APPROVED") ? "badge success" :
                    (entry.action().contains("REJECTED") ? "badge error" : "badge");
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Audited Action").setAutoWidth(true);

        auditGrid.addColumn(AuditLogEntry::targetEntityId).setHeader("Target Entity").setAutoWidth(true);
        auditGrid.addColumn(e -> e.previousStateVersion() + " → " + e.newStateVersion()).setHeader("State Transition").setAutoWidth(true);
        auditGrid.addColumn(e -> e.timestamp().toString()).setHeader("Timestamp").setAutoWidth(true);
        auditGrid.addColumn(AuditLogEntry::rationale).setHeader("Rationale / Reason").setWidth("220px");

        auditGrid.addComponentColumn(entry -> {
            String shortSig = entry.signatureHash().length() > 12 ? entry.signatureHash().substring(0, 12) + "..." : entry.signatureHash();
            Span sigBadge = new Span(shortSig);
            sigBadge.getElement().getThemeList().add("badge contrast");

            Button verifyBtn = new Button("Verify", ev -> {
                boolean valid = auditService.verifyIntegrity(entry.id());
                if (valid) {
                    Notification.show("✓ SHA-256 signature is VALID and tamper-evident", 3000, Notification.Position.BOTTOM_END);
                } else {
                    Notification.show("✗ WARNING: Signature mismatch! Record has been tampered with!", 5000, Notification.Position.BOTTOM_END);
                }
            });
            verifyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout sigLayout = new HorizontalLayout(sigBadge, verifyBtn);
            sigLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            return sigLayout;
        }).setHeader("SHA-256 Signature").setAutoWidth(true);

        add(title, desc, toolbar, auditGrid);
        refreshLogs();
    }

    private void refreshLogs() {
        List<AuditLogEntry> logs = auditService.getAllAuditLogs();
        String sel = actionFilter.getValue();
        if (sel != null && !"ALL".equalsIgnoreCase(sel)) {
            logs = logs.stream().filter(l -> sel.equalsIgnoreCase(l.action())).toList();
        }
        auditGrid.setItems(logs);
    }
}

