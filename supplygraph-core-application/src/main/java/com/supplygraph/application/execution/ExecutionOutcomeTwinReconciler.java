package com.supplygraph.application.execution;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.event.EventType;
import com.supplygraph.domain.model.event.OperationalEvent;
import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Closed-loop reconciler that mutates the live Digital Twin state (inventory ledger and network graph)
 * when recovery actions complete and produce verified operational evidence.
 */
@Service
public class ExecutionOutcomeTwinReconciler {

    private static final Logger log = LoggerFactory.getLogger(ExecutionOutcomeTwinReconciler.class);

    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final AuditGovernanceService auditGovernanceService;

    public ExecutionOutcomeTwinReconciler(
            DigitalTwinRepositoryPort digitalTwinRepository,
            AuditGovernanceService auditGovernanceService
    ) {
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.auditGovernanceService = Objects.requireNonNull(auditGovernanceService, "AuditGovernanceService must not be null");
    }

    @Transactional
    public boolean reconcile(OperationalEvent event) {
        if (event == null || event.getEventType() != EventType.RECOVERY_ACTION_COMPLETED) {
            return false;
        }

        WorkspaceId wsId = event.getWorkspaceId();
        Optional<DigitalTwinState> twinOpt = digitalTwinRepository.findByWorkspaceId(wsId);
        if (twinOpt.isEmpty()) {
            log.warn("Cannot reconcile execution outcome: Digital twin not found for workspace {}", wsId.value());
            return false;
        }

        DigitalTwinState currentTwin = twinOpt.get();
        Map<String, String> payload = event.getPayload();
        String taskType = payload.getOrDefault("taskType", "UNKNOWN");
        String targetEntity = event.getAffectedEntityId();
        boolean mutated = false;

        // Reconcile inventory: a verified transfer moves stock from the source facility to the target facility (balanced double entry).
        // Nothing is assumed: material, quantity and both facilities must come from the evidence; otherwise the twin is left unchanged.
        String qtyStr = payload.getOrDefault("quantity", payload.get("actual"));
        String mat = payload.get("materialId");
        String sourceNode = payload.get("sourceNodeId");
        String targetNode = payload.getOrDefault("targetNodeId", targetEntity);
        if (qtyStr != null && mat != null && sourceNode != null && targetNode != null && !sourceNode.equals(targetNode)) {
            try {
                BigDecimal qtyVal = new BigDecimal(qtyStr);
                UnitOfMeasure unit = currentTwin.getBom().findMaterial(MaterialId.of(mat)).map(m -> m.getBaseUnit()).orElse(UnitOfMeasure.PIECE);
                InventoryAccountId sourceAcc = InventoryAccountId.of(NodeId.of(sourceNode), MaterialId.of(mat), BalanceCategory.ON_HAND_AVAILABLE);
                InventoryAccountId destAcc = InventoryAccountId.of(NodeId.of(targetNode), MaterialId.of(mat), BalanceCategory.ON_HAND_AVAILABLE);
                Quantity q = Quantity.of(qtyVal, unit);

                TransactionId txId = TransactionId.of("TX-" + event.getEventId().value());
                if (currentTwin.getInventoryLedger().findTransaction(txId).isEmpty()) {
                    currentTwin.getInventoryLedger().postTransaction(new InventoryTransaction(
                            txId,
                            Instant.now(),
                            "RECOVERY_EXECUTION_OUTCOME",
                            event.getEventId().value(),
                            List.of(
                                    new InventoryPosting(UUID.randomUUID().toString(), sourceAcc, PostingType.CREDIT, q),   // CREDIT lowers the source balance
                                    new InventoryPosting(UUID.randomUUID().toString(), destAcc, PostingType.DEBIT, q)
                            )
                    ));
                    mutated = true;
                    log.info("Moved {} of {} from {} to {} in the twin of workspace {} (event {})", qtyStr, mat, sourceNode, targetNode, wsId.value(), event.getEventId().value());
                }
            } catch (Exception ex) {
                log.warn("Could not post inventory transaction for event {}: {}", event.getEventId().value(), ex.getMessage());
            }
        }

        // Reconcile Transportation / Logistics Lane
        if ("REROUTE_SHIPMENT".equalsIgnoreCase(taskType) || "CHANGE_TRANSPORT_MODE".equalsIgnoreCase(taskType) || payload.containsKey("laneId")) {
            String laneIdStr = payload.getOrDefault("laneId", targetEntity);
            if (laneIdStr != null && currentTwin.getGraph() != null) {
                EdgeId edgeId = EdgeId.of(laneIdStr);
                Optional<TransportationLane> laneOpt = currentTwin.getGraph().findLane(edgeId);
                if (laneOpt.isPresent()) {
                    TransportationLane lane = laneOpt.get();
                    if (!lane.isOperational()) {
                        lane.setOperational(true);
                        mutated = true;
                        log.info("Restored operational status for lane {} in workspace {}", edgeId.value(), wsId.value());
                    }
                }
            }
        }

        if (mutated) {
            DigitalTwinState advanced = new DigitalTwinState(
                    wsId,
                    currentTwin.getCurrentVersion().next(),
                    currentTwin.getGraph(),
                    currentTwin.getBom()
            );
            currentTwin.getInventoryLedger().getTransactionHistory().forEach(tx -> advanced.getInventoryLedger().postTransaction(tx));
            digitalTwinRepository.save(advanced);

            auditGovernanceService.logAction(
                    UserId.of(event.getProvenance() != null && event.getProvenance().sourceUser() != null ? event.getProvenance().sourceUser() : "EXECUTION_RECONCILER"),
                    wsId,
                    AuditAction.DIGITAL_TWIN_ADVANCED,
                    "DIGITAL_TWIN:" + wsId.value(),
                    "v" + currentTwin.getCurrentVersion().value(),
                    "v" + advanced.getCurrentVersion().value(),
                    "Digital twin state mutated by executed recovery outcome (" + taskType + ")",
                    "CONF-NONE"
            );
            return true;
        }

        return false;
    }
}
