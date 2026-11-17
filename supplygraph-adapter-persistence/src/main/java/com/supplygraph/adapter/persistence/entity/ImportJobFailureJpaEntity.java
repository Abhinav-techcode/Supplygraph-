package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "import_job_failures")
public class ImportJobFailureJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJobJpaEntity job;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "field_name", length = 64, nullable = false)
    private String fieldName;

    @Column(name = "rejected_value", columnDefinition = "TEXT")
    private String rejectedValue;

    @Column(name = "error_code", length = 64, nullable = false)
    private String errorCode;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "suggested_correction", columnDefinition = "TEXT")
    private String suggestedCorrection;

    public ImportJobFailureJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ImportJobJpaEntity getJob() { return job; }
    public void setJob(ImportJobJpaEntity job) { this.job = job; }

    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(String rejectedValue) { this.rejectedValue = rejectedValue; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSuggestedCorrection() { return suggestedCorrection; }
    public void setSuggestedCorrection(String suggestedCorrection) { this.suggestedCorrection = suggestedCorrection; }
}
