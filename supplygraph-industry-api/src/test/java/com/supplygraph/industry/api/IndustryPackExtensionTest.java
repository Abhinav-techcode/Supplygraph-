package com.supplygraph.industry.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Industry Extension SPI Contract Tests")
class IndustryPackExtensionTest {

    @Test
    @DisplayName("Should successfully construct and validate IndustryIdentifier")
    void shouldConstructValidIndustryIdentifier() {
        IndustryIdentifier id = IndustryIdentifier.of("ev_battery");
        assertThat(id.code()).isEqualTo("EV_BATTERY");
    }

    @Test
    @DisplayName("Should create mock IndustryPackExtension conforming to SPI")
    void shouldConformToSpi() {
        IndustryPackExtension pack = new IndustryPackExtension() {
            @Override
            public IndustryIdentifier getId() {
                return IndustryIdentifier.of("TEST_IND");
            }

            @Override
            public String getDisplayName() {
                return "Test Industry";
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

            @Override
            public String getDescription() {
                return "Test Industry Description";
            }

            @Override
            public List<String> getSupportedScenarioCodes() {
                return List.of("SC-01", "SC-02");
            }
        };

        assertThat(pack.getId().code()).isEqualTo("TEST_IND");
        assertThat(pack.getSupportedScenarioCodes()).containsExactly("SC-01", "SC-02");
    }
}

