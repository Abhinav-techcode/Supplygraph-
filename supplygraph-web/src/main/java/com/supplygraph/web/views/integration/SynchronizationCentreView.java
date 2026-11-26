package com.supplygraph.web.views.integration;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.connector.ConnectorExecutionService;
import com.supplygraph.application.connector.ConnectorRegistryService;
import com.supplygraph.application.connector.SyncReconciliationService;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ConnectorExecution;
import com.supplygraph.domain.model.connector.ExecutionStatus;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "synchronization", layout = MainLayout.class)
@PageTitle("Synchronization Centre | ORBIX")
public class SynchronizationCentreView extends IntegrationViewBase {
    private final ConnectorRegistryService registry;
    private final ConnectorExecutionService executions;
    private final SyncReconciliationService sync;
    private final ComboBox<Connector> selector = new ComboBox<>("Connector");
    private final Paragraph checkpoint = new Paragraph("Select a connector to see its checkpoint.");
    private final Paragraph result = new Paragraph("No synchronization run in this session.");

    public SynchronizationCentreView(ConnectorRegistryService registry, ConnectorExecutionService executions, SyncReconciliationService sync) {
        super("Synchronization Centre", "Trigger a manual synchronization. Incoming values are reconciled under field-authority policy; conflicts are never silently overwritten.");
        this.registry = registry;
        this.executions = executions;
        this.sync = sync;
        selector.setItemLabelGenerator(c -> c.name() + " · " + UiSupport.modeLabel(c.mode()) + " · " + c.status());
        selector.setWidth("32em");
        selector.addValueChangeListener(e -> showCheckpoint());
        Button run = new Button("Synchronize now", e -> {
            Connector c = selector.getValue();
            if (c == null) { UiSupport.error(new ValidationException("Select a connector first")); return; }
            ConnectorExecution x = UiSupport.call(() -> executions.synchronize(c.id()), null);
            if (x == null) return;
            result.setText((x.simulated() ? "[SIMULATED] " : "") + "Execution " + x.id() + " → " + x.status() + " · received " + x.recordsReceived()
                    + ", processed " + x.recordsProcessed() + ", rejected " + x.recordsRejected()
                    + (x.status() == ExecutionStatus.SUCCEEDED ? "" : " · " + x.errorCategory() + ": " + x.errorMessage()));
            showCheckpoint();
        });
        run.addThemeName("primary");
        body(new HorizontalLayout(selector, run), checkpoint, result);
        selector.setItems(UiSupport.call(() -> registry.list(UiSupport.workspace()), List.<Connector>of()));
    }

    private void showCheckpoint() {
        Connector c = selector.getValue();
        if (c == null) return;
        checkpoint.setText(sync.checkpoint(c.id()).map(cp -> "Checkpoint: " + cp.syncCount() + " sync(s), last at " + UiSupport.ts(cp.lastSyncedAt()) + " (next run is incremental)")
                .orElse("No checkpoint yet — the next synchronization is the initial synchronization."));
    }
}
