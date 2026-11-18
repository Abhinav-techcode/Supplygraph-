package com.supplygraph.web.views.integration;

import com.supplygraph.application.connector.SyncReconciliationService;
import com.supplygraph.domain.model.sync.*;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "reconciliation", layout = MainLayout.class)
@PageTitle("Data Reconciliation Centre | SupplyGraph")
public class DataReconciliationCentreView extends IntegrationViewBase {
    private final SyncReconciliationService sync;
    private final Grid<ReconciliationItem> open = new Grid<>();
    private final Grid<ReconciliationItem> history = new Grid<>();
    private final TextArea rationale = new TextArea("Resolution rationale (required, audited)");
    private final Span empty = new Span("No open conflicts. Conflicts appear when both SupplyGraph and the source system changed a value since the last sync.");

    public DataReconciliationCentreView(SyncReconciliationService sync) {
        super("Data Reconciliation Centre", "Resolve three-way conflicts (external vs internal vs last-synced) and configure field authority.");
        this.sync = sync;
        columns(open);
        open.addComponentColumn(i -> {
            Button accept = new Button("Accept external", e -> resolve(i, true));
            Button keep = new Button("Keep internal", e -> resolve(i, false));
            return new HorizontalLayout(accept, keep);
        }).setHeader("Resolve");
        columns(history);
        history.addColumn(ReconciliationItem::status).setHeader("Outcome").setAutoWidth(true);
        history.addColumn(i -> i.resolvedBy() == null ? "" : i.resolvedBy()).setHeader("By").setAutoWidth(true);
        history.addColumn(i -> i.simulated() ? "SIMULATED" : "LIVE").setHeader("Provenance").setAutoWidth(true);
        rationale.setWidth("40em");
        open.setHeight("16em"); history.setHeight("18em");

        ComboBox<String> entity = new ComboBox<>("Entity type", "PURCHASE_ORDER", "STOCK", "SHIPMENT", "SUPPLIER", "PRODUCTION_LINE", "QUALITY_HOLD");
        TextField field = new TextField("Field (or *)");
        field.setValue("*");
        ComboBox<FieldAuthority> authority = new ComboBox<>("Authority", FieldAuthority.values());
        Button save = new Button("Save override", e -> {
            if (entity.isEmpty() || field.isEmpty() || authority.isEmpty()) { UiSupport.error(new IllegalArgumentException("Entity, field and authority are required")); return; }
            UiSupport.run("Field authority override saved", () -> sync.setPolicy(UiSupport.workspace(), entity.getValue(), field.getValue().trim(), authority.getValue()));
        });
        body(new H3("Open conflicts"), empty, open, rationale, new H3("Field authority override"), new HorizontalLayout(entity, field, authority, save),
                new H3("Reconciliation history"), history);
        refresh();
    }

    private static void columns(Grid<ReconciliationItem> g) {
        g.addColumn(i -> UiSupport.ts(i.createdAt())).setHeader("Detected").setAutoWidth(true);
        g.addColumn(ReconciliationItem::entityType).setHeader("Entity").setAutoWidth(true);
        g.addColumn(ReconciliationItem::entityKey).setHeader("Key").setAutoWidth(true);
        g.addColumn(ReconciliationItem::field).setHeader("Field").setAutoWidth(true);
        g.addColumn(i -> i.externalValue()).setHeader("External").setAutoWidth(true);
        g.addColumn(i -> i.internalValue() == null ? "—" : i.internalValue()).setHeader("Internal").setAutoWidth(true);
        g.addColumn(i -> i.lastSyncedValue() == null ? "—" : i.lastSyncedValue()).setHeader("Last synced").setAutoWidth(true);
    }

    private void resolve(ReconciliationItem item, boolean acceptExternal) {
        if (UiSupport.run(acceptExternal ? "External value accepted" : "Internal value kept", () -> sync.resolve(item.id(), acceptExternal, rationale.getValue()))) {
            rationale.clear();
            refresh();
        }
    }

    private void refresh() {
        List<ReconciliationItem> conflicts = UiSupport.call(() -> sync.openConflicts(UiSupport.workspace()), List.<ReconciliationItem>of());
        open.setItems(conflicts);
        empty.setVisible(conflicts.isEmpty());
        open.setVisible(!conflicts.isEmpty());
        history.setItems(UiSupport.call(() -> sync.history(UiSupport.workspace(), 200), List.<ReconciliationItem>of()));
    }
}
