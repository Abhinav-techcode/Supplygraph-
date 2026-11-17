package com.supplygraph.application.integration;

import com.supplygraph.domain.model.integration.DataCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CSV and JSON Enterprise Data Parsers Test")
class CsvAndJsonDataParserTest {

    private final CsvDataParser csvParser = new CsvDataParser();
    private final JsonDataParser jsonParser = new JsonDataParser();

    @Test
    @DisplayName("Should successfully parse RFC-4180 CSV with quotes, commas, and headers")
    void shouldParseValidCsv() throws IOException {
        String csv = """
                supplier_id,name,location,category,lead_time
                SUP-01,"Munich Fab, Europe",DEU,SEMICONDUCTOR,45
                SUP-02,"Tokyo Silicon Ltd",JPN,RAW_MATERIAL,30
                """;
        var result = csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), DataCategory.SUPPLIER);

        assertThat(result.failures()).isEmpty();
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).get("supplier_id")).isEqualTo("SUP-01");
        assertThat(result.records().get(0).get("name")).isEqualTo("Munich Fab, Europe");
        assertThat(result.records().get(0).get("lead_time")).isEqualTo("45");
        assertThat(result.records().get(1).get("supplier_id")).isEqualTo("SUP-02");
    }

    @Test
    @DisplayName("Should detect mismatched column count in CSV line")
    void shouldDetectMismatchedColumnsInCsv() throws IOException {
        String csv = """
                material_id,name,unit
                MAT-01,Lithium Hydroxide,KG
                MAT-02,Incomplete Line
                """;
        var result = csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), DataCategory.MATERIAL);

        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).errorCode()).isEqualTo("MISMATCHED_COLUMN_COUNT");
        assertThat(result.failures().get(0).rowIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should successfully parse JSON array of enterprise entities")
    void shouldParseValidJson() throws IOException {
        String json = """
                [
                  {
                    "warehouse_id": "WH-FRANKFURT",
                    "material_id": "MAT-MCU-32",
                    "quantity": "2500",
                    "unit": "PIECE"
                  },
                  {
                    "warehouse_id": "WH-MUNICH",
                    "material_id": "MAT-LITHIUM-01",
                    "quantity": "850.5",
                    "unit": "KG"
                  }
                ]
                """;
        var result = jsonParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), DataCategory.INVENTORY);

        assertThat(result.failures()).isEmpty();
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).get("warehouse_id")).isEqualTo("WH-FRANKFURT");
        assertThat(result.records().get(0).get("material_id")).isEqualTo("MAT-MCU-32");
        assertThat(result.records().get(0).get("quantity")).isEqualTo("2500");
    }

    @Test
    @DisplayName("Should reject malformed JSON that does not start with array or object")
    void shouldRejectMalformedJson() throws IOException {
        String invalid = "plain text, not json";
        var result = jsonParser.parse(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8)), DataCategory.INVENTORY);

        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).errorCode()).isEqualTo("INVALID_JSON_STRUCTURE");
    }
}
