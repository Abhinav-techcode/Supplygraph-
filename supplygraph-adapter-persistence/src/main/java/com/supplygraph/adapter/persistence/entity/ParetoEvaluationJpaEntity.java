package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pareto_evaluations")
public class ParetoEvaluationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "all_solutions_json", columnDefinition = "TEXT", nullable = false)
    private String allSolutionsJson;

    @Column(name = "frontier_solutions_json", columnDefinition = "TEXT", nullable = false)
    private String frontierSolutionsJson;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected ParetoEvaluationJpaEntity() {}

    public ParetoEvaluationJpaEntity(String id, String disruptionId, String allSolutionsJson, String frontierSolutionsJson, Instant evaluatedAt) {
        this.id = id;
        this.disruptionId = disruptionId;
        this.allSolutionsJson = allSolutionsJson;
        this.frontierSolutionsJson = frontierSolutionsJson;
        this.evaluatedAt = evaluatedAt;
    }

    public String getId() { return id; }
    public String getDisruptionId() { return disruptionId; }
    public String getAllSolutionsJson() { return allSolutionsJson; }
    public String getFrontierSolutionsJson() { return frontierSolutionsJson; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
