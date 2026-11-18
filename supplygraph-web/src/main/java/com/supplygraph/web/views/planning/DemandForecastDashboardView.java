package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.DemandPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "planning/demand/forecasts", layout = MainLayout.class)
@PageTitle("Demand Forecast Dashboard | SupplyGraph")
public class DemandForecastDashboardView extends PlanningViewBase {

    public DemandForecastDashboardView(DemandPlanningService demandService) {
        super("Demand Forecast Dashboard", "Aggregated repository of demand forecasts across products and network fulfillment nodes.");

        WorkspaceId ws = currentWorkspaceId();
        List<DemandForecast> forecasts = demandService.listForecasts(ws);

        Grid<DemandForecast> grid = new Grid<>();
        grid.addColumn(f -> f.id().value()).setHeader("Forecast ID").setAutoWidth(true);
        grid.addColumn(f -> f.materialId().value()).setHeader("Material").setAutoWidth(true);
        grid.addColumn(f -> f.destinationLocationId().value()).setHeader("Destination").setAutoWidth(true);
        grid.addColumn(f -> f.method().getDisplayName()).setHeader("Method").setAutoWidth(true);
        grid.addColumn(f -> String.format("%.0f units", f.getTotalForecastedVolume())).setHeader("Forecast Volume").setAutoWidth(true);
        grid.addColumn(f -> String.format("%.2f%%", f.accuracyMetrics().meanAbsolutePercentageError() * 100)).setHeader("MAPE").setAutoWidth(true);
        grid.addColumn(f -> f.periodOverrides().size() + " overrides").setHeader("Collaborative Overrides").setAutoWidth(true);
        grid.addColumn(f -> f.generatedAt().toString()).setHeader("Generated At").setAutoWidth(true);

        grid.setItems(forecasts);
        grid.setSizeFull();

        if (forecasts.isEmpty()) {
            add(new Span("No forecasts generated yet. Use the Demand Planning Workspace to run statistical models."));
        } else {
            add(grid);
        }
    }
}

