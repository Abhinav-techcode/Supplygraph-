package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Structural evaluation of supply chain network topology, vulnerabilities, and shock-absorption capacity.
 */
public record NetworkResilienceAssessment(
        ResilienceAssessmentId id,
        String workspaceId,
        Instant assessedAt,
        double overallResilienceIndex,
        int singlePointsOfFailureCount,
        double supplierConcentrationHHI,
        int criticalPathDepth,
        double alternativeSupplierCoverageRatio,
        int recoveryBottlenecksCount,
        List<ResilienceFactor> factorScores,
        List<String> criticalVulnerabilities,
        List<String> recommendedMitigations
) implements Serializable {

    public NetworkResilienceAssessment {
        Objects.requireNonNull(id, "ResilienceAssessmentId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        assessedAt = assessedAt != null ? assessedAt : Instant.now();
        factorScores = factorScores != null ? Collections.unmodifiableList(factorScores) : Collections.emptyList();
        criticalVulnerabilities = criticalVulnerabilities != null ? Collections.unmodifiableList(criticalVulnerabilities) : Collections.emptyList();
        recommendedMitigations = recommendedMitigations != null ? Collections.unmodifiableList(recommendedMitigations) : Collections.emptyList();
    }

    public static NetworkResilienceAssessment of(
            String workspaceId,
            double overallResilienceIndex,
            int spofCount,
            double hhi,
            int maxDepth,
            double altRatio,
            int bottlenecks,
            List<ResilienceFactor> factors,
            List<String> vulnerabilities,
            List<String> mitigations
    ) {
        return new NetworkResilienceAssessment(
                ResilienceAssessmentId.generate(),
                workspaceId,
                Instant.now(),
                Math.max(0.0, Math.min(100.0, overallResilienceIndex)),
                spofCount,
                hhi,
                maxDepth,
                Math.max(0.0, Math.min(1.0, altRatio)),
                bottlenecks,
                factors,
                vulnerabilities,
                mitigations
        );
    }
}
