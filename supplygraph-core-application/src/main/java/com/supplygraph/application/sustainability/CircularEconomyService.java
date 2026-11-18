package com.supplygraph.application.sustainability;

import com.supplygraph.application.sustainability.port.out.CircularLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.sustainability.CircularFlowType;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntry;
import com.supplygraph.domain.model.sustainability.CircularLedgerEntryId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CircularEconomyService {

    private final CircularLedgerRepositoryPort circularLedgerRepository;

    public CircularEconomyService(CircularLedgerRepositoryPort circularLedgerRepository) {
        this.circularLedgerRepository = circularLedgerRepository;
    }

    public CircularLedgerEntry recordRecovery(
            WorkspaceId workspaceId,
            MaterialId materialId,
            CircularFlowType flowType,
            double intakeQuantity,
            double recoveryYieldPercentage
    ) {
        double recoveredQuantity = intakeQuantity * (recoveryYieldPercentage / 100.0);
        double virginAvoidanceKg = recoveredQuantity * 0.95; // 95% secondary material substitution factor

        CircularLedgerEntry entry = new CircularLedgerEntry(
                CircularLedgerEntryId.generate(),
                workspaceId,
                materialId,
                flowType,
                intakeQuantity,
                recoveryYieldPercentage,
                recoveredQuantity,
                virginAvoidanceKg,
                Instant.now()
        );

        circularLedgerRepository.save(entry);
        return entry;
    }

    public double getTotalVirginMaterialAvoidedKg(WorkspaceId workspaceId) {
        return circularLedgerRepository.findByWorkspaceId(workspaceId).stream()
                .mapToDouble(CircularLedgerEntry::virginMaterialAvoidedKg)
                .sum();
    }

    public List<CircularLedgerEntry> listLedgerEntries(WorkspaceId workspaceId) {
        return circularLedgerRepository.findByWorkspaceId(workspaceId);
    }
}

