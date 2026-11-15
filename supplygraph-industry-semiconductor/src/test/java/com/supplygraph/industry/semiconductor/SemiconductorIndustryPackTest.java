package com.supplygraph.industry.semiconductor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Semiconductor Industry Pack Tests")
class SemiconductorIndustryPackTest {

    @Test
    @DisplayName("Should initialize Semiconductor pack with 8 required scenario codes")
    void shouldInitializeWith8Scenarios() {
        SemiconductorIndustryPack pack = new SemiconductorIndustryPack();

        assertThat(pack.getId().code()).isEqualTo("SEMICONDUCTOR");
        assertThat(pack.getDisplayName()).isEqualTo("Semiconductor Manufacturing");
        assertThat(pack.getSupportedScenarioCodes()).hasSize(8);
        assertThat(pack.getSupportedScenarioCodes()).contains(
                "SC-SC-01-WAFER-SHORTAGE",
                "SC-SC-05-YIELD-DETERIORATION"
        );
    }
}

