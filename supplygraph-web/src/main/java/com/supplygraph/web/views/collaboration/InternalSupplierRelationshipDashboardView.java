package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.collaboration.OrderCollaborationService;
import com.supplygraph.application.collaboration.RfqCollaborationService;
import com.supplygraph.application.collaboration.SupplierPortalService;
import com.supplygraph.application.collaboration.SupplierQualityCollaborationService;
import com.supplygraph.application.sustainability.SupplierSustainabilityService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.PartnerOrganization;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Optional;

@Route(value = "collaboration/suppliers", layout = MainLayout.class)
@PageTitle("Supplier Relationship Management (SRM) | SupplyGraph")
public class InternalSupplierRelationshipDashboardView extends CollaborationViewBase {

    public record SrmPartnerRow(
            String id,
            String name,
            String tier,
            String country,
            double esgScore,
            long activeOrders,
            long openIncidents,
            String relationshipHealth
    ) {}

    public InternalSupplierRelationshipDashboardView(
            SupplierPortalService portalService,
            OrderCollaborationService orderService,
            SupplierQualityCollaborationService qualityService,
            SupplierSustainabilityService esgService
    ) {
        super("Internal Supplier Relationship Management (SRM)", "Executive 360-degree vendor performance scorecard, ESG compliance rating, delivery risk, and quality scorecard.");

        WorkspaceId ws = currentWorkspaceId();
        List<PartnerOrganization> partners = portalService.listPartners(ws);

        List<SrmPartnerRow> rows = partners.stream().map(p -> {
            Optional<SupplierEsgScorecard> esg = esgService.getScorecardForSupplier(ws, new com.supplygraph.domain.model.graph.NodeId(p.id().value()));
            double score = esg.map(SupplierEsgScorecard::compositeEsgScore).orElse(75.0);
            long orders = orderService.listPos(ws).stream().filter(po -> po.supplierId().equals(p.id())).count();
            long incidents = qualityService.listIncidents(ws).stream().filter(i -> i.supplierId().equals(p.id()) && !i.closed()).count();

            String health = incidents > 0 ? "AT RISK" : score >= 80 ? "STRATEGIC PARTNER" : "STABLE";

            return new SrmPartnerRow(
                    p.id().value(),
                    p.name(),
                    p.tier().name(),
                    p.countryCode(),
                    score,
                    orders,
                    incidents,
                    health
            );
        }).toList();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();

        long strategicCount = rows.stream().filter(r -> "STRATEGIC PARTNER".equals(r.relationshipHealth())).count();
        long atRiskCount = rows.stream().filter(r -> "AT RISK".equals(r.relationshipHealth())).count();

        kpiRow.add(createKpiCard("Active Partners", String.valueOf(partners.size()), "Direct SRM ledger", "primary"));
        kpiRow.add(createKpiCard("Strategic Tier Partners", String.valueOf(strategicCount), "High ESG & zero quality alerts", "success"));
        kpiRow.add(createKpiCard("At-Risk Suppliers", String.valueOf(atRiskCount), "Active 8D corrective actions", "error"));
        kpiRow.add(createKpiCard("Average ESG Rating", "No data", "No ESG scorecards summarized here", "contrast"));

        add(kpiRow);

        add(new H3("Supplier 360° Relationship Scorecards"));
        Grid<SrmPartnerRow> grid = new Grid<>();
        grid.addColumn(SrmPartnerRow::id).setHeader("Supplier ID").setAutoWidth(true);
        grid.addColumn(SrmPartnerRow::name).setHeader("Legal Name").setAutoWidth(true);
        grid.addColumn(SrmPartnerRow::tier).setHeader("Tier").setAutoWidth(true);
        grid.addColumn(SrmPartnerRow::country).setHeader("Country").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f", r.esgScore())).setHeader("Composite ESG").setAutoWidth(true);
        grid.addColumn(SrmPartnerRow::activeOrders).setHeader("Active POs").setAutoWidth(true);
        grid.addColumn(SrmPartnerRow::openIncidents).setHeader("Open 8Ds").setAutoWidth(true);
        grid.addComponentColumn(r -> {
            Span badge = new Span(r.relationshipHealth());
            badge.getElement().getThemeList().add("badge small " + ("STRATEGIC PARTNER".equals(r.relationshipHealth()) ? "success" : "AT RISK".equals(r.relationshipHealth()) ? "error" : "primary"));
            return badge;
        }).setHeader("Relationship Health").setAutoWidth(true);

        grid.setItems(rows);
        grid.setHeight("400px");

        add(grid);
    }
}

