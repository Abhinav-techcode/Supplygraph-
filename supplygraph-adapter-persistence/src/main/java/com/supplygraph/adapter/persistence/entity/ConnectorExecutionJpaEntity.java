package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "connector_executions")
public class ConnectorExecutionJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "connector_id", length = 64, nullable = false) private String connectorId;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(length = 32, nullable = false) private String operation;
    @Column(name = "trigger_type", length = 16, nullable = false) private String triggerType;
    @Column(length = 24, nullable = false) private String status;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(name = "records_received", nullable = false) private int recordsReceived;
    @Column(name = "records_processed", nullable = false) private int recordsProcessed;
    @Column(name = "records_rejected", nullable = false) private int recordsRejected;
    @Column(name = "error_category", length = 32) private String errorCategory;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(nullable = false) private int attempt;
    @Column(name = "retry_of", length = 64) private String retryOf;
    @Column(name = "correlation_id", length = 64, nullable = false) private String correlationId;
    @Column(nullable = false) private boolean simulated;
    @Column(name = "requested_by", length = 64) private String requestedBy;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getConnectorId() { return connectorId; } public void setConnectorId(String v) { connectorId = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getOperation() { return operation; } public void setOperation(String v) { operation = v; }
    public String getTriggerType() { return triggerType; } public void setTriggerType(String v) { triggerType = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant v) { startedAt = v; }
    public Instant getFinishedAt() { return finishedAt; } public void setFinishedAt(Instant v) { finishedAt = v; }
    public int getRecordsReceived() { return recordsReceived; } public void setRecordsReceived(int v) { recordsReceived = v; }
    public int getRecordsProcessed() { return recordsProcessed; } public void setRecordsProcessed(int v) { recordsProcessed = v; }
    public int getRecordsRejected() { return recordsRejected; } public void setRecordsRejected(int v) { recordsRejected = v; }
    public String getErrorCategory() { return errorCategory; } public void setErrorCategory(String v) { errorCategory = v; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { errorMessage = v; }
    public int getAttempt() { return attempt; } public void setAttempt(int v) { attempt = v; }
    public String getRetryOf() { return retryOf; } public void setRetryOf(String v) { retryOf = v; }
    public String getCorrelationId() { return correlationId; } public void setCorrelationId(String v) { correlationId = v; }
    public boolean isSimulated() { return simulated; } public void setSimulated(boolean v) { simulated = v; }
    public String getRequestedBy() { return requestedBy; } public void setRequestedBy(String v) { requestedBy = v; }
}
