package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_quality_incidents_8d")
public class QualityIncident8DJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "supplier_id", length = 64, nullable = false)
    private String supplierId;

    @Column(name = "material_id", length = 64, nullable = false)
    private String materialId;

    @Column(name = "defect_title", length = 255, nullable = false)
    private String defectTitle;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @Column(name = "current_step", length = 64, nullable = false)
    private String currentStep;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    public QualityIncident8DJpaEntity() {}

    public QualityIncident8DJpaEntity(String id, String workspaceId, String supplierId, String materialId, String defectTitle, String severity, String currentStep, boolean closed, Instant reportedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.supplierId = supplierId;
        this.materialId = materialId;
        this.defectTitle = defectTitle;
        this.severity = severity;
        this.currentStep = currentStep;
        this.closed = closed;
        this.reportedAt = reportedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    public String getDefectTitle() { return defectTitle; }
    public void setDefectTitle(String defectTitle) { this.defectTitle = defectTitle; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}

