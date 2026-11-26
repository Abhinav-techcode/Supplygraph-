package com.supplygraph.web.views.integration;

import com.supplygraph.application.connector.ConnectorRegistryService;
import com.supplygraph.application.connector.RuntimeOperationsService;
import com.supplygraph.application.connector.SyncReconciliationService;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ConnectorMode;
import com.supplygraph.domain.model.connector.ConnectorStatus;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "integration-centre", layout = MainLayout.class)
@PageTitle("Enterprise Integration Centre | ORBIX")
public class EnterpriseIntegrationCentreView extends IntegrationViewBase {
    public EnterpriseIntegrationCentreView(ConnectorRegistryService registry, RuntimeOperationsService ops, SyncReconciliationService sync) {
        super("Enterprise Integration Centre", "Entry point to the connector runtime. Counts below come from persisted data.");
        List<Connector> connectors = UiSupport.call(() -> registry.list(UiSupport.workspace()), List.<Connector>of());
        long active = connectors.stream().filter(c -> c.status() == ConnectorStatus.ACTIVE).count();
        long simulated = connectors.stream().filter(c -> c.mode() == ConnectorMode.SANDBOX_SIMULATED).count();
        var snap = UiSupport.call(() -> ops.snapshot(UiSupport.workspace()), null);
        long dead = snap == null ? 0 : snap.outboxCounts().getOrDefault(OutboxStatus.DEAD_LETTER, 0L);
        int conflicts = UiSupport.call(() -> sync.openConflicts(UiSupport.workspace()), List.of()).size();
        body(new Paragraph("Connectors: " + connectors.size() + " (" + active + " active, " + simulated + " simulated sandbox, " + (connectors.size() - simulated) + " live)"),
                new Paragraph("Dead-lettered events: " + dead + " · Open reconciliation conflicts: " + conflicts),
                new Paragraph("Honesty note: only the generic REST adapter (verified against a local test server) and the SIMULATED sandbox adapter exist. No vendor ERP/PLM/WMS/TMS adapter has been implemented or verified."));
        String[][] links = {{"connectors", "Connector Registry"}, {"connector-config", "Configuration"}, {"connector-health", "Health Monitor"},
                {"synchronization", "Synchronization Centre"}, {"sync-history", "Synchronization History"}, {"event-delivery", "Event Delivery"},
                {"dead-letters", "Dead Letters"}, {"event-replay", "Event Replay"}, {"reconciliation", "Reconciliation"},
                {"integration-security", "Security & Audit"}, {"runtime-operations", "Runtime Operations"}};
        HorizontalLayout row = new HorizontalLayout();
        row.getStyle().set("flex-wrap", "wrap");
        for (String[] l : links) row.add(new com.vaadin.flow.component.html.Anchor(l[0], l[1]));
        body(row);
    }
}
