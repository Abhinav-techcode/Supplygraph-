package com.supplygraph.web.views.integration;

import com.supplygraph.application.connector.ConnectorExecutionService;
import com.supplygraph.application.connector.RuntimeOperationsService;
import com.supplygraph.application.connector.RuntimeOperationsService.ConnectorAvailability;
import com.supplygraph.domain.model.connector.ConnectorExecution;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "connector-health", layout = MainLayout.class)
@PageTitle("Connector Health Monitor | SupplyGraph")
public class ConnectorHealthMonitorView extends IntegrationViewBase {
    private final RuntimeOperationsService ops;
    private final ConnectorExecutionService executions;
    private final Grid<ConnectorAvailability> grid = new Grid<>();
    private final Span empty = new Span("No connectors to monitor.");

    public ConnectorHealthMonitorView(RuntimeOperationsService ops, ConnectorExecutionService executions) {
        super("Connector Health Monitor", "Availability derived from the most recent recorded executions of each connector (no synthetic health).");
        this.ops = ops;
        this.executions = executions;
        grid.addColumn(a -> a.connector().name()).setHeader("Connector").setAutoWidth(true);
        grid.addColumn(a -> UiSupport.modeLabel(a.connector().mode())).setHeader("Mode").setAutoWidth(true);
        grid.addColumn(a -> a.connector().status()).setHeader("State").setAutoWidth(true);
        grid.addColumn(a -> a.lastExecution() == null ? "never executed" : a.lastExecution().status() + " (" + a.lastExecution().operation() + ")").setHeader("Last execution").setAutoWidth(true);
        grid.addColumn(a -> a.lastExecution() == null ? "—" : UiSupport.ts(a.lastExecution().startedAt())).setHeader("When").setAutoWidth(true);
        grid.addColumn(a -> a.lastExecution() == null ? "unknown" : a.lastSucceeded() ? "AVAILABLE" : "DEGRADED").setHeader("Availability").setAutoWidth(true);
        grid.addColumn(ConnectorAvailability::recentFailures).setHeader("Failures in last 10").setAutoWidth(true);
        grid.addComponentColumn(a -> {
            Button b = new Button("Test connectivity");
            b.addClickListener(e -> {
                ConnectorExecution r = UiSupport.call(() -> executions.testConnectivity(a.connector().id()), null);
                if (r != null) { UiSupport.run("Test finished: " + r.status() + (r.errorMessage() != null ? " — " + r.errorMessage() : ""), () -> {}); refresh(); }
            });
            return b;
        }).setHeader("Action");
        grid.setSizeFull();
        body(new HorizontalLayout(new Button("Refresh", e -> refresh())), empty, grid);
        refresh();
    }

    private void refresh() {
        List<ConnectorAvailability> rows = UiSupport.call(() -> ops.snapshot(UiSupport.workspace()).connectors(), List.<ConnectorAvailability>of());
        grid.setItems(rows);
        empty.setVisible(rows.isEmpty());
        grid.setVisible(!rows.isEmpty());
    }
}
