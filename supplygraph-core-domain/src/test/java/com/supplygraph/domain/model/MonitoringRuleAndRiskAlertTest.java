package com.supplygraph.domain.model;

import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.monitoring.*;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Monitoring Rule & Risk Alert Domain Tests")
class MonitoringRuleAndRiskAlertTest {

    @Test
    @DisplayName("Should create monitoring rule and toggle enable status")
    void shouldCreateMonitoringRule() {
        MonitoringRule rule = MonitoringRule.of(
                "RULE-SUP-DELAY",
                "Supplier Transit Delay Threshold",
                "Alerts when supplier transit delay exceeds 24 hours",
                "AUTOMOTIVE",
                MonitoringSeverity.HIGH,
                BigDecimal.valueOf(24),
                MonitoringEvaluationTrigger.ON_EVENT,
                "SUPPLIER_DELAY_THRESHOLD"
        );

        assertThat(rule.getId().value()).isEqualTo("RULE-SUP-DELAY");
        assertThat(rule.isEnabled()).isTrue();
        rule.setEnabled(false);
        assertThat(rule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should create risk alert with suppression key and track lifecycle")
    void shouldCreateRiskAlertAndTrackLifecycle() {
        RiskAlert alert = RiskAlert.createActive(
                WorkspaceId.of("WS-TEST-1"),
                MonitoringRuleId.of("RULE-SUP-DELAY"),
                MonitoringSeverity.CRITICAL,
                "Critical Transit Delay at Supplier Munich",
                "Photolithography chemical shipment delayed by 48 hours",
                "SUP-MUNICH",
                "NODE",
                EventId.of("EVT-100"),
                StateVersion.of(5L),
                Map.of("delayHours", "48", "impactedLot", "LOT-901")
        );

        assertThat(alert.getStatus()).isEqualTo(RiskAlertStatus.ACTIVE);
        assertThat(alert.getSuppressionKey()).isEqualTo("WS-TEST-1:RULE-SUP-DELAY:SUP-MUNICH");
        assertThat(alert.getSourceStateVersion().value()).isEqualTo(5L);
        assertThat(alert.getEvidence()).containsEntry("delayHours", "48");

        alert.acknowledge(Instant.now());
        assertThat(alert.getStatus()).isEqualTo(RiskAlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedAt()).isNotNull();

        alert.resolve(Instant.now());
        assertThat(alert.getStatus()).isEqualTo(RiskAlertStatus.RESOLVED);
        assertThat(alert.getResolvedAt()).isNotNull();
    }
}

