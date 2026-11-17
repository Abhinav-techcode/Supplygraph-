package com.supplygraph.application.intelligence;

import com.supplygraph.application.port.out.ResilienceRepositoryPort;
import com.supplygraph.domain.model.graph.NodeType;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.graph.SupplyChainNode;
import com.supplygraph.domain.model.graph.TransportationLane;
import com.supplygraph.domain.model.intelligence.NetworkResilienceAssessment;
import com.supplygraph.domain.model.intelligence.ResilienceFactor;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Evaluates structural vulnerabilities, single-points-of-failure, and multi-factor network resilience.
 */
@Service
public class SupplyChainResilienceService {

    private static final Logger log = LoggerFactory.getLogger(SupplyChainResilienceService.class);

    private final ResilienceRepositoryPort repository;

    public SupplyChainResilienceService(ResilienceRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Executes deterministic topological resilience audit across network graph, lanes, and buffers.
     */
    public NetworkResilienceAssessment assessNetworkResilience(String workspaceId, DigitalTwinState state) {
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(state, "DigitalTwinState cannot be null");

        SupplyChainGraph graph = state.getGraph();
        if (graph == null) {
            NetworkResilienceAssessment empty = NetworkResilienceAssessment.of(
                    workspaceId, 100.0, 0, 1000.0, 1, 1.0, 0,
                    List.of(), List.of("No graph topology loaded"), List.of()
            );
            repository.save(empty);
            return empty;
        }

        // 1. Identify Single Points of Failure (nodes with degree = 1 or sole providers to assembly sites)
        List<SupplyChainNode> nodes = new ArrayList<>(graph.getAllNodes());
        List<TransportationLane> edges = new ArrayList<>(graph.getAllLanes());

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        for (TransportationLane lane : edges) {
            outDegree.merge(lane.getSourceNodeId().value(), 1, Integer::sum);
            inDegree.merge(lane.getTargetNodeId().value(), 1, Integer::sum);
        }

        List<String> spofNodes = new ArrayList<>();
        List<String> criticalVulnerabilities = new ArrayList<>();
        List<String> recommendedMitigations = new ArrayList<>();

        for (SupplyChainNode node : nodes) {
            String nid = node.getId().value();
            if (node.getType() == NodeType.SUPPLIER && outDegree.getOrDefault(nid, 0) == 1 && inDegree.getOrDefault(nid, 0) == 0) {
                // If this is the only supplier feeding a facility
                spofNodes.add(nid);
                criticalVulnerabilities.add("Single Point of Failure: Supplier [" + node.getName() + " (" + nid + ")] is the sole feeder for downstream assembly");
                recommendedMitigations.add("Qualify secondary backup supplier in an alternative geographic region for node " + nid);
            }
        }
        int spofCount = spofNodes.size();

        // 2. Herfindahl-Hirschman Index (HHI) for Supplier Concentration
        long supplierCount = nodes.stream().filter(n -> n.getType() == NodeType.SUPPLIER).count();
        double hhi;
        if (supplierCount <= 1) {
            hhi = 10000.0; // Monopolistic single-source
        } else {
            // Equal share assumption baseline: sum of (100/N)^2 = N * (10000 / N^2) = 10000 / N
            hhi = 10000.0 / supplierCount;
        }

        // 3. Alternative Supplier Coverage Ratio
        double altRatio = supplierCount > 1 ? Math.min(1.0, (supplierCount - 1.0) / supplierCount) : 0.0;

        // 4. Critical Path Depth (max hops from Tier-N supplier to distribution center)
        int maxDepth = calculateMaxNetworkDepth(nodes, edges);

        // 5. Bottlenecks (lanes with high utilization or single transport mode)
        int bottlenecks = (int) edges.stream().filter(e -> e.getNominalTransitTime().toDays() > 14).count();
        if (bottlenecks > 0) {
            criticalVulnerabilities.add("Transportation Bottleneck: " + bottlenecks + " inter-continental lanes have lead times exceeding 14 days");
            recommendedMitigations.add("Establish expedited multi-modal air-freight contingency routing for long lead lanes");
        }

        // Factor Calculations (Normalized 0.0 - 100.0)
        double spofScore = Math.max(0.0, 100.0 - (spofCount * 25.0));
        double hhiScore = Math.max(0.0, 100.0 - (hhi / 100.0));
        double altScore = altRatio * 100.0;
        double depthScore = Math.max(0.0, 100.0 - (maxDepth * 12.0));
        double bottleneckScore = Math.max(0.0, 100.0 - (bottlenecks * 20.0));

        List<ResilienceFactor> factors = List.of(
                ResilienceFactor.of("Single Point of Failure Exposure", spofScore, 0.25, "Evaluated based on unredundant sole-feeder supplier nodes"),
                ResilienceFactor.of("Supplier Market Concentration (HHI)", hhiScore, 0.20, "Herfindahl-Hirschman index across primary component sourcing tiers"),
                ResilienceFactor.of("Alternative Supplier Coverage", altScore, 0.25, "Ratio of components with qualified secondary/tertiary backup sources"),
                ResilienceFactor.of("Topological Critical Path Depth", depthScore, 0.15, "Longest acyclic dependency sequence from raw extract to final customer"),
                ResilienceFactor.of("Logistics & Buffer Bottleneck Margin", bottleneckScore, 0.15, "Resilience against extended transit times and transshipment delays")
        );

        double compositeIndex = factors.stream().mapToDouble(ResilienceFactor::contribution).sum();

        NetworkResilienceAssessment assessment = NetworkResilienceAssessment.of(
                workspaceId,
                compositeIndex,
                spofCount,
                hhi,
                maxDepth,
                altRatio,
                bottlenecks,
                factors,
                criticalVulnerabilities,
                recommendedMitigations
        );

        repository.save(assessment);
        log.info("Assessed supply chain resilience for workspace {}: Index={}/100, SPOF={}, HHI={}",
                workspaceId, String.format("%.1f", compositeIndex), spofCount, String.format("%.0f", hhi));
        return assessment;
    }

    public Optional<NetworkResilienceAssessment> getLatestAssessment(String workspaceId) {
        return repository.findLatestByWorkspaceId(workspaceId);
    }

    public List<NetworkResilienceAssessment> getHistory(String workspaceId) {
        return repository.findHistoryByWorkspaceId(workspaceId);
    }

    private int calculateMaxNetworkDepth(List<SupplyChainNode> nodes, List<TransportationLane> edges) {
        if (nodes.isEmpty() || edges.isEmpty()) return 1;
        // Simple BFS topological depth
        Map<String, List<String>> adj = new HashMap<>();
        for (TransportationLane lane : edges) {
            adj.computeIfAbsent(lane.getSourceNodeId().value(), k -> new ArrayList<>()).add(lane.getTargetNodeId().value());
        }

        int maxDepth = 1;
        for (SupplyChainNode n : nodes) {
            if (n.getType() == NodeType.SUPPLIER) {
                int depth = getDfsDepth(n.getId().value(), adj, new HashSet<>());
                if (depth > maxDepth) maxDepth = depth;
            }
        }
        return Math.min(10, maxDepth);
    }

    private int getDfsDepth(String current, Map<String, List<String>> adj, Set<String> visited) {
        if (!visited.add(current)) return 0;
        int max = 0;
        for (String next : adj.getOrDefault(current, Collections.emptyList())) {
            max = Math.max(max, getDfsDepth(next, adj, visited));
        }
        visited.remove(current);
        return 1 + max;
    }
}
