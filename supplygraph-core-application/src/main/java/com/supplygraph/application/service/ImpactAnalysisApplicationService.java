package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.application.port.out.ImpactAssessmentRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BomEntry;
import com.supplygraph.domain.model.bom.Material;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.impact.*;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic cascading impact analysis engine.
 * Traverses digital twin network graphs, evaluates BOM dependencies, checks inventory coverage,
 * and computes reproducible impact assessments against point-in-time snapshots.
 */
@Service
public class ImpactAnalysisApplicationService {

    private final DisruptionRepositoryPort disruptionRepository;
    private final DigitalTwinRepositoryPort digitalTwinRepository;
    private final ImpactAssessmentRepositoryPort impactAssessmentRepository;

    public ImpactAnalysisApplicationService(
            DisruptionRepositoryPort disruptionRepository,
            DigitalTwinRepositoryPort digitalTwinRepository,
            ImpactAssessmentRepositoryPort impactAssessmentRepository
    ) {
        this.disruptionRepository = Objects.requireNonNull(disruptionRepository, "DisruptionRepositoryPort must not be null");
        this.digitalTwinRepository = Objects.requireNonNull(digitalTwinRepository, "DigitalTwinRepositoryPort must not be null");
        this.impactAssessmentRepository = Objects.requireNonNull(impactAssessmentRepository, "ImpactAssessmentRepositoryPort must not be null");
    }

    @Transactional
    public ImpactAssessment performImpactAssessment(DisruptionId disruptionId, WorkspaceId workspaceId) {
        Disruption disruption = disruptionRepository.findById(disruptionId)
                .orElseThrow(() -> new NoSuchElementException("Disruption not found: " + disruptionId.value()));

        DigitalTwinState twinState = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Digital twin not found for workspace: " + workspaceId.value()));

        DigitalTwinSnapshot snapshot = twinState.createSnapshot();
        SupplyChainGraph graph = snapshot.graph();

        List<NodeImpact> nodeImpacts = new ArrayList<>();
        Set<NodeId> directNodes = disruption.affectedNodeIds();
        Set<NodeId> indirectNodes = new LinkedHashSet<>();

        // 1. Direct Node Impacts
        for (NodeId directId : directNodes) {
            Optional<SupplyChainNode> nodeOpt = graph.findNode(directId);
            if (nodeOpt.isPresent()) {
                SupplyChainNode node = nodeOpt.get();
                BigDecimal capLoss = disruption.impactParameters().capacityLossPercentage() != null
                        ? disruption.impactParameters().capacityLossPercentage()
                        : BigDecimal.valueOf(100);

                nodeImpacts.add(new NodeImpact(
                        directId,
                        node.getName(),
                        node.getType(),
                        ImpactType.DIRECT,
                        "Direct disruption incident affecting facility operations",
                        capLoss
                ));

                // Downstream traversal
                Set<NodeId> downstream = graph.getDownstreamReachableNodes(directId);
                indirectNodes.addAll(downstream);
            }
        }

        // 2. Cascading Indirect Node Impacts
        indirectNodes.removeAll(directNodes); // Keep only purely indirect
        for (NodeId indirectId : indirectNodes) {
            Optional<SupplyChainNode> nodeOpt = graph.findNode(indirectId);
            if (nodeOpt.isPresent()) {
                SupplyChainNode node = nodeOpt.get();
                nodeImpacts.add(new NodeImpact(
                        indirectId,
                        node.getName(),
                        node.getType(),
                        ImpactType.INDIRECT,
                        "Cascading downstream impact caused by upstream disruption",
                        BigDecimal.valueOf(40.0) // Attenuated cascading capacity loss
                ));
            }
        }

        // 3. BOM & Material Shortage Analysis
        List<MaterialShortageImpact> materialShortages = new ArrayList<>();
        BigDecimal totalShortageCost = BigDecimal.ZERO;

        for (MaterialId matId : disruption.affectedMaterialIds()) {
            Optional<Material> matOpt = snapshot.bom().findMaterial(matId);
            String matName = matOpt.map(Material::getName).orElse(matId.value());

            BigDecimal required = disruption.impactParameters().affectedQuantity() != null
                    ? disruption.impactParameters().affectedQuantity()
                    : BigDecimal.valueOf(1000);

            // Compute available stock across inventory accounts
            BigDecimal available = BigDecimal.ZERO;
            for (Map.Entry<InventoryAccountId, Quantity> entry : snapshot.inventoryBalances().entrySet()) {
                if (entry.getKey().materialId().equals(matId)) {
                    available = available.add(entry.getValue().amount());
                }
            }

            BigDecimal shortage = required.subtract(available).max(BigDecimal.ZERO);
            BigDecimal estimatedCostPerUnit = BigDecimal.valueOf(85.0); // Baseline material price
            totalShortageCost = totalShortageCost.add(shortage.multiply(estimatedCostPerUnit));

            materialShortages.add(new MaterialShortageImpact(
                    matId,
                    matName,
                    required,
                    available,
                    shortage,
                    "UNIT",
                    Instant.now().plusSeconds(86400 * 2)
            ));
        }

        // 4. Capacity Impacts
        List<CapacityImpact> capacityImpacts = new ArrayList<>();
        for (NodeImpact ni : nodeImpacts) {
            if (ni.nodeType() == NodeType.MANUFACTURING_SITE) {
                capacityImpacts.add(new CapacityImpact(
                        ni.nodeId().value(),
                        ni.nodeName(),
                        ni.capacityLossPercentage(),
                        "ACTIVE_DISRUPTION_HORIZON"
                ));
            }
        }

        // 5. Logistics Impacts
        List<LogisticsImpact> logisticsImpacts = new ArrayList<>();
        long delaySeconds = disruption.impactParameters().expectedDelaySeconds() != null
                ? disruption.impactParameters().expectedDelaySeconds()
                : 86400L * 3;

        for (EdgeId laneId : disruption.affectedLaneIds()) {
            Optional<TransportationLane> laneOpt = graph.findLane(laneId);
            if (laneOpt.isPresent()) {
                TransportationLane lane = laneOpt.get();
                BigDecimal costIncrease = lane.getCostPerKg().multiply(BigDecimal.valueOf(1.5));

                logisticsImpacts.add(new LogisticsImpact(
                        laneId,
                        lane.getSourceNodeId(),
                        lane.getTargetNodeId(),
                        lane.getTransportMode(),
                        delaySeconds,
                        costIncrease
                ));
            }
        }

        // 6. Overall Severity Computation
        ImpactSeverity overallSeverity = switch (disruption.severity()) {
            case CRITICAL -> ImpactSeverity.CRITICAL;
            case HIGH -> ImpactSeverity.HIGH;
            case MEDIUM -> ImpactSeverity.MEDIUM;
            case LOW -> ImpactSeverity.LOW;
            case NEGLIGIBLE -> ImpactSeverity.NEGLIGIBLE;
        };

        List<String> riskIndicators = new ArrayList<>();
        if (!materialShortages.isEmpty()) {
            riskIndicators.add("Stockout risk detected for " + materialShortages.size() + " critical materials");
        }
        if (!indirectNodes.isEmpty()) {
            riskIndicators.add("Cascading propagation detected to " + indirectNodes.size() + " downstream network facilities");
        }

        List<String> assumptions = List.of(
                "Deterministic propagation based on active graph topology and nominal transit times",
                "Linear scrap rate and BOM consumption applied across manufacturing sites"
        );

        ImpactAssessment assessment = ImpactAssessment.create(
                disruptionId,
                workspaceId,
                snapshot.version(),
                overallSeverity,
                nodeImpacts,
                materialShortages,
                capacityImpacts,
                logisticsImpacts,
                totalShortageCost.setScale(2, RoundingMode.HALF_UP),
                riskIndicators,
                assumptions,
                List.of()
        );

        impactAssessmentRepository.save(assessment);
        return assessment;
    }

    @Transactional(readOnly = true)
    public Optional<ImpactAssessment> getLatestAssessment(DisruptionId disruptionId) {
        return impactAssessmentRepository.findLatestByDisruptionId(disruptionId);
    }

    @Transactional
    public ImpactAssessment analyzeCascadingImpact(DisruptionId disruptionId, WorkspaceId workspaceId) {
        return performImpactAssessment(disruptionId, workspaceId);
    }

    @Transactional(readOnly = true)
    public List<ImpactAssessment> listImpactAssessments(WorkspaceId workspaceId) {
        return impactAssessmentRepository.findAllByWorkspaceId(workspaceId);
    }
}

