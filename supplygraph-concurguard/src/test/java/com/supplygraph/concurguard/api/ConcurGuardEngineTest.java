package com.supplygraph.concurguard.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConcurGuard API Contract Tests")
class ConcurGuardEngineTest {

    @Test
    @DisplayName("Should define all 7 required conflict classes")
    void shouldDefineAllConflictClasses() {
        assertThat(ConflictType.values()).containsExactly(
                ConflictType.QUANTITY_CONFLICT,
                ConflictType.CAPACITY_CONFLICT,
                ConflictType.TEMPORAL_OVERLAP,
                ConflictType.SEMANTIC_ALIAS,
                ConflictType.DEPENDENCY_CONFLICT,
                ConflictType.STALE_STATE_CONFLICT,
                ConflictType.INDUSTRY_POLICY_VIOLATION
        );
    }

    @Test
    @DisplayName("Should define all required arbitration outcomes")
    void shouldDefineArbitrationOutcomes() {
        assertThat(ArbitrationStatus.values()).containsExactly(
                ArbitrationStatus.ALLOW,
                ArbitrationStatus.DELAY,
                ArbitrationStatus.RESCHEDULE,
                ArbitrationStatus.MODIFY,
                ArbitrationStatus.REJECT,
                ArbitrationStatus.REQUEST_REPLANNING
        );
    }
}

