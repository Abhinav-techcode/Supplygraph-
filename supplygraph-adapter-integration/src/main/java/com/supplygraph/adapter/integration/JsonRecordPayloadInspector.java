package com.supplygraph.adapter.integration;

import com.supplygraph.domain.error.SecurityViolationException;
import com.supplygraph.domain.error.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplygraph.application.connector.ExternalRecord;
import com.supplygraph.application.connector.RecordPayloadInspector;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/** A record is valid when it has a non-blank text "id", non-blank text "entityType", and a "fields" object with at least one scalar value. */
@Component
public class JsonRecordPayloadInspector implements RecordPayloadInspector {
    private static final int MAX_RECORDS = 10_000;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Inspection inspect(byte[] body) {
        JsonNode array = array(body);
        return new Inspection(array.size(), (int) toRecords(array).size());
    }

    @Override
    public List<ExternalRecord> parse(byte[] body) { return toRecords(array(body)); }

    private JsonNode array(byte[] body) {
        JsonNode root;
        try { root = mapper.readTree(body); } catch (IOException e) { throw new ValidationException("Malformed JSON"); }
        JsonNode array = root == null ? null : root.isArray() ? root : root.path("records");
        if (array == null || !array.isArray()) throw new ValidationException("Expected a JSON array or {\"records\": [...]}");
        if (array.size() > MAX_RECORDS) throw new ValidationException("Too many records (max " + MAX_RECORDS + ")");
        return array;
    }

    private List<ExternalRecord> toRecords(JsonNode array) {
        List<ExternalRecord> out = new ArrayList<>();
        for (JsonNode n : array) {
            if (!n.isObject() || !text(n.path("id")) || !text(n.path("entityType")) || !n.path("fields").isObject()) continue;
            Map<String, String> fields = new LinkedHashMap<>();
            n.path("fields").fields().forEachRemaining(e -> { if (e.getValue().isValueNode() && !e.getValue().isNull()) fields.put(e.getKey(), e.getValue().asText()); });
            if (!fields.isEmpty()) out.add(new ExternalRecord(n.path("id").asText(), n.path("entityType").asText(), fields));
        }
        return out;
    }

    private static boolean text(JsonNode n) { return n.isTextual() && !n.asText().isBlank(); }
}
