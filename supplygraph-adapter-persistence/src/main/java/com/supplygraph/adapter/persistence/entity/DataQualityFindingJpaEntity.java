package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "data_quality_findings")
public class DataQualityFindingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private DataQualityReportJpaEntity report;

    @Column(name = "rule_id", length = 64, nullable = false)
    private String ruleId;

    @Column(name = "dimension", length = 32, nullable = false)
    private String dimension;

    @Column(name = "entity_id", length = 128, nullable = false)
    private String entityId;

    @Column(name = "field_name", length = 64, nullable = false)
    private String fieldName;

    @Column(name = "actual_value", columnDefinition = "TEXT")
    private String actualValue;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "severity", length = 16, nullable = false)
    private String severity;

    @Column(name = "suggested_remediation", columnDefinition = "TEXT")
    private String suggestedRemediation;

    public DataQualityFindingJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DataQualityReportJpaEntity getReport() { return report; }
    public void setReport(DataQualityReportJpaEntity report) { this.report = report; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSuggestedRemediation() { return suggestedRemediation; }
    public void setSuggestedRemediation(String suggestedRemediation) { this.suggestedRemediation = suggestedRemediation; }
}
