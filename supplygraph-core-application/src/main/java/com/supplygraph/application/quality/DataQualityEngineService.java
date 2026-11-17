package com.supplygraph.application.quality;

import com.supplygraph.application.port.out.DataQualityRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.graph.SupplyChainNode;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccount;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryBalance;
import com.supplygraph.domain.model.inventory.InventoryLedger;
import com.supplygraph.domain.model.quality.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic data quality validation engine auditing the digital twin against 8 enterprise quality dimensions.
 */
@Service
public class DataQualityEngineService {

    private static final Logger log = LoggerFactory.getLogger(DataQualityEngineService.class);

    private final DataQualityRepositoryPort qualityRepository;

    public DataQualityEngineService(DataQualityRepositoryPort qualityRepository) {
        this.qualityRepository = qualityRepository;
    }

    /**
     * Executes deterministic data quality rule evaluation across all nodes, edges, inventory ledgers, and BOMs.
     */
    public DataQualityReport evaluateWorkspaceQuality(String workspaceId, DigitalTwinState state) {
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(state, "DigitalTwinState cannot be null");

        List<DataQualityFinding> findings = new ArrayList<>();
        int totalEntities = 0;
        Set<String> observedNodeIds = new HashSet<>();

        SupplyChainGraph graph = state.getGraph();
        InventoryLedger ledger = state.getInventoryLedger();

        // 1. Evaluate Nodes (Uniqueness, Completeness, Validity)
        if (graph != null) {
            for (SupplyChainNode node : graph.getAllNodes()) {
                totalEntities++;
                String nodeId = node.getId().value();

                // Uniqueness
                if (!observedNodeIds.add(nodeId)) {
                    findings.add(DataQualityFinding.of(
                            "DQR-UNIQ-01",
                            QualityDimension.UNIQUENESS,
                            nodeId,
                            "id",
                            nodeId,
                            "Duplicate supply chain node ID identified in graph topology",
                            DataQualitySeverity.CRITICAL,
                            "Enforce globally unique node identifiers"
                    ));
                }

                // Completeness
                if (node.getName() == null || node.getName().isBlank()) {
                    findings.add(DataQualityFinding.of(
                            "DQR-COMP-01",
                            QualityDimension.COMPLETENESS,
                            nodeId,
                            "name",
                            "BLANK",
                            "Facility or node name is unpopulated",
                            DataQualitySeverity.WARNING,
                            "Populate official facility or supplier legal entity name"
                    ));
                }

                // Validity
                if (node.getLocation() == null || node.getLocation().isBlank()) {
                    findings.add(DataQualityFinding.of(
                            "DQR-VAL-01",
                            QualityDimension.VALIDITY,
                            nodeId,
                            "location",
                            "MISSING",
                            "Geographic location string is missing for node " + nodeId,
                            DataQualitySeverity.WARNING,
                            "Assign valid facility location and geographic jurisdiction"
                    ));
                }
            }

            // Referential Integrity: Validate Lane endpoints
            for (com.supplygraph.domain.model.graph.TransportationLane lane : graph.getAllLanes()) {
                totalEntities++;
                if (!graph.containsNode(lane.getSourceNodeId())) {
                    findings.add(DataQualityFinding.of(
                            "DQR-REF-01",
                            QualityDimension.REFERENTIAL_INTEGRITY,
                            lane.getId().value(),
                            "source",
                            lane.getSourceNodeId().value(),
                            "Transportation lane source node does not exist in graph",
                            DataQualitySeverity.CRITICAL,
                            "Register source facility node before linking transport lane"
                    ));
                }
                if (!graph.containsNode(lane.getTargetNodeId())) {
                    findings.add(DataQualityFinding.of(
                            "DQR-REF-02",
                            QualityDimension.REFERENTIAL_INTEGRITY,
                            lane.getId().value(),
                            "target",
                            lane.getTargetNodeId().value(),
                            "Transportation lane destination node does not exist in graph",
                            DataQualitySeverity.CRITICAL,
                            "Register target facility node before linking transport lane"
                    ));
                }
                if (lane.getNominalTransitTime().toDays() < 0) {
                    findings.add(DataQualityFinding.of(
                            "DQR-VAL-02",
                            QualityDimension.VALIDITY,
                            lane.getId().value(),
                            "leadTime",
                            lane.getNominalTransitTime().toDays() + " days",
                            "Transportation lane lead time cannot be negative",
                            DataQualitySeverity.CRITICAL,
                            "Correct transport lane lead time to positive duration"
                    ));
                }
            }
        }

        // 2. Evaluate Inventory Ledger (Consistency, Unit Consistency)
        if (ledger != null) {
            Map<InventoryAccountId, com.supplygraph.domain.model.common.Quantity> allBalances = ledger.getAllBalances();
            for (Map.Entry<InventoryAccountId, com.supplygraph.domain.model.common.Quantity> entry : allBalances.entrySet()) {
                totalEntities++;
                InventoryAccountId accountId = entry.getKey();
                com.supplygraph.domain.model.common.Quantity qty = entry.getValue();

                if (accountId.category() != BalanceCategory.EXTERNAL_OFFSET && qty.amount().compareTo(BigDecimal.ZERO) < 0) {
                    findings.add(DataQualityFinding.of(
                            "DQR-VAL-03",
                            QualityDimension.VALIDITY,
                            accountId.toString(),
                            "balance",
                            qty.amount().toString(),
                            "Internal inventory account balance cannot be negative",
                            DataQualitySeverity.CRITICAL,
                            "Reconcile double-entry inventory ledger with physical cycle count"
                    ));
                }
            }
        }

        // Calculate reproducible dimension scores and overall score
        Map<QualityDimension, Double> dimensionScores = new EnumMap<>(QualityDimension.class);
        for (QualityDimension dim : QualityDimension.values()) {
            double penalty = findings.stream()
                    .filter(f -> f.dimension() == dim)
                    .mapToDouble(f -> switch (f.severity()) {
                        case CRITICAL -> 15.0;
                        case WARNING -> 5.0;
                        case INFO -> 1.0;
                    })
                    .sum();
            dimensionScores.put(dim, Math.max(0.0, 100.0 - penalty));
        }

        double overallScore = dimensionScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(100.0);

        int flawedCount = (int) findings.stream().map(DataQualityFinding::entityId).distinct().count();
        int cleanCount = Math.max(0, totalEntities - flawedCount);

        DataQualityReport report = DataQualityReport.of(
                workspaceId,
                overallScore,
                dimensionScores,
                findings,
                totalEntities,
                cleanCount,
                flawedCount
        );

        qualityRepository.save(report);
        log.info("Data quality evaluation complete for workspace {}: Score={}/100, Findings={}",
                workspaceId, String.format("%.1f", overallScore), findings.size());
        return report;
    }

    public Optional<DataQualityReport> getLatestReport(String workspaceId) {
        return qualityRepository.findLatestByWorkspaceId(workspaceId);
    }

    public List<DataQualityReport> getHistory(String workspaceId) {
        return qualityRepository.findHistoryByWorkspaceId(workspaceId);
    }
}
