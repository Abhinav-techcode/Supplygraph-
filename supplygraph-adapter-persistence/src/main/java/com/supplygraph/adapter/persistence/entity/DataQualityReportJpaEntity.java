package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_quality_reports")
public class DataQualityReportJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "overall_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal overallScore;

    @Column(name = "total_entities", nullable = false)
    private int totalEntities;

    @Column(name = "clean_records", nullable = false)
    private int cleanRecords;

    @Column(name = "flawed_records", nullable = false)
    private int flawedRecords;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DataQualityFindingJpaEntity> findings = new ArrayList<>();

    public DataQualityReportJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }

    public int getTotalEntities() { return totalEntities; }
    public void setTotalEntities(int totalEntities) { this.totalEntities = totalEntities; }

    public int getCleanRecords() { return cleanRecords; }
    public void setCleanRecords(int cleanRecords) { this.cleanRecords = cleanRecords; }

    public int getFlawedRecords() { return flawedRecords; }
    public void setFlawedRecords(int flawedRecords) { this.flawedRecords = flawedRecords; }

    public List<DataQualityFindingJpaEntity> getFindings() { return findings; }
    public void setFindings(List<DataQualityFindingJpaEntity> findings) { this.findings = findings; }
}
