package com.supplygraph.application.service;

import com.supplygraph.domain.error.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Disruption not found: " + disruptionId.value()));

        DigitalTwinState twinState = digitalTwinRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital twin not found for workspace: " + workspaceId.value()));

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

        // 3. Multi-tier BOM and material shortage analysis, computed from the twin. Quantities that the twin cannot supply are reported as missing.
        List<MaterialShortageImpact> materialShortages = new ArrayList<>();
        List<String> bomImpactDetails = new ArrayList<>();
        List<String> ttsDetails = new ArrayList<>();
        List<String> spofAlerts = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        com.supplygraph.application.agent.TwinInsights insights = com.supplygraph.application.agent.TwinInsights.of(snapshot, com.supplygraph.application.agent.TwinInsights.incidentOf(disruption));

        Set<MaterialId> allImpactedMaterials = new LinkedHashSet<>(disruption.affectedMaterialIds());
        if (allImpactedMaterials.isEmpty()) {
            insights.material().ifPresent(allImpactedMaterials::add);
            if (!allImpactedMaterials.isEmpty()) unknown.add("The disruption declares no affected material; the material with the largest stock in the network was assumed.");
        }
        Optional<NodeId> exposed = insights.exposedSite();
        double requiredUnits = disruption.impactParameters().affectedQuantity() != null ? disruption.impactParameters().affectedQuantity().doubleValue() : insights.requiredUnits();
        if (requiredUnits <= 0) unknown.add("Requirement over the outage is unknown: no downstream manufacturing site with a known throughput, and no affected quantity declared.");

        for (MaterialId matId : allImpactedMaterials) {
            String matName = insights.materialName(matId);
            BigDecimal available = BigDecimal.ZERO;
            for (Map.Entry<InventoryAccountId, Quantity> entry : snapshot.inventoryBalances().entrySet()) {
                if (entry.getKey().materialId().equals(matId) && !disruption.affectedNodeIds().contains(entry.getKey().nodeId())) available = available.add(entry.getValue().amount());
            }
            BigDecimal required = BigDecimal.valueOf(requiredUnits).setScale(0, RoundingMode.HALF_UP);
            BigDecimal shortage = required.subtract(available).max(BigDecimal.ZERO);

            java.util.OptionalDouble tts = insights.timeToSurviveDays(matId);
            Instant stockout = null;
            if (tts.isPresent()) {
                stockout = Instant.now().plusSeconds((long) (tts.getAsDouble() * 86400));
                ttsDetails.add("Time to survive for " + matName + " at " + exposed.map(NodeId::value).orElse("?") + ": " + String.format("%.1f", tts.getAsDouble())
                        + " days (own stock / daily throughput).");
            } else {
                unknown.add("Time to survive for " + matName + " is unknown: the exposed site has no known daily throughput.");
            }
            long reachableSources = insights.stockHolders(matId).stream().filter(h -> exposed.isEmpty() || insights.fastestRoute(h.node(), exposed.get()).isPresent()).count()
                    + insights.alternativeSuppliers().size();
            if (reachableSources == 0) spofAlerts.add("Single source: no stock holder or supplier outside the disruption can reach " + exposed.map(NodeId::value).orElse("the network") + " for " + matName + ".");

            materialShortages.add(new MaterialShortageImpact(matId, matName, required, available, shortage,
                    snapshot.bom().findMaterial(matId).map(m -> m.getBaseUnit().name()).orElse("PIECE"), stockout));
            List<MaterialId> parents = insights.dependentProducts(matId);
            if (!parents.isEmpty()) bomImpactDetails.add("BOM cascade: " + matName + " feeds " + parents.size() + " parent assemblies / products: " + parents.stream().map(MaterialId::value).limit(8).toList());
        }
        insights.singlePointsOfFailure().forEach(n -> spofAlerts.add("Facility " + n.value() + " receives freight only through the disrupted zone."));
        BigDecimal totalShortageCost = BigDecimal.ZERO;
        unknown.add("Financial exposure is not estimated: the twin holds no unit prices or order values.");

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
        riskIndicators.addAll(bomImpactDetails);
        riskIndicators.addAll(ttsDetails);
        riskIndicators.addAll(spofAlerts);

        List<String> assumptions = List.of(
                "Deterministic propagation over the twin's lanes; every lane is assumed to run at nominal transit time",
                "Requirement = throughput per hour x 24 x outage days of the first downstream manufacturing site",
                "Time to survive = the exposed site's own stock of the material / its daily throughput"
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
                unknown
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

