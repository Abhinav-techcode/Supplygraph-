package com.supplygraph.application.planning;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.planning.port.out.DemandForecastRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.planning.DemandForecast;
import com.supplygraph.domain.model.planning.DemandForecastId;
import com.supplygraph.domain.model.planning.ForecastAccuracyMetrics;
import com.supplygraph.domain.model.planning.ForecastMethod;
import com.supplygraph.domain.model.planning.HistoricalDemandPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DemandPlanningService {

    private final DemandForecastRepositoryPort forecastRepository;

    public DemandPlanningService(DemandForecastRepositoryPort forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    public DemandForecast generateForecast(
            WorkspaceId workspaceId,
            MaterialId materialId,
            NodeId destinationLocationId,
            ForecastMethod method,
            List<HistoricalDemandPoint> history,
            int horizonPeriods,
            double alpha,
            double beta
    ) {
        if (history == null || history.isEmpty()) {
            throw new ValidationException("Historical demand cannot be empty");
        }
        if (horizonPeriods <= 0) {
            throw new ValidationException("Forecast horizon must be positive");
        }

        List<Double> historicalValues = history.stream().map(HistoricalDemandPoint::quantity).toList();
        List<Double> predictions = new ArrayList<>(horizonPeriods);

        switch (method) {
            case SIMPLE_MOVING_AVERAGE -> {
                int window = Math.min(3, historicalValues.size());
                double sum = 0.0;
                for (int i = historicalValues.size() - window; i < historicalValues.size(); i++) {
                    sum += historicalValues.get(i);
                }
                double sma = sum / window;
                for (int h = 0; h < horizonPeriods; h++) {
                    predictions.add(sma);
                }
            }
            case WEIGHTED_MOVING_AVERAGE -> {
                int window = Math.min(3, historicalValues.size());
                double weightedSum = 0.0;
                double weightTotal = 0.0;
                for (int i = 0; i < window; i++) {
                    double weight = i + 1.0;
                    weightedSum += historicalValues.get(historicalValues.size() - window + i) * weight;
                    weightTotal += weight;
                }
                double wma = weightedSum / weightTotal;
                for (int h = 0; h < horizonPeriods; h++) {
                    predictions.add(wma);
                }
            }
            case EXPONENTIAL_SMOOTHING -> {
                double level = historicalValues.get(0);
                for (int i = 1; i < historicalValues.size(); i++) {
                    level = (alpha * historicalValues.get(i)) + ((1.0 - alpha) * level);
                }
                for (int h = 0; h < horizonPeriods; h++) {
                    predictions.add(level);
                }
            }
            case TREND_ADJUSTED_HOLT_WINTERS -> {
                double level = historicalValues.get(0);
                double trend = historicalValues.size() > 1 ? (historicalValues.get(1) - historicalValues.get(0)) : 0.0;
                for (int i = 1; i < historicalValues.size(); i++) {
                    double actual = historicalValues.get(i);
                    double prevLevel = level;
                    level = (alpha * actual) + ((1.0 - alpha) * (prevLevel + trend));
                    trend = (beta * (level - prevLevel)) + ((1.0 - beta) * trend);
                }
                for (int h = 1; h <= horizonPeriods; h++) {
                    predictions.add(Math.max(0.0, level + (h * trend)));
                }
            }
            case HISTORICAL_BASELINE -> {
                double lastValue = historicalValues.get(historicalValues.size() - 1);
                for (int h = 0; h < horizonPeriods; h++) {
                    predictions.add(lastValue);
                }
            }
        }

        ForecastAccuracyMetrics accuracy = calculateAccuracyMetrics(historicalValues, predictions.get(0));

        DemandForecast forecast = new DemandForecast(
                DemandForecastId.generate(),
                workspaceId,
                materialId,
                destinationLocationId,
                method,
                predictions,
                history,
                accuracy,
                new LinkedHashMap<>(),
                Instant.now()
        );

        forecastRepository.save(forecast);
        return forecast;
    }

    public DemandForecast applyOverride(DemandForecastId forecastId, int periodIndex, double overrideQuantity) {
        DemandForecast existing = forecastRepository.findById(forecastId)
                .orElseThrow(() -> new ValidationException("Forecast not found: " + forecastId.value()));

        Map<Integer, Double> updatedOverrides = new LinkedHashMap<>(existing.periodOverrides());
        updatedOverrides.put(periodIndex, overrideQuantity);

        DemandForecast updated = new DemandForecast(
                existing.id(),
                existing.workspaceId(),
                existing.materialId(),
                existing.destinationLocationId(),
                existing.method(),
                existing.forecastedQuantities(),
                existing.historicalBaseline(),
                existing.accuracyMetrics(),
                updatedOverrides,
                Instant.now()
        );

        forecastRepository.save(updated);
        return updated;
    }

    public Optional<DemandForecast> getForecast(DemandForecastId id) {
        return forecastRepository.findById(id);
    }

    public List<DemandForecast> listForecasts(WorkspaceId workspaceId) {
        return forecastRepository.findByWorkspaceId(workspaceId);
    }

    private ForecastAccuracyMetrics calculateAccuracyMetrics(List<Double> historical, double predicted) {
        if (historical.isEmpty()) {
            return ForecastAccuracyMetrics.zero();
        }

        double sumAbsoluteError = 0.0;
        double sumPercentageError = 0.0;
        double sumSquaredError = 0.0;
        double runningSumError = 0.0;

        for (double actual : historical) {
            double error = actual - predicted;
            sumAbsoluteError += Math.abs(error);
            if (actual > 0) {
                sumPercentageError += Math.abs(error) / actual;
            }
            sumSquaredError += error * error;
            runningSumError += error;
        }

        int n = historical.size();
        double mad = sumAbsoluteError / n;
        double mape = sumPercentageError / n;
        double rmse = Math.sqrt(sumSquaredError / n);
        double trackingSignal = mad > 0 ? (runningSumError / mad) : 0.0;

        return new ForecastAccuracyMetrics(mad, mape, rmse, trackingSignal);
    }
}

