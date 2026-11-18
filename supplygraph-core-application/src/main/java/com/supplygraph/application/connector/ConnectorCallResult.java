package com.supplygraph.application.connector;

import com.supplygraph.domain.model.connector.ErrorCategory;

import java.util.List;

/** Outcome of one adapter call. "processed" means passed payload validation; reconciliation is applied by the sync service. */
public record ConnectorCallResult(boolean success, int recordsReceived, int recordsProcessed, int recordsRejected,
                                  ErrorCategory errorCategory, String message, boolean retryable, List<ExternalRecord> records) {
    public ConnectorCallResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
    public static ConnectorCallResult ok(int received, int processed, int rejected, String message) {
        return new ConnectorCallResult(true, received, processed, rejected, null, message, false, List.of());
    }
    public static ConnectorCallResult ok(int received, int processed, int rejected, String message, List<ExternalRecord> records) {
        return new ConnectorCallResult(true, received, processed, rejected, null, message, false, records);
    }
    public static ConnectorCallResult failure(ErrorCategory c, String message, boolean retryable) {
        return new ConnectorCallResult(false, 0, 0, 0, c, message, retryable, List.of());
    }
}
