package com.supplygraph.domain.model.integration;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the execution state, audit metadata, and metrics of an enterprise data import batch.
 */
public final class ImportJob implements Serializable {

    private final ImportJobId id;
    private final String workspaceId;
    private final ImportSourceType sourceType;
    private final DataCategory category;
    private final String filename;
    private final String fileChecksum;
    private final String schemaVersion;
    private int totalRecords;
    private int acceptedRecords;
    private int rejectedRecords;
    private final List<ImportRecordFailure> failures;
    private ImportStatus status;
    private final String importedBy;
    private final Instant createdAt;
    private Instant completedAt;

    public ImportJob(
            ImportJobId id,
            String workspaceId,
            ImportSourceType sourceType,
            DataCategory category,
            String filename,
            String fileChecksum,
            String schemaVersion,
            int totalRecords,
            int acceptedRecords,
            int rejectedRecords,
            List<ImportRecordFailure> failures,
            ImportStatus status,
            String importedBy,
            Instant createdAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id, "ImportJobId cannot be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        this.sourceType = Objects.requireNonNull(sourceType, "ImportSourceType cannot be null");
        this.category = Objects.requireNonNull(category, "DataCategory cannot be null");
        this.filename = filename != null ? filename : "direct_input";
        this.fileChecksum = fileChecksum != null ? fileChecksum : "UNSPECIFIED";
        this.schemaVersion = schemaVersion != null ? schemaVersion : "1.0";
        this.totalRecords = totalRecords;
        this.acceptedRecords = acceptedRecords;
        this.rejectedRecords = rejectedRecords;
        this.failures = failures != null ? new ArrayList<>(failures) : new ArrayList<>();
        this.status = Objects.requireNonNull(status, "ImportStatus cannot be null");
        this.importedBy = importedBy != null ? importedBy : "SYSTEM";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.completedAt = completedAt;
    }

    public static ImportJob createNew(
            String workspaceId,
            ImportSourceType sourceType,
            DataCategory category,
            String filename,
            String fileChecksum,
            String importedBy
    ) {
        return new ImportJob(
                ImportJobId.generate(),
                workspaceId,
                sourceType,
                category,
                filename,
                fileChecksum,
                "1.0",
                0,
                0,
                0,
                new ArrayList<>(),
                ImportStatus.PENDING_VALIDATION,
                importedBy,
                Instant.now(),
                null
        );
    }

    public void markValidated(int total, int accepted, int rejected, List<ImportRecordFailure> recordFailures) {
        this.totalRecords = total;
        this.acceptedRecords = accepted;
        this.rejectedRecords = rejected;
        this.failures.clear();
        if (recordFailures != null) {
            this.failures.addAll(recordFailures);
        }
        if (rejected == 0) {
            this.status = ImportStatus.VALIDATED;
        } else if (accepted > 0) {
            this.status = ImportStatus.VALIDATED;
        } else {
            this.status = ImportStatus.REJECTED;
            this.completedAt = Instant.now();
        }
    }

    public void markInProgress() {
        this.status = ImportStatus.IN_PROGRESS;
    }

    public void markCompleted(int finalAccepted, int finalRejected) {
        this.acceptedRecords = finalAccepted;
        this.rejectedRecords = finalRejected;
        if (finalRejected > 0 && finalAccepted > 0) {
            this.status = ImportStatus.PARTIALLY_COMPLETED;
        } else if (finalAccepted > 0) {
            this.status = ImportStatus.COMPLETED;
        } else {
            this.status = ImportStatus.REJECTED;
        }
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = ImportStatus.FAILED;
        this.failures.add(new ImportRecordFailure(0, "SYSTEM", "N/A", "EXECUTION_FAILURE", reason, "Check system logs and retry"));
        this.completedAt = Instant.now();
    }

    public ImportJobId id() {
        return id;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public ImportSourceType sourceType() {
        return sourceType;
    }

    public DataCategory category() {
        return category;
    }

    public String filename() {
        return filename;
    }

    public String fileChecksum() {
        return fileChecksum;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public int totalRecords() {
        return totalRecords;
    }

    public int acceptedRecords() {
        return acceptedRecords;
    }

    public int rejectedRecords() {
        return rejectedRecords;
    }

    public List<ImportRecordFailure> failures() {
        return Collections.unmodifiableList(failures);
    }

    public ImportStatus status() {
        return status;
    }

    public String importedBy() {
        return importedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}

