package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.DemandPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "planning/demand/detail", layout = MainLayout.class)
@PageTitle("Forecast Detail Explorer | ORBIX")
public class ForecastDetailExplorerView extends PlanningViewBase {

    private final DemandPlanningService demandService;
    private final VerticalLayout detailsContainer = new VerticalLayout();

    public ForecastDetailExplorerView(DemandPlanningService demandService) {
        super("Forecast Detail Explorer", "In-depth period analysis and collaborative override adjustment for statistical forecasts.");
        this.demandService = demandService;

        WorkspaceId ws = currentWorkspaceId();
        List<DemandForecast> forecasts = demandService.listForecasts(ws);

        ComboBox<DemandForecast> forecastSelect = new ComboBox<>("Select Forecast", forecasts);
        forecastSelect.setItemLabelGenerator(f -> f.id().value() + " - " + f.materialId().value() + " (" + f.method().name() + ")");
        forecastSelect.setWidth("450px");

        forecastSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                renderDetail(e.getValue());
            }
        });

        if (!forecasts.isEmpty()) {
            forecastSelect.setValue(forecasts.get(0));
        }

        add(forecastSelect, detailsContainer);
    }

    private void renderDetail(DemandForecast forecast) {
        detailsContainer.removeAll();

        record PeriodRow(int periodIndex, double statisticalForecast, Double override, double effective) {}
        List<PeriodRow> rows = new ArrayList<>();
        for (int i = 0; i < forecast.forecastedQuantities().size(); i++) {
            double stat = forecast.forecastedQuantities().get(i);
            Double ov = forecast.periodOverrides().get(i);
            double eff = forecast.getEffectiveQuantity(i);
            rows.add(new PeriodRow(i + 1, stat, ov, eff));
        }

        Grid<PeriodRow> grid = new Grid<>();
        grid.addColumn(PeriodRow::periodIndex).setHeader("Period #").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f", r.statisticalForecast())).setHeader("Statistical Baseline").setAutoWidth(true);
        grid.addColumn(r -> r.override() != null ? String.format("%.1f", r.override()) : "—").setHeader("Collaborative Override").setAutoWidth(true);
        grid.addColumn(r -> String.format("%.1f", r.effective())).setHeader("Final Effective Demand").setAutoWidth(true);

        grid.setItems(rows);
        grid.setHeight("250px");

        detailsContainer.add(new H3("Period Demand Distribution"), grid);

        HorizontalLayout overrideForm = new HorizontalLayout();
        overrideForm.setAlignItems(Alignment.BASELINE);

        IntegerField periodField = new IntegerField("Period Index (1-based)");
        periodField.setValue(1);
        periodField.setMin(1);
        periodField.setMax(forecast.forecastedQuantities().size());

        NumberField overrideQtyField = new NumberField("New Override Quantity");
        overrideQtyField.setValue(forecast.getEffectiveQuantity(0) * 1.1);

        Button applyBtn = new Button("Apply Override", e -> {
            try {
                DemandForecast updated = demandService.applyOverride(
                        forecast.id(),
                        periodField.getValue() - 1,
                        overrideQtyField.getValue()
                );
                renderDetail(updated);
                Notification.show("Collaborative override applied to period " + periodField.getValue(), 3000, Notification.Position.TOP_END);
            } catch (Exception ex) {
                Notification.show("Override failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        applyBtn.getElement().getThemeList().add("primary");

        overrideForm.add(periodField, overrideQtyField, applyBtn);
        detailsContainer.add(new H3("Collaborative Demand Adjustment"), overrideForm);
    }
}

