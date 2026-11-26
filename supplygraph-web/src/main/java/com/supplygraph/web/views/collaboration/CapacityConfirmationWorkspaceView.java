package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "portal/capacity", layout = MainLayout.class)
@PageTitle("Supplier Capacity Confirmation | ORBIX")
public class CapacityConfirmationWorkspaceView extends CollaborationViewBase {

    private final SupplierPortalService portalService;

    public record CapacityCommitment(String period, String facility, double nominalCapacityHours, double committedHours, double surgeCapacityHours) {}

    public CapacityConfirmationWorkspaceView(SupplierPortalService portalService) {
        super("Supplier Capacity Commitment Workspace", "Supplier production capacity declaration, forward shift availability locking, and bottleneck visibility.");
        this.portalService = portalService;

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Committed Hours (Q1)", "No data", "No capacity confirmations summarized yet", "primary"));
        kpiRow.add(createKpiCard("Peak Available Surge", "No data", "No capacity confirmations summarized yet", "success"));
        kpiRow.add(createKpiCard("Capacity Utilization", "No data", "No capacity confirmations summarized yet", "warning"));
        kpiRow.add(createKpiCard("Committed Suppliers", String.valueOf(partners.size()), "Registered facility plants", "contrast"));

        add(kpiRow);

        add(new H3("Declare Forward Capacity Commitment"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField periodField = new TextField("Planning Period", "2026-M03");
        TextField facField = new TextField("Facility / Cell Line", "CELL-LINE-EAST-01");
        NumberField nominalField = new NumberField("Nominal Hours");
        nominalField.setValue(2000.0);
        NumberField committedField = new NumberField("Committed Hours");
        committedField.setValue(1750.0);
        NumberField surgeField = new NumberField("Surge Buffer Hours");
        surgeField.setValue(350.0);

        List<CapacityCommitment> commitments = new ArrayList<>(List.of(
                new CapacityCommitment("2026-M01", "CELL-LINE-EAST-01", 2000.0, 1800.0, 300.0),
                new CapacityCommitment("2026-M02", "CELL-LINE-EAST-01", 2000.0, 1900.0, 200.0),
                new CapacityCommitment("2026-M03", "CELL-LINE-EAST-01", 2000.0, 1750.0, 350.0)
        ));

        Grid<CapacityCommitment> grid = new Grid<>();

        Button commitBtn = new Button("Commit Capacity", e -> {
            commitments.add(new CapacityCommitment(
                    periodField.getValue(),
                    facField.getValue(),
                    nominalField.getValue(),
                    committedField.getValue(),
                    surgeField.getValue()
            ));
            grid.setItems(commitments);
            Notification.show("Recorded capacity commitment for " + periodField.getValue(), 3000, Notification.Position.TOP_CENTER);
        });
        commitBtn.getElement().getThemeList().add("primary");

        form.add(periodField, facField, nominalField, committedField, surgeField, commitBtn);
        add(form);

        add(new H3("Forward Confirmed Capacity Ledger"));
        grid.addColumn(CapacityCommitment::period).setHeader("Period").setAutoWidth(true);
        grid.addColumn(CapacityCommitment::facility).setHeader("Facility Line").setAutoWidth(true);
        grid.addColumn(c -> c.nominalCapacityHours() + " hrs").setHeader("Nominal Capacity").setAutoWidth(true);
        grid.addColumn(c -> c.committedHours() + " hrs").setHeader("Committed Hours").setAutoWidth(true);
        grid.addColumn(c -> c.surgeCapacityHours() + " hrs").setHeader("Surge Buffer").setAutoWidth(true);
        grid.addComponentColumn(c -> {
            double util = (c.committedHours() / c.nominalCapacityHours()) * 100.0;
            Span badge = new Span(String.format("%.1f%%", util));
            badge.getElement().getThemeList().add("badge small " + (util > 90 ? "error" : util > 75 ? "warning" : "success"));
            return badge;
        }).setHeader("Utilization").setAutoWidth(true);

        grid.setItems(commitments);
        grid.setHeight("350px");

        add(grid);
    }
}

