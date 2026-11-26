package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.ProcurementPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.ProcurementPlan;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning/procurement", layout = MainLayout.class)
@PageTitle("Procurement Planning Centre | ORBIX")
public class ProcurementPlanningCentreView extends PlanningViewBase {

    public ProcurementPlanningCentreView(ProcurementPlanningService procurementService) {
        super("Procurement Planning Centre", "Multi-tier material requirements explosion, minimum order quantities (MOQ), and supplier volume allocations.");

        WorkspaceId ws = currentWorkspaceId();
        List<ProcurementPlan> plans = procurementService.listProcurementPlans(ws);

        HorizontalLayout navRow = new HorizontalLayout();
        navRow.add(new Button("Supplier Allocation Comparison", VaadinIcon.SPLIT.create(), e -> getUI().ifPresent(ui -> ui.navigate("planning/procurement/allocation"))));
        add(navRow);

        add(new H3("Strategic Procurement Plans"));
        Grid<ProcurementPlan> grid = new Grid<>();
        grid.addColumn(p -> p.id().value()).setHeader("Plan ID").setAutoWidth(true);
        grid.addColumn(ProcurementPlan::planName).setHeader("Plan Name").setAutoWidth(true);
        grid.addColumn(p -> p.netRequirements().size() + " components").setHeader("Net Requirements").setAutoWidth(true);
        grid.addColumn(p -> p.supplierAllocations().size() + " sourcing lines").setHeader("Supplier Allocations").setAutoWidth(true);
        grid.addColumn(p -> "$" + p.totalProjectedCost()).setHeader("Total Projected Cost").setAutoWidth(true);
        grid.addColumn(p -> p.generatedAt().toString()).setHeader("Generated At").setAutoWidth(true);

        grid.setItems(plans);
        grid.setSizeFull();

        if (plans.isEmpty()) {
            add(new Span("No procurement plans registered. Launch the Supplier Allocation Comparison to formulate a multi-source split."));
        } else {
            add(grid);
        }
    }
}

