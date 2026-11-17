package com.supplygraph.application.service;

import com.supplygraph.application.port.out.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.monitoring.*;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Continuous monitoring engine evaluating incoming operational events against
 * configurable industry rules and current digital twin state.
 */
@Service
public class OperationalMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(OperationalMonitoringService.class);

    private final MonitoringRuleRepositoryPort ruleRepository;
    private final RiskAlertRepositoryPort alertRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final RecoveryPlanRepositoryPort recoveryPlanRepository;
    private final ContinuousImpactReassessmentService reassessmentService;
    private final AdaptiveReplanningService replanningService;

    public OperationalMonitoringService(
            MonitoringRuleRepositoryPort ruleRepository,
            RiskAlertRepositoryPort alertRepository,
            DigitalTwinRepositoryPort digitalTwinRepository,
            RecoveryPlanRepositoryPort recoveryPlanRepository,
            @Lazy ContinuousImpactReassessmentService reassessmentService,
            @Lazy AdaptiveReplanningService replanningService
    ) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "MonitoringRuleRepositoryPort must not be null");
        this.alertRepository = Objects.requireNonNull(alertRepository, "RiskAlertRepositoryPort must not be null");
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.recoveryPlanRepository = Objects.requireNonNull(recoveryPlanRepository, "RecoveryPlanRepositoryPort must not be null");
        this.reassessmentService = reassessmentService;
        this.replanningService = replanningService;
    }

    @Transactional
    public List<RiskAlert> evaluateEvent(OperationalEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        List<MonitoringRule> rules = ruleRepository.findAll().stream()
                .filter(MonitoringRule::isEnabled)
                .toList();

        List<RiskAlert> generatedAlerts = new ArrayList<>();
        StateVersion currentVersion = digitalTwinRepository.findByWorkspaceId(event.getWorkspaceId())
                .map(DigitalTwinState::getCurrentVersion)
                .orElse(StateVersion.initial());

        for (MonitoringRule rule : rules) {
            Optional<RiskAlert> alertOpt = evaluateRule(rule, event, currentVersion);
            if (alertOpt.isPresent()) {
                RiskAlert alert = alertOpt.get();

                // Alert Deduplication & Suppression check
                Optional<RiskAlert> activeExisting = alertRepository.findActiveBySuppressionKey(alert.getSuppressionKey());
                if (activeExisting.isPresent()) {
                    log.info("Suppressing duplicate active alert for suppressionKey {}", alert.getSuppressionKey());
                    alert.suppress();
                } else {
                    alertRepository.save(alert);
                    generatedAlerts.add(alert);
                    log.warn("Operational risk alert generated: [{}] {} (Severity: {})",
                            alert.getId().value(), alert.getTitle(), alert.getSeverity());

                    // Check for invalidation of existing active/pending recovery plans
                    checkRecoveryPlanInvalidations(alert, event);

                    // If high or critical, trigger continuous reassessment and adaptive replanning
                    if (alert.getSeverity() == MonitoringSeverity.CRITICAL || alert.getSeverity() == MonitoringSeverity.HIGH) {
                        reassessmentService.reassessOnEvent(event, alert);
                        replanningService.triggerAdaptiveReplanning(alert, event);
                    }
                }
            }
        }
        return generatedAlerts;
    }

    private Optional<RiskAlert> evaluateRule(MonitoringRule rule, OperationalEvent event, StateVersion version) {
        String condition = rule.getConditionType();

        // 1. Supplier Delivery Delay
        if ("SUPPLIER_DELAY_THRESHOLD".equalsIgnoreCase(condition)
                && event.getEventType() == EventType.SUPPLIER_DELIVERY_DELAYED) {
            String delayHoursStr = event.getPayload().getOrDefault("delayHours", "0");
            try {
                BigDecimal delayHours = new BigDecimal(delayHoursStr);
                if (delayHours.compareTo(rule.getThreshold()) > 0) {
                    return Optional.of(RiskAlert.createActive(
                            event.getWorkspaceId(),
                            rule.getId(),
                            rule.getSeverity(),
                            "Supplier Delivery Delay Exceeds Threshold",
                            "Supplier " + event.getAffectedEntityId() + " delayed delivery by " + delayHours + "h (Threshold: " + rule.getThreshold() + "h)",
                            event.getAffectedEntityId(),
                            event.getAffectedEntityType(),
                            event.getEventId(),
                            version,
                            Map.of("delayHours", delayHoursStr, "threshold", rule.getThreshold().toString())
                    ));
                }
            } catch (Exception ignored) {}
        }

        // 2. Capacity Drop Threshold
        if ("CAPACITY_DROP_THRESHOLD".equalsIgnoreCase(condition)
                && (event.getEventType() == EventType.SUPPLIER_CAPACITY_CHANGED || event.getEventType() == EventType.PRODUCTION_CAPACITY_CHANGED)) {
            String dropPctStr = event.getPayload().getOrDefault("capacityDropPercentage", "0");
            try {
                BigDecimal dropPct = new BigDecimal(dropPctStr);
                if (dropPct.compareTo(rule.getThreshold()) >= 0) {
                    return Optional.of(RiskAlert.createActive(
                            event.getWorkspaceId(),
                            rule.getId(),
                            rule.getSeverity(),
                            "Operational Capacity Drop Exceeds Safe Threshold",
                            "Facility " + event.getAffectedEntityId() + " capacity diminished by " + dropPct + "% (Threshold: " + rule.getThreshold() + "%)",
                            event.getAffectedEntityId(),
                            event.getAffectedEntityType(),
                            event.getEventId(),
                            version,
                            Map.of("dropPercentage", dropPctStr, "threshold", rule.getThreshold().toString())
                    ));
                }
            } catch (Exception ignored) {}
        }

        // 3. Inventory Safety Stock Breach
        if ("INVENTORY_SAFETY_STOCK_BREACH".equalsIgnoreCase(condition)
                && event.getEventType() == EventType.INVENTORY_LEVEL_CHANGED) {
            String balanceStr = event.getPayload().getOrDefault("onHandQuantity", "0");
            try {
                BigDecimal balance = new BigDecimal(balanceStr);
                if (balance.compareTo(rule.getThreshold()) < 0) {
                    return Optional.of(RiskAlert.createActive(
                            event.getWorkspaceId(),
                            rule.getId(),
                            rule.getSeverity(),
                            "Inventory Level Breached Safety Buffer",
                            "Node " + event.getAffectedEntityId() + " on-hand quantity dropped to " + balance + " (Safety Stock: " + rule.getThreshold() + ")",
                            event.getAffectedEntityId(),
                            event.getAffectedEntityType(),
                            event.getEventId(),
                            version,
                            Map.of("currentBalance", balanceStr, "safetyThreshold", rule.getThreshold().toString())
                    ));
                }
            } catch (Exception ignored) {}
        }

        // 4. Quality Hold Incident
        if ("QUALITY_HOLD_INCIDENT".equalsIgnoreCase(condition)
                && event.getEventType() == EventType.QUALITY_HOLD_RECORDED) {
            return Optional.of(RiskAlert.createActive(
                    event.getWorkspaceId(),
                    rule.getId(),
                    rule.getSeverity(),
                    "Quality Containment Hold Placed on Material Lot",
                    "Quality containment hold initiated on lot for material " + event.getAffectedEntityId(),
                    event.getAffectedEntityId(),
                    event.getAffectedEntityType(),
                    event.getEventId(),
                    version,
                    event.getPayload()
            ));
        }

        return Optional.empty();
    }

    private void checkRecoveryPlanInvalidations(RiskAlert alert, OperationalEvent event) {
        List<RecoveryPlan> plans = recoveryPlanRepository.findAllByWorkspaceId(event.getWorkspaceId());
        for (RecoveryPlan plan : plans) {
            if (plan.status() == RecoveryPlanStatus.APPROVED || plan.status() == RecoveryPlanStatus.PENDING_APPROVAL) {
                boolean reliesOnEntity = plan.actions().stream().anyMatch(action ->
                        (action.targetNodeId() != null && action.targetNodeId().value().equalsIgnoreCase(event.getAffectedEntityId()))
                                || (action.targetMaterialId() != null && action.targetMaterialId().value().equalsIgnoreCase(event.getAffectedEntityId()))
                );

                if (reliesOnEntity) {
                    log.warn("Active or pending recovery plan {} relies on disrupted entity {}. Assumption invalidated!",
                            plan.id().value(), event.getAffectedEntityId());
                }
            }
        }
    }

    @Transactional
    public void acknowledgeAlert(RiskAlertId alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.acknowledge(Instant.now());
            alertRepository.save(alert);
        });
    }

    @Transactional
    public void resolveAlert(RiskAlertId alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.resolve(Instant.now());
            alertRepository.save(alert);
        });
    }

    public List<RiskAlert> getActiveAlerts(WorkspaceId workspaceId) {
        return alertRepository.findActiveAlerts(workspaceId);
    }

    public List<RiskAlert> getAllAlerts(WorkspaceId workspaceId) {
        return alertRepository.findByWorkspaceId(workspaceId);
    }

    public List<MonitoringRule> getAllRules() {
        return ruleRepository.findAll();
    }

    @Transactional
    public void toggleRule(MonitoringRuleId ruleId, boolean enabled) {
        ruleRepository.findById(ruleId).ifPresent(r -> {
            r.setEnabled(enabled);
            ruleRepository.save(r);
        });
    }
}
