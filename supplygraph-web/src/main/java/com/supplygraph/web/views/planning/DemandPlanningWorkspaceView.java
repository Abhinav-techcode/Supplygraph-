package com.supplygraph.web.views.planning;

import com.supplygraph.application.planning.DemandPlanningService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@Route(value = "planning/demand", layout = MainLayout.class)
@PageTitle("Demand Planning Workspace | SupplyGraph")
public class DemandPlanningWorkspaceView extends PlanningViewBase {

    private final DemandPlanningService demandService;
    private final VerticalLayout resultsLayout = new VerticalLayout();

    public DemandPlanningWorkspaceView(DemandPlanningService demandService) {
        super("Demand Planning Workspace", "Generate statistical demand forecasts using SMA, WMA, Single Exponential Smoothing, and Holt-Winters trend adjustments.");
        this.demandService = demandService;

        WorkspaceId ws = currentWorkspaceId();

        HorizontalLayout form = new HorizontalLayout();
        form.setWidthFull();
        form.setAlignItems(Alignment.BASELINE);

        TextField materialField = new TextField("Material ID");
        materialField.setValue("MAT-BP-4680");

        TextField plantField = new TextField("Plant / Location ID");
        plantField.setValue("PLANT-BERLIN");

        ComboBox<ForecastMethod> methodSelect = new ComboBox<>("Forecast Method", ForecastMethod.values());
        methodSelect.setItemLabelGenerator(ForecastMethod::getDisplayName);
        methodSelect.setValue(ForecastMethod.TREND_ADJUSTED_HOLT_WINTERS);

        IntegerField horizonField = new IntegerField("Horizon (Periods)");
        horizonField.setValue(6);
        horizonField.setMin(1);

        NumberField alphaField = new NumberField("Alpha (Level)");
        alphaField.setValue(0.3);

        NumberField betaField = new NumberField("Beta (Trend)");
        betaField.setValue(0.1);

        Button generateBtn = new Button("Generate Forecast", e -> {
            try {
                List<HistoricalDemandPoint> sampleHistory = List.of(
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(6), 180.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(5), 195.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(4), 210.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(3), 225.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(2), 240.0),
                        new HistoricalDemandPoint(LocalDate.now().minusMonths(1), 260.0)
                );

                DemandForecast forecast = demandService.generateForecast(
                        ws,
                        new MaterialId(materialField.getValue()),
                        new NodeId(plantField.getValue()),
                        methodSelect.getValue(),
                        sampleHistory,
                        horizonField.getValue(),
                        alphaField.getValue(),
                        betaField.getValue()
                );

                renderResults(forecast);
                Notification.show("Forecast generated: " + forecast.id().value(), 3000, Notification.Position.TOP_END);
            } catch (Exception ex) {
                Notification.show("Generation error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        generateBtn.getElement().getThemeList().add("primary");

        form.add(materialField, plantField, methodSelect, horizonField, alphaField, betaField, generateBtn);
        add(form, resultsLayout);
    }

    private void renderResults(DemandForecast forecast) {
        resultsLayout.removeAll();
        resultsLayout.add(new H3("Forecast Evaluation: " + forecast.id().value()));

        HorizontalLayout metricsRow = new HorizontalLayout();
        metricsRow.setWidthFull();

        metricsRow.add(metricPill("MAD", String.format("%.2f", forecast.accuracyMetrics().meanAbsoluteDeviation())));
        metricsRow.add(metricPill("MAPE", String.format("%.2f%%", forecast.accuracyMetrics().meanAbsolutePercentageError() * 100)));
        metricsRow.add(metricPill("RMSE", String.format("%.2f", forecast.accuracyMetrics().rootMeanSquareError())));
        metricsRow.add(metricPill("Tracking Signal", String.format("%.2f", forecast.accuracyMetrics().trackingSignal())));
        metricsRow.add(metricPill("Total Volume", String.format("%.0f units", forecast.getTotalForecastedVolume())));

        resultsLayout.add(metricsRow);

        resultsLayout.add(new H3("Period Predictions"));
        HorizontalLayout buckets = new HorizontalLayout();
        for (int i = 0; i < forecast.forecastedQuantities().size(); i++) {
            VerticalLayout b = new VerticalLayout();
            b.addClassNames("border", "p-s", "rounded-s");
            b.add(new Span("Period +" + (i + 1)));
            b.add(new H3(String.format("%.1f", forecast.getEffectiveQuantity(i))));
            buckets.add(b);
        }
        resultsLayout.add(buckets);
    }

    private VerticalLayout metricPill(String title, String val) {
        VerticalLayout v = new VerticalLayout();
        v.addClassNames("border", "p-s", "rounded-m");
        v.add(new Span(title), new H3(val));
        return v;
    }
}

