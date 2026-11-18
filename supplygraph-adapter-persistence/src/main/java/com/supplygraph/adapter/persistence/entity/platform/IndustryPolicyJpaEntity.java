package com.supplygraph.adapter.persistence.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_industry_policies")
public class IndustryPolicyJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "industry_code", length = 64, nullable = false)
    private String industryCode;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "policy_json", columnDefinition = "TEXT", nullable = false)
    private String policyJson;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IndustryPolicyJpaEntity() {}

    public IndustryPolicyJpaEntity(String id, String workspaceId, String industryCode, int versionNumber, String policyJson, String status, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.industryCode = industryCode;
        this.versionNumber = versionNumber;
        this.policyJson = policyJson;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getIndustryCode() { return industryCode; }
    public void setIndustryCode(String industryCode) { this.industryCode = industryCode; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getPolicyJson() { return policyJson; }
    public void setPolicyJson(String policyJson) { this.policyJson = policyJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

