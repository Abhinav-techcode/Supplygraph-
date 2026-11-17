package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_jobs")
public class ImportJobJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "source_type", length = 32, nullable = false)
    private String sourceType;

    @Column(name = "category", length = 32, nullable = false)
    private String category;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "file_checksum", length = 128)
    private String fileChecksum;

    @Column(name = "schema_version", length = 32)
    private String schemaVersion;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "accepted_records", nullable = false)
    private int acceptedRecords;

    @Column(name = "rejected_records", nullable = false)
    private int rejectedRecords;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "imported_by", length = 64)
    private String importedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ImportJobFailureJpaEntity> failures = new ArrayList<>();

    public ImportJobJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFileChecksum() { return fileChecksum; }
    public void setFileChecksum(String fileChecksum) { this.fileChecksum = fileChecksum; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getAcceptedRecords() { return acceptedRecords; }
    public void setAcceptedRecords(int acceptedRecords) { this.acceptedRecords = acceptedRecords; }

    public int getRejectedRecords() { return rejectedRecords; }
    public void setRejectedRecords(int rejectedRecords) { this.rejectedRecords = rejectedRecords; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public List<ImportJobFailureJpaEntity> getFailures() { return failures; }
    public void setFailures(List<ImportJobFailureJpaEntity> failures) { this.failures = failures; }
}
