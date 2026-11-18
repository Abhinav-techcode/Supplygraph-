package com.supplygraph.web.views.integration;

import com.supplygraph.application.connector.ConnectorRegistryService;
import com.supplygraph.domain.model.connector.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "connectors", layout = MainLayout.class)
@PageTitle("Connector Registry | SupplyGraph")
public class ConnectorRegistryView extends IntegrationViewBase {
    private final ConnectorRegistryService service;
    private final Grid<Connector> grid = new Grid<>();
    private final Span empty = new Span("No connectors registered yet. Use “Register connector” to add a sandbox (simulated) or live connector.");

    public ConnectorRegistryView(ConnectorRegistryService service) {
        super("Connector Registry", "Registered enterprise connectors, their mode (SIMULATED vs LIVE) and lifecycle state.");
        this.service = service;
        grid.addColumn(c -> c.id().value()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Connector::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Connector::category).setHeader("Category").setAutoWidth(true);
        grid.addColumn(c -> UiSupport.modeLabel(c.mode())).setHeader("Mode").setAutoWidth(true);
        grid.addColumn(Connector::adapterType).setHeader("Adapter").setAutoWidth(true);
        grid.addColumn(Connector::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(c -> UiSupport.ts(c.updatedAt())).setHeader("Updated").setAutoWidth(true);
        grid.addComponentColumn(c -> {
            Button toggle = new Button(c.status() == ConnectorStatus.ACTIVE ? "Deactivate" : "Activate");
            toggle.setEnabled(c.configuration() != null);
            toggle.addClickListener(e -> {
                if (UiSupport.run(c.status() == ConnectorStatus.ACTIVE ? "Connector deactivated" : "Connector activated",
                        () -> { if (c.status() == ConnectorStatus.ACTIVE) service.deactivate(c.id()); else service.activate(c.id()); })) refresh();
            });
            return toggle;
        }).setHeader("Action");
        grid.setSizeFull();
        Button register = new Button("Register connector", e -> openRegisterDialog());
        register.addThemeName("primary");
        body(new HorizontalLayout(register, new Button("Refresh", e -> refresh())), empty, grid);
        refresh();
    }

    private void refresh() {
        var list = UiSupport.call(() -> service.list(UiSupport.workspace()), java.util.List.<Connector>of());
        grid.setItems(list);
        empty.setVisible(list.isEmpty());
        grid.setVisible(!list.isEmpty());
    }

    private void openRegisterDialog() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Register connector");
        TextField name = new TextField("Name");
        ComboBox<ConnectorCategory> category = new ComboBox<>("Category", ConnectorCategory.values());
        ComboBox<ConnectorMode> mode = new ComboBox<>("Mode", ConnectorMode.values());
        mode.setItemLabelGenerator(UiSupport::modeLabel);
        mode.setValue(ConnectorMode.SANDBOX_SIMULATED);
        ComboBox<String> adapter = new ComboBox<>("Adapter", "sandbox", "rest");
        adapter.setValue("sandbox");
        Span hint = new Span("Sandbox connectors are simulated: they never contact an external system.");
        Checkbox activate = new Checkbox("Configure with sandbox defaults and activate immediately (SIMULATED connectors only)", true);
        Button save = new Button("Register", e -> {
            if (name.isEmpty() || category.isEmpty() || mode.isEmpty() || adapter.isEmpty()) { UiSupport.error(new IllegalArgumentException("Name, category, mode and adapter are required")); return; }
            boolean auto = activate.getValue() && mode.getValue() == ConnectorMode.SANDBOX_SIMULATED;
            if (UiSupport.run(auto ? "Connector registered, configured and activated (SIMULATED)" : "Connector registered — configure it, then activate", () -> {
                Connector c = service.register(UiSupport.workspace(), name.getValue().trim(), category.getValue(), mode.getValue(), adapter.getValue());
                if (auto) {
                    c = service.configure(c.id(), new ConnectorConfiguration(null, null, java.util.List.of(), 5000, 1_048_576, 3));
                    service.activate(c.id());
                }
            })) { d.close(); refresh(); }
        });
        save.addThemeName("primary");
        d.add(new com.vaadin.flow.component.orderedlayout.VerticalLayout(name, category, mode, adapter, hint, activate));
        d.getFooter().add(new Button("Cancel", e -> d.close()), save);
        d.open();
    }
}
