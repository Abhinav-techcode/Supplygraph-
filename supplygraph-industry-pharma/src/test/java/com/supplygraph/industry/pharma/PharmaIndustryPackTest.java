package com.supplygraph.industry.pharma;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pharmaceutical Industry Pack Tests")
class PharmaIndustryPackTest {

    @Test
    @DisplayName("Should initialize Pharma pack with 8 required scenario codes")
    void shouldInitializeWith8Scenarios() {
        PharmaIndustryPack pack = new PharmaIndustryPack();

        assertThat(pack.getId().code()).isEqualTo("PHARMACEUTICAL");
        assertThat(pack.getDisplayName()).isEqualTo("Pharmaceutical Supply Chain");
        assertThat(pack.getSupportedScenarioCodes()).hasSize(8);
        assertThat(pack.getSupportedScenarioCodes()).contains(
                "PH-SC-01-API-SHORTAGE",
                "PH-SC-02-COLD-CHAIN-EXCURSION"
        );
    }
}

