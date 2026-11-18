package com.supplygraph.application.connector;

import java.util.Map;

/** One validated record from an external system: {"id","entityType","fields":{...}}. */
public record ExternalRecord(String id, String entityType, Map<String, String> fields) {
    public ExternalRecord { fields = Map.copyOf(fields); }
}
