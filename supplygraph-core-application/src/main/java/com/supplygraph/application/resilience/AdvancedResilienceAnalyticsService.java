package com.supplygraph.application.resilience;

import com.supplygraph.application.port.out.AdvancedResilienceRepositoryPort;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.ManufacturingSite;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.graph.SupplyChainNode;
import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;
import com.supplygraph.domain.model.resilience.advanced.NodeResilienceMetric;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Advanced enterprise resilience analytics evaluating Time-to-Survive (TTS),
 * Time-to-Recover (TTR), critical supplier exposure, and path redundancy.
 */
@Service
public class AdvancedResilienceAnalyticsService {

    /** Marks a figure the twin cannot supply. Shown as "not measured", never as a number. */
    public static final double NOT_MEASURED = -1.0;

    private static final Logger log = LoggerFactory.getLogger(AdvancedResilienceAnalyticsService.class);

    private final AdvancedResilienceRepositoryPort repository;
    private final DigitalTwinRepositoryPort twinRepository;

    public AdvancedResilienceAnalyticsService(
            AdvancedResilienceRepositoryPort repository,
            DigitalTwinRepositoryPort twinRepository
    ) {
        this.repository = repository;
        this.twinRepository = twinRepository;
    }

    /**
     * Assesses comprehensive resilience metrics across the current digital twin network.
     */
    public AdvancedResilienceMetrics evaluateWorkspaceResilience(String workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");

        DigitalTwinState state = twinRepository.findByWorkspaceId(WorkspaceId.of(workspaceId))
                .orElse(null);

        SupplyChainGraph graph = state != null ? state.getGraph() : null;
        List<NodeResilienceMetric> breakdown = new ArrayList<>();

        double sumTTS = 0.0;
        int measuredTTS = 0;
        int bottleneckCount = 0;
        double sumCriticality = 0.0;
        var balances = state != null ? state.createSnapshot().inventoryBalances() : java.util.Map.<com.supplygraph.domain.model.inventory.InventoryAccountId, com.supplygraph.domain.model.common.Quantity>of();

        // Everything below is read from the twin. Time-to-recover is NOT measurable (the twin holds no recovery-time data) and is reported as NOT_MEASURED.
        if (graph != null && !graph.getAllNodes().isEmpty()) {
            int total = graph.getNodeCount();
            for (SupplyChainNode node : graph.getAllNodes()) {
                NodeId nid = node.getId();
                int degree = graph.getOutgoingLanes(nid).size() + graph.getIncomingLanes(nid).size();

                // Single point of failure: some facility downstream has this node as its ONLY supplier of goods.
                boolean isBottleneck = false;
                for (NodeId succ : graph.getDirectSuccessors(nid)) {
                    if (graph.getDirectPredecessors(succ).size() == 1) { isBottleneck = true; break; }
                }
                if (isBottleneck) bottleneckCount++;

                // Time-to-survive: days a manufacturing site can run from its own stock, at its stated throughput. Other node types consume nothing, so it is not defined for them.
                double localTTS = NOT_MEASURED;
                if (node instanceof ManufacturingSite site && site.getMaxThroughputPerHour() > 0 && state != null) {
                    double onHand = 0;
                    for (var e : balances.entrySet())
                        if (e.getKey().nodeId().equals(nid)) onHand += e.getValue().amount().doubleValue();
                    localTTS = Math.round(onHand / (site.getMaxThroughputPerHour() * 24.0) * 10.0) / 10.0;
                    sumTTS += localTTS;
                    measuredTTS++;
                }

                // Criticality: the share of the rest of the network that sits downstream of this node.
                double criticality = total > 1 ? Math.round((graph.getDownstreamReachableNodes(nid).size() / (double) (total - 1)) * 100.0) / 100.0 : 0.0;
                sumCriticality += criticality;

                breakdown.add(NodeResilienceMetric.of(nid.value(), node.getName(), localTTS, NOT_MEASURED, degree, criticality));
            }
        }

        int count = breakdown.size();
        double avgTTS = measuredTTS > 0 ? Math.round((sumTTS / measuredTTS) * 10.0) / 10.0 : NOT_MEASURED;
        double avgTTR = NOT_MEASURED;
        double supplierExposure = count == 0 ? 0.0 : (double) bottleneckCount / count;   // share of facilities that are a single point of failure
        double flexibility = 1.0 - supplierExposure;
        // Structural resilience index: 100 x the share of facilities that are NOT a single point of failure. Purely a statement about the network's shape.
        double compositeScore = count == 0 ? 0.0 : 100.0 * flexibility;

        String assessmentId = "RES-ADV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AdvancedResilienceMetrics metrics = AdvancedResilienceMetrics.of(
                assessmentId,
                workspaceId,
                avgTTS,
                avgTTR,
                Math.round(supplierExposure * 100.0) / 100.0,
                Math.round(flexibility * 100.0) / 100.0,
                bottleneckCount,
                Math.round(compositeScore * 10.0) / 10.0,
                breakdown,
                Instant.now()
        );

        repository.save(metrics);
        log.info("Advanced resilience assessed for workspace {}: TTS={}d, TTR={}d, Score={}/100",
                workspaceId, avgTTS, avgTTR, metrics.compositeResilienceScore());
        return metrics;
    }

    public Optional<AdvancedResilienceMetrics> findLatestByWorkspaceId(String workspaceId) {
        return repository.findLatestByWorkspaceId(workspaceId);
    }

    public List<AdvancedResilienceMetrics> findAll() {
        return repository.findAll();
    }
}
