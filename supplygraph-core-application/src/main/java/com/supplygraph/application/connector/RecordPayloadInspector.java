package com.supplygraph.application.connector;

import java.util.List;

/**
 * Port for payload parsing/validation. Accepted shape: a JSON array (or {"records":[...]}) of objects
 * {"id": non-blank, "entityType": non-blank, "fields": object with at least one scalar}. Implemented outside core.
 */
public interface RecordPayloadInspector {
    record Inspection(int received, int valid) {
        public int rejected() { return received - valid; }
    }
    /** @throws IllegalArgumentException if the payload is not well-formed. */
    Inspection inspect(byte[] body);

    /** Returns only the valid records. @throws IllegalArgumentException if the payload is not well-formed. */
    List<ExternalRecord> parse(byte[] body);
}
