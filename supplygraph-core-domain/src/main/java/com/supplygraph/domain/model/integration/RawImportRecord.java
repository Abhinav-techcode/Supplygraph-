package com.supplygraph.domain.model.integration;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed in-memory record extracted from an input dataset before transactional persistence.
 */
public record RawImportRecord(
        int rowIndex,
        DataCategory category,
        Map<String, String> fields,
        String rawPayload,
        boolean valid
) implements Serializable {

    public RawImportRecord {
        Objects.requireNonNull(category, "DataCategory cannot be null");
        fields = fields != null ? Collections.unmodifiableMap(fields) : Collections.emptyMap();
        rawPayload = rawPayload != null ? rawPayload : "";
    }

    public static RawImportRecord of(int rowIndex, DataCategory category, Map<String, String> fields, String rawPayload, boolean valid) {
        return new RawImportRecord(rowIndex, category, fields, rawPayload, valid);
    }

    public String get(String key) {
        return fields.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return fields.getOrDefault(key, defaultValue);
    }
}

