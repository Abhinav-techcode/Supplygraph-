package com.supplygraph.application.integration;

import com.supplygraph.domain.model.integration.DataCategory;
import com.supplygraph.domain.model.integration.ImportRecordFailure;
import com.supplygraph.domain.model.integration.RawImportRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard parser for JSON arrays of key-value objects representing enterprise datasets.
 */
public final class JsonDataParser {

    public record ParseResult(List<RawImportRecord> records, List<ImportRecordFailure> failures) {}

    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern PAIR_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.-]+|true|false|null))");

    public ParseResult parse(InputStream inputStream, DataCategory category) throws IOException {
        List<RawImportRecord> records = new ArrayList<>();
        List<ImportRecordFailure> failures = new ArrayList<>();

        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append(" ");
            }
        }

        String raw = jsonContent.toString().trim();
        if (raw.isBlank()) {
            failures.add(new ImportRecordFailure(0, "PAYLOAD", "EMPTY", "EMPTY_FILE", "Input JSON payload is empty", "Provide a valid JSON array of objects"));
            return new ParseResult(records, failures);
        }

        if (!raw.startsWith("[") && !raw.startsWith("{")) {
            failures.add(new ImportRecordFailure(0, "SYNTAX", raw.substring(0, Math.min(30, raw.length())), "INVALID_JSON_STRUCTURE", "JSON must begin with '[' or '{'", "Wrap data in a JSON array"));
            return new ParseResult(records, failures);
        }

        Matcher objMatcher = OBJECT_PATTERN.matcher(raw);
        int rowIndex = 1;
        while (objMatcher.find()) {
            String objBody = objMatcher.group(1);
            Map<String, String> fields = new LinkedHashMap<>();

            Matcher pairMatcher = PAIR_PATTERN.matcher(objBody);
            while (pairMatcher.find()) {
                String key = pairMatcher.group(1).trim().toLowerCase().replace(" ", "_");
                String strVal = pairMatcher.group(2);
                String primitiveVal = pairMatcher.group(3);
                String val = strVal != null ? strVal : (primitiveVal != null ? primitiveVal : "");
                fields.put(key, val);
            }

            if (fields.isEmpty()) {
                failures.add(new ImportRecordFailure(rowIndex, "OBJECT", objBody, "EMPTY_JSON_OBJECT", "Object at index " + rowIndex + " has no parsable fields", "Ensure valid key-value pairs"));
                records.add(new RawImportRecord(rowIndex, category, Map.of(), objBody, false));
            } else {
                records.add(new RawImportRecord(rowIndex, category, fields, "{" + objBody + "}", true));
            }
            rowIndex++;
        }

        if (records.isEmpty() && failures.isEmpty()) {
            failures.add(new ImportRecordFailure(0, "PARSER", raw, "NO_RECORDS_FOUND", "No structured JSON objects identified", "Verify JSON array syntax"));
        }

        return new ParseResult(records, failures);
    }
}

