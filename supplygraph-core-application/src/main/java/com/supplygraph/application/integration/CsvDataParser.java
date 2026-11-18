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

/**
 * Deterministic RFC-4180 compliant CSV parser supporting quoted strings, commas, and line endings.
 */
public final class CsvDataParser {

    public record ParseResult(List<RawImportRecord> records, List<ImportRecordFailure> failures) {}

    public ParseResult parse(InputStream inputStream, DataCategory category) throws IOException {
        List<RawImportRecord> records = new ArrayList<>();
        List<ImportRecordFailure> failures = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                failures.add(new ImportRecordFailure(0, "HEADER", "EMPTY", "EMPTY_FILE", "Input CSV file is empty", "Provide a CSV file with valid header columns"));
                return new ParseResult(records, failures);
            }

            List<String> headers = parseCsvLine(headerLine);
            if (headers.isEmpty()) {
                failures.add(new ImportRecordFailure(0, "HEADER", headerLine, "INVALID_HEADER", "No valid columns identified in header", "Ensure columns are separated by commas"));
                return new ParseResult(records, failures);
            }

            String line;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                if (line.isBlank()) {
                    continue; // Skip blank lines
                }

                try {
                    List<String> values = parseCsvLine(line);
                    if (values.size() != headers.size()) {
                        failures.add(new ImportRecordFailure(
                                rowIndex,
                                "COLUMN_COUNT",
                                "Found " + values.size() + " values for " + headers.size() + " columns",
                                "MISMATCHED_COLUMN_COUNT",
                                "Row has " + values.size() + " fields; expected " + headers.size(),
                                "Align row columns to match header specification"
                        ));
                        records.add(new RawImportRecord(rowIndex, category, Map.of(), line, false));
                        continue;
                    }

                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String headerKey = headers.get(i).trim().toLowerCase().replace(" ", "_");
                        rowMap.put(headerKey, values.get(i).trim());
                    }
                    records.add(new RawImportRecord(rowIndex, category, rowMap, line, true));
                } catch (Exception ex) {
                    failures.add(new ImportRecordFailure(
                            rowIndex,
                            "LINE_SYNTAX",
                            line,
                            "CSV_PARSE_ERROR",
                            "Failed to parse CSV syntax: " + ex.getMessage(),
                            "Verify proper escaping of quotes in line"
                    ));
                    records.add(new RawImportRecord(rowIndex, category, Map.of(), line, false));
                }
            }
        }

        return new ParseResult(records, failures);
    }

    public static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        int len = line.length();

        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // Skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',' || c == '\t' || c == ';') {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}

