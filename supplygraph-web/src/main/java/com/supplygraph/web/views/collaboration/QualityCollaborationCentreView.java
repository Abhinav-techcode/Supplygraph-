package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.collaboration.EightDStep;
import com.supplygraph.domain.model.collaboration.IncidentSeverity;
import com.supplygraph.domain.model.collaboration.PartnerOrganizationId;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "collaboration/quality", layout = MainLayout.class)
@PageTitle("Quality Collaboration & 8D Problem Solving | ORBIX")
public class QualityCollaborationCentreView extends CollaborationViewBase {

    private final SupplierQualityCollaborationService qualityService;

    public QualityCollaborationCentreView(SupplierQualityCollaborationService qualityService) {
        super("Quality Collaboration & 8D Problem Solving", "Global 8D quality incident investigation, Ishikawa root-cause drilldowns, containment, and permanent corrective action (PCA).");
        this.qualityService = qualityService;

        WorkspaceId ws = currentWorkspaceId();
        List<QualityIncident8D> incidents = qualityService.listIncidents(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        long openCount = incidents.stream().filter(i -> !i.closed()).count();
        long critCount = incidents.stream().filter(i -> i.severity() == IncidentSeverity.CRITICAL && !i.closed()).count();
        long closedCount = incidents.stream().filter(QualityIncident8D::closed).count();

        kpiRow.add(createKpiCard("Active 8D Investigations", String.valueOf(openCount), "Open corrective actions", "warning"));
        kpiRow.add(createKpiCard("Critical Severity", String.valueOf(critCount), "Immediate containment required", "error"));
        kpiRow.add(createKpiCard("Resolved & Closed 8Ds", String.valueOf(closedCount), "Permanent action verified", "success"));
        kpiRow.add(createKpiCard("Total Incidents Logged", String.valueOf(incidents.size()), "Cumulative quality ledger", "contrast"));

        add(kpiRow);

        add(new H3("Open New 8D Quality Investigation"));
        HorizontalLayout form = new HorizontalLayout();
        form.setAlignItems(Alignment.END);

        TextField supplierField = new TextField("Supplier ID", "PARTNER-CATL-01");
        TextField matField = new TextField("Material ID", "MAT-SEPARATOR-PE");
        TextField defectField = new TextField("Defect Description", "Tensile elongation failure under thermal stress");
        defectField.setWidth("350px");
        ComboBox<IncidentSeverity> sevBox = new ComboBox<>("Severity");
        sevBox.setItems(IncidentSeverity.values());
        sevBox.setValue(IncidentSeverity.HIGH);

        Grid<QualityIncident8D> grid = new Grid<>();

        Button openBtn = new Button("Open 8D Ticket", e -> {
            if (supplierField.getValue() == null || matField.getValue() == null || defectField.getValue() == null || sevBox.getValue() == null) {
                Notification.show("Please enter all required incident attributes", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            QualityIncident8D inc = qualityService.openIncident(
                    ws,
                    new PartnerOrganizationId(supplierField.getValue()),
                    new MaterialId(matField.getValue()),
                    defectField.getValue(),
                    sevBox.getValue()
            );
            Notification.show("Opened 8D Incident: " + inc.id().value(), 3000, Notification.Position.TOP_CENTER);
            grid.setItems(qualityService.listIncidents(ws));
        });
        openBtn.getElement().getThemeList().add("primary");

        form.add(supplierField, matField, defectField, sevBox, openBtn);
        add(form);

        add(new H3("Active 8D Quality Incidents"));
        grid.addColumn(i -> i.id().value()).setHeader("8D ID").setAutoWidth(true);
        grid.addColumn(i -> i.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        grid.addColumn(i -> i.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(QualityIncident8D::defectTitle).setHeader("Defect Title").setAutoWidth(true);
        grid.addComponentColumn(i -> {
            Span badge = new Span(i.severity().name());
            badge.getElement().getThemeList().add("badge small " + (i.severity() == IncidentSeverity.CRITICAL ? "error" : i.severity() == IncidentSeverity.HIGH ? "warning" : "primary"));
            return badge;
        }).setHeader("Severity").setAutoWidth(true);
        grid.addComponentColumn(i -> {
            Span badge = new Span(i.currentStep().name());
            badge.getElement().getThemeList().add("badge small " + (i.closed() ? "success" : "contrast"));
            return badge;
        }).setHeader("Current 8D Stage").setAutoWidth(true);
        grid.addColumn(i -> i.reportedAt().toString()).setHeader("Reported At").setAutoWidth(true);

        ComboBox<EightDStep> stepBox = new ComboBox<>("Advance to 8D Step");
        stepBox.setItems(EightDStep.values());
        stepBox.setValue(EightDStep.D3_CONTAINMENT_ACTIONS);

        Button advanceBtn = new Button("Advance 8D Stage", e -> {
            QualityIncident8D selected = grid.asSingleSelect().getValue();
            if (selected == null || stepBox.getValue() == null) {
                Notification.show("Please select an incident and target step", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            QualityIncident8D adv = qualityService.advanceStep(ws, selected.id(), stepBox.getValue());
            Notification.show("Advanced " + adv.id().value() + " to " + adv.currentStep().name(), 3000, Notification.Position.TOP_CENTER);
            grid.setItems(qualityService.listIncidents(ws));
        });
        advanceBtn.getElement().getThemeList().add("success");

        HorizontalLayout actionRow = new HorizontalLayout(stepBox, advanceBtn);
        actionRow.setAlignItems(Alignment.END);

        grid.setItems(incidents);
        grid.setHeight("350px");

        add(grid, actionRow);
    }
}

