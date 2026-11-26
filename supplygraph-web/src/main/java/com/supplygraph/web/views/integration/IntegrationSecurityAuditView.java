package com.supplygraph.web.views.integration;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.connector.ConnectorRegistryService;
import com.supplygraph.domain.model.audit.AuditLogEntry;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Set;

@Route(value = "integration-security", layout = MainLayout.class)
@PageTitle("Integration Security & Audit | ORBIX")
public class IntegrationSecurityAuditView extends IntegrationViewBase {
    private static final Set<String> ACTIONS = Set.of("CONNECTOR_REGISTERED", "CONNECTOR_CONFIGURED", "CONNECTOR_ACTIVATED", "CONNECTOR_DEACTIVATED",
            "CONNECTOR_EXECUTED", "CONNECTOR_EXECUTION_RETRIED", "EVENT_REPLAYED", "RECONCILIATION_RESOLVED", "FIELD_POLICY_CHANGED", "WEBHOOK_REJECTED");
    private final AuditGovernanceService audit;
    private final ConnectorRegistryService registry;
    private final Grid<Connector> posture = new Grid<>();
    private final Grid<AuditLogEntry> trail = new Grid<>();

    public IntegrationSecurityAuditView(AuditGovernanceService audit, ConnectorRegistryService registry) {
        super("Integration Security & Audit", "Connector security posture (credential references only — secrets are never stored or shown) and the signed audit trail of integration actions.");
        this.audit = audit;
        this.registry = registry;
        posture.addColumn(Connector::name).setHeader("Connector").setAutoWidth(true);
        posture.addColumn(c -> UiSupport.modeLabel(c.mode())).setHeader("Mode").setAutoWidth(true);
        posture.addColumn(c -> c.configuration() == null || c.configuration().credential() == null ? "none" : c.configuration().credential().value() + " (reference)").setHeader("Credential").setAutoWidth(true);
        posture.addColumn(c -> c.configuration() == null || c.configuration().allowedHosts().isEmpty() ? "none" : String.join(", ", c.configuration().allowedHosts())).setHeader("Allowed hosts").setAutoWidth(true);
        posture.addColumn(c -> c.configuration() == null ? "—" : c.configuration().timeoutMillis() + " ms / " + c.configuration().maxResponseBytes() + " B").setHeader("Timeout / size cap").setAutoWidth(true);
        posture.setHeight("14em");
        trail.addColumn(e -> UiSupport.ts(e.timestamp())).setHeader("When").setAutoWidth(true);
        trail.addColumn(e -> e.userId().value()).setHeader("User").setAutoWidth(true);
        trail.addColumn(AuditLogEntry::action).setHeader("Action").setAutoWidth(true);
        trail.addColumn(AuditLogEntry::targetEntityId).setHeader("Target").setAutoWidth(true);
        trail.addColumn(AuditLogEntry::rationale).setHeader("Rationale").setAutoWidth(true);
        trail.addColumn(e -> e.isSignatureValid() ? "valid" : "INVALID").setHeader("Signature").setAutoWidth(true);
        trail.setSizeFull();
        body(new Button("Refresh", e -> refresh()), new H3("Connector security posture"), posture,
                new Paragraph("Webhooks are authenticated by HMAC-SHA256 over timestamp+body with a 5-minute window and replay suppression; outbound calls are restricted to allow-listed, publicly routable hosts."),
                new H3("Audit trail (integration actions)"), trail);
        refresh();
    }

    private void refresh() {
        posture.setItems(UiSupport.call(() -> registry.list(UiSupport.workspace()), List.<Connector>of()));
        trail.setItems(UiSupport.call(() -> audit.getAuditTrail(UiSupport.workspace()), List.<AuditLogEntry>of()).stream().filter(e -> ACTIONS.contains(e.action())).toList());
    }
}
