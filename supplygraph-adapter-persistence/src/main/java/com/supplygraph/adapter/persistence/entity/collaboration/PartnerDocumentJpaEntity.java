package com.supplygraph.adapter.persistence.entity.collaboration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_partner_documents")
public class PartnerDocumentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "partner_id", length = 64, nullable = false)
    private String partnerId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "category", length = 64, nullable = false)
    private String category;

    @Column(name = "cryptographic_hash", length = 128, nullable = false)
    private String cryptographicHash;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public PartnerDocumentJpaEntity() {}

    public PartnerDocumentJpaEntity(String id, String workspaceId, String partnerId, String fileName, String category, String cryptographicHash, boolean verified, Instant uploadedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.partnerId = partnerId;
        this.fileName = fileName;
        this.category = category;
        this.cryptographicHash = cryptographicHash;
        this.verified = verified;
        this.uploadedAt = uploadedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCryptographicHash() { return cryptographicHash; }
    public void setCryptographicHash(String cryptographicHash) { this.cryptographicHash = cryptographicHash; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}

