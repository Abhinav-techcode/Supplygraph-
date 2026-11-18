package com.supplygraph.web.views.workspace;

import com.supplygraph.application.collaboration.PartnerDocumentExchangeService;
import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.application.sustainability.ProductTraceabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.IncidentSeverity;
import com.supplygraph.domain.model.collaboration.QualityIncident8D;
import com.supplygraph.domain.model.sustainability.GenealogyDAG;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "workspace/quality", layout = MainLayout.class)
@PageTitle("Quality Manager Workspace | SupplyGraph")
public class QualityManagerWorkspaceView extends WorkspaceViewBase {

    private final SupplierQualityCollaborationService qualityService;
    private final ProductTraceabilityService traceabilityService;
    private final PartnerDocumentExchangeService docService;

    public QualityManagerWorkspaceView(
            SupplierQualityCollaborationService qualityService,
            ProductTraceabilityService traceabilityService,
            PartnerDocumentExchangeService docService
    ) {
        super("Quality & Product Integrity Cockpit", "Global 8D non-conformance management, multi-tier lot genealogy provenance, and regulatory batch recall readiness.", "Role :: Quality & Compliance Director");
        this.qualityService = qualityService;
        this.traceabilityService = traceabilityService;
        this.docService = docService;

        WorkspaceId ws = currentWorkspaceId();
        List<QualityIncident8D> incidents = qualityService.listIncidents(ws);
        GenealogyDAG dag = traceabilityService.getOrCreateDag(ws);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        kpiRow.add(createKpiCard("Active 8D Incidents", String.valueOf(incidents.stream().filter(i -> !i.closed()).count()), "Containment & root-cause", "error"));
        kpiRow.add(createKpiCard("Genealogy Lots", String.valueOf(dag.getAllNodes().size()), "Tracked batch artifacts", "primary"));
        kpiRow.add(createKpiCard("Quality Certificates (CoAs)", String.valueOf(docService.listDocuments(ws).size()), "Vaulted test reports", "success"));
        kpiRow.add(createKpiCard("PPM Defect Rate", "No data", "No defect-rate source connected", "contrast"));

        add(kpiRow);

        add(new H3("8D Problem Solving Action Ledger"));
        Grid<QualityIncident8D> grid = new Grid<>();
        grid.addColumn(i -> i.id().value()).setHeader("Ticket #").setAutoWidth(true);
        grid.addColumn(i -> i.supplierId().value()).setHeader("Supplier").setAutoWidth(true);
        grid.addColumn(i -> i.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(QualityIncident8D::defectTitle).setHeader("Defect Title").setAutoWidth(true);
        grid.addColumn(i -> i.severity().name()).setHeader("Severity").setAutoWidth(true);
        grid.addColumn(i -> i.currentStep().name()).setHeader("8D Stage").setAutoWidth(true);
        grid.addColumn(i -> i.reportedAt().toString()).setHeader("Reported").setAutoWidth(true);

        grid.setItems(incidents);
        grid.setHeight("350px");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.add(new Button("8D Quality Workspace", VaadinIcon.DIPLOMA.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/quality"))));
        actionRow.add(new Button("Recall Impact Blast Radius", VaadinIcon.WARNING.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/traceability/recall"))));
        actionRow.add(new Button("Product Genealogy DAG", VaadinIcon.CONNECT_O.create(), e -> getUI().ifPresent(ui -> ui.navigate("sustainability/traceability/genealogy"))));
        actionRow.add(new Button("Document Vault", VaadinIcon.FILE_TEXT.create(), e -> getUI().ifPresent(ui -> ui.navigate("collaboration/documents"))));

        add(grid, actionRow);
    }
}

