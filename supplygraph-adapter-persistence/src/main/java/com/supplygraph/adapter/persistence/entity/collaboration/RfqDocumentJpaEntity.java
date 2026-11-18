package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sg_rfq_documents")
public class RfqDocumentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "target_material_id", length = 64, nullable = false)
    private String targetMaterialId;

    @Column(name = "target_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal targetQuantity;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDate submissionDeadline;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "quotations_json", columnDefinition = "TEXT", nullable = false)
    private String quotationsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RfqDocumentJpaEntity() {}

    public RfqDocumentJpaEntity(String id, String workspaceId, String title, String targetMaterialId, BigDecimal targetQuantity, LocalDate submissionDeadline, String status, String quotationsJson, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.targetMaterialId = targetMaterialId;
        this.targetQuantity = targetQuantity;
        this.submissionDeadline = submissionDeadline;
        this.status = status;
        this.quotationsJson = quotationsJson;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTargetMaterialId() { return targetMaterialId; }
    public void setTargetMaterialId(String targetMaterialId) { this.targetMaterialId = targetMaterialId; }

    public BigDecimal getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(BigDecimal targetQuantity) { this.targetQuantity = targetQuantity; }

    public LocalDate getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(LocalDate submissionDeadline) { this.submissionDeadline = submissionDeadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQuotationsJson() { return quotationsJson; }
    public void setQuotationsJson(String quotationsJson) { this.quotationsJson = quotationsJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

