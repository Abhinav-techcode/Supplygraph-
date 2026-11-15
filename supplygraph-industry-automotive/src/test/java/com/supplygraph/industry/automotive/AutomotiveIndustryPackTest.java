package com.supplygraph.industry.automotive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Automotive Industry Pack Tests")
class AutomotiveIndustryPackTest {

    @Test
    @DisplayName("Should initialize Automotive pack with 8 required scenario codes")
    void shouldInitializeWith8Scenarios() {
        AutomotiveIndustryPack pack = new AutomotiveIndustryPack();

        assertThat(pack.getId().code()).isEqualTo("AUTOMOTIVE");
        assertThat(pack.getDisplayName()).isEqualTo("Automotive Component Manufacturing");
        assertThat(pack.getSupportedScenarioCodes()).hasSize(8);
        assertThat(pack.getSupportedScenarioCodes()).contains(
                "AU-SC-01-CRITICAL-PART-SHORTAGE",
                "AU-SC-03-JIT-DELIVERY-FAILURE"
        );
    }
}

