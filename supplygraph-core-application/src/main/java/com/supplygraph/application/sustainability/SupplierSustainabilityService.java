package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.SupplierEsgRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.sustainability.EsgScorecardId;
import com.supplygraph.domain.model.sustainability.SupplierEsgScorecard;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierSustainabilityService {

    private final SupplierEsgRepositoryPort supplierEsgRepository;

    public SupplierSustainabilityService(SupplierEsgRepositoryPort supplierEsgRepository) {
        this.supplierEsgRepository = supplierEsgRepository;
    }

    public SupplierEsgScorecard recordEsgAudit(
            WorkspaceId workspaceId,
            NodeId supplierId,
            double environmentalScore,
            double socialScore,
            double governanceScore,
            boolean cbamCompliant,
            boolean conflictMineralsCertified,
            boolean eudrCompliant
    ) {
        double composite = (environmentalScore * 0.40) + (socialScore * 0.30) + (governanceScore * 0.30);

        SupplierEsgScorecard scorecard = new SupplierEsgScorecard(
                EsgScorecardId.generate(),
                workspaceId,
                supplierId,
                environmentalScore,
                socialScore,
                governanceScore,
                composite,
                cbamCompliant,
                conflictMineralsCertified,
                eudrCompliant,
                Instant.now()
        );

        supplierEsgRepository.save(scorecard);
        return scorecard;
    }

    public Optional<SupplierEsgScorecard> getScorecard(EsgScorecardId id) {
        return supplierEsgRepository.findById(id);
    }

    public Optional<SupplierEsgScorecard> getScorecardForSupplier(WorkspaceId workspaceId, NodeId supplierId) {
        return supplierEsgRepository.findBySupplierId(workspaceId, supplierId);
    }

    public List<SupplierEsgScorecard> listScorecards(WorkspaceId workspaceId) {
        return supplierEsgRepository.findByWorkspaceId(workspaceId);
    }
}

