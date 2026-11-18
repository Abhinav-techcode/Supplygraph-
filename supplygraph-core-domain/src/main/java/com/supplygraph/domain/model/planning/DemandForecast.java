package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Demand forecast aggregate holding multi-period forecast buckets, accuracy statistics, and collaborative overrides.
 */
public record DemandForecast(
        DemandForecastId id,
        WorkspaceId workspaceId,
        MaterialId materialId,
        NodeId destinationLocationId,
        ForecastMethod method,
        List<Double> forecastedQuantities,
        List<HistoricalDemandPoint> historicalBaseline,
        ForecastAccuracyMetrics accuracyMetrics,
        Map<Integer, Double> periodOverrides,
        Instant generatedAt
) implements Serializable {
    public DemandForecast {
        Objects.requireNonNull(id, "Forecast id cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace id cannot be null");
        Objects.requireNonNull(materialId, "Material id cannot be null");
        Objects.requireNonNull(destinationLocationId, "Destination location cannot be null");
        Objects.requireNonNull(method, "Forecast method cannot be null");
        forecastedQuantities = List.copyOf(forecastedQuantities);
        historicalBaseline = List.copyOf(historicalBaseline);
        Objects.requireNonNull(accuracyMetrics, "Accuracy metrics cannot be null");
        periodOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(periodOverrides));
        Objects.requireNonNull(generatedAt, "Generation timestamp cannot be null");
    }

    public double getEffectiveQuantity(int periodIndex) {
        if (periodIndex < 0 || periodIndex >= forecastedQuantities.size()) {
            throw new IndexOutOfBoundsException("Invalid forecast period: " + periodIndex);
        }
        if (periodOverrides.containsKey(periodIndex)) {
            return periodOverrides.get(periodIndex);
        }
        return forecastedQuantities.get(periodIndex);
    }

    public double getTotalForecastedVolume() {
        double total = 0.0;
        for (int i = 0; i < forecastedQuantities.size(); i++) {
            total += getEffectiveQuantity(i);
        }
        return total;
    }
}
