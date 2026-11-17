package com.supplygraph.application.service;

import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.monitoring.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Operational Monitoring Service Tests")
class OperationalMonitoringServiceTest {

    private MonitoringRuleRepositoryPort ruleRepo;
    private RiskAlertRepositoryPort alertRepo;
    private DigitalTwinRepositoryPort digitalTwinRepo;
    private RecoveryPlanRepositoryPort recoveryPlanRepo;
    private ContinuousImpactReassessmentService reassessmentService;
    private AdaptiveReplanningService replanningService;
    private OperationalMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        ruleRepo = mock(MonitoringRuleRepositoryPort.class);
        alertRepo = mock(RiskAlertRepositoryPort.class);
        digitalTwinRepo = mock(DigitalTwinRepositoryPort.class);
        recoveryPlanRepo = mock(RecoveryPlanRepositoryPort.class);
        reassessmentService = mock(ContinuousImpactReassessmentService.class);
        replanningService = mock(AdaptiveReplanningService.class);

        monitoringService = new OperationalMonitoringService(
                ruleRepo, alertRepo, digitalTwinRepo, recoveryPlanRepo,
                reassessmentService, replanningService
        );
    }

    @Test
    @DisplayName("Should evaluate rule and trigger alert when supplier delay exceeds threshold")
    void shouldTriggerAlertOnSupplierDelay() {
        MonitoringRule rule = MonitoringRule.of(
                "RULE-DELAY", "Supplier Delay Threshold", "Desc", "AUTOMOTIVE",
                MonitoringSeverity.CRITICAL, BigDecimal.valueOf(24),
                MonitoringEvaluationTrigger.ON_EVENT, "SUPPLIER_DELAY_THRESHOLD"
        );
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(alertRepo.findActiveBySuppressionKey(any())).thenReturn(Optional.empty());

        DigitalTwinState twinState = new DigitalTwinState(WorkspaceId.of("WS-TEST-1"), StateVersion.of(1L), null, null);
        when(digitalTwinRepo.findByWorkspaceId(any())).thenReturn(Optional.of(twinState));
        when(recoveryPlanRepo.findAllByWorkspaceId(any())).thenReturn(List.of());

        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now(),
                "SUP-MUNICH",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of("delayHours", "48"),
                EventProvenance.internal("system"),
                "KEY-10"
        );

        List<RiskAlert> alerts = monitoringService.evaluateEvent(event);

        assertThat(alerts).hasSize(1);
        RiskAlert alert = alerts.get(0);
        assertThat(alert.getSeverity()).isEqualTo(MonitoringSeverity.CRITICAL);
        assertThat(alert.getAffectedEntityId()).isEqualTo("SUP-MUNICH");

        verify(alertRepo).save(alert);
        verify(reassessmentService).reassessOnEvent(eq(event), eq(alert));
        verify(replanningService).triggerAdaptiveReplanning(eq(alert), eq(event));
    }

    @Test
    @DisplayName("Should suppress duplicate active alert on unchanged condition")
    void shouldSuppressDuplicateActiveAlert() {
        MonitoringRule rule = MonitoringRule.of(
                "RULE-DELAY", "Supplier Delay Threshold", "Desc", "AUTOMOTIVE",
                MonitoringSeverity.HIGH, BigDecimal.valueOf(24),
                MonitoringEvaluationTrigger.ON_EVENT, "SUPPLIER_DELAY_THRESHOLD"
        );
        when(ruleRepo.findAll()).thenReturn(List.of(rule));

        RiskAlert existingAlert = RiskAlert.createActive(
                WorkspaceId.of("WS-TEST-1"), rule.getId(), MonitoringSeverity.HIGH,
                "Title", "Desc", "SUP-MUNICH", "NODE", EventId.of("EVT-1"), StateVersion.of(1L), Map.of()
        );
        when(alertRepo.findActiveBySuppressionKey(any())).thenReturn(Optional.of(existingAlert));
        when(digitalTwinRepo.findByWorkspaceId(any())).thenReturn(Optional.empty());

        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now(),
                "SUP-MUNICH",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of("delayHours", "36"),
                EventProvenance.internal("system"),
                "KEY-20"
        );

        List<RiskAlert> alerts = monitoringService.evaluateEvent(event);

        // Alert was suppressed, so not returned in newly generated active alerts
        assertThat(alerts).isEmpty();
        verify(alertRepo, never()).save(any());
    }
}

