package com.supplygraph.industry.ev;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EV Battery Industry Pack Tests")
class EvBatteryIndustryPackTest {

    @Test
    @DisplayName("Should initialize EV Battery pack with 8 required scenario codes")
    void shouldInitializeWith8Scenarios() {
        EvBatteryIndustryPack pack = new EvBatteryIndustryPack();

        assertThat(pack.getId().code()).isEqualTo("EV_BATTERY");
        assertThat(pack.getDisplayName()).isEqualTo("EV Battery Manufacturing");
        assertThat(pack.getSupportedScenarioCodes()).hasSize(8);
        assertThat(pack.getSupportedScenarioCodes()).contains(
                "EV-SC-01-LITHIUM-SHORTAGE",
                "EV-SC-04-TEMP-EXCURSION"
        );
    }
}

