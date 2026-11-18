package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregated risk and operational dependency profile for a supplier node in the network.
 */
public record SupplierRiskProfile(
        String supplierId,
        String supplierName,
        int tier,
        String country,
        double overallRiskScore,
        List<SupplierIntelligenceMetric> metrics,
        boolean isSingleSourceForCriticalMaterials,
        int downstreamSitesImpacted,
        int alternativeSuppliersCount,
        String qualificationStatus
) implements Serializable {

    public SupplierRiskProfile {
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        supplierName = supplierName != null ? supplierName : supplierId;
        country = country != null ? country : "UNKNOWN";
        qualificationStatus = qualificationStatus != null ? qualificationStatus : "QUALIFIED";
        metrics = metrics != null ? Collections.unmodifiableList(metrics) : Collections.emptyList();
    }

    public Optional<SupplierIntelligenceMetric> getMetric(SupplierMetricType type) {
        return metrics.stream().filter(m -> m.metricType() == type).findFirst();
    }

    public double getMetricValueOrDefault(SupplierMetricType type, double defaultValue) {
        return getMetric(type).map(SupplierIntelligenceMetric::value).orElse(defaultValue);
    }
}

