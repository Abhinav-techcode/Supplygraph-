package com.supplygraph.domain.model.collaboration;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record QualityIncident8D(
        QualityIncident8DId id,
        WorkspaceId workspaceId,
        PartnerOrganizationId supplierId,
        MaterialId materialId,
        String defectTitle,
        IncidentSeverity severity,
        EightDStep currentStep,
        boolean closed,
        Instant reportedAt
) implements Serializable {
    public QualityIncident8D {
        Objects.requireNonNull(id, "8D Incident ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        Objects.requireNonNull(defectTitle, "Defect title cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(currentStep, "Current step cannot be null");
        Objects.requireNonNull(reportedAt, "Reported timestamp cannot be null");
    }

    public QualityIncident8D advanceStep(EightDStep nextStep) {
        boolean isClosed = nextStep == EightDStep.D8_CONGRATULATE_TEAM;
        return new QualityIncident8D(id, workspaceId, supplierId, materialId, defectTitle, severity, nextStep, isClosed, reportedAt);
    }
}

