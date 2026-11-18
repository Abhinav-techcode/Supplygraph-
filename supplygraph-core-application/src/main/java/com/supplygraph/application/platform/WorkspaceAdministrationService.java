package com.supplygraph.application.platform;

import com.supplygraph.application.platform.port.out.WorkspaceQuotaRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaId;
import com.supplygraph.domain.model.platform.WorkspaceQuotaRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class WorkspaceAdministrationService {

    private final WorkspaceQuotaRepositoryPort quotaRepository;

    public WorkspaceAdministrationService(WorkspaceQuotaRepositoryPort quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    public WorkspaceQuotaRecord getOrCreateQuota(WorkspaceId workspaceId) {
        return quotaRepository.findByWorkspaceId(workspaceId)
                .orElseGet(() -> {
                    WorkspaceQuotaRecord defaultQuota = new WorkspaceQuotaRecord(
                            WorkspaceQuotaId.generate(),
                            workspaceId,
                            1000,
                            50,
                            10,
                            true,
                            Instant.now()
                    );
                    quotaRepository.save(defaultQuota);
                    return defaultQuota;
                });
    }

    public WorkspaceQuotaRecord configureQuota(
            WorkspaceId workspaceId,
            int maxNodes,
            int maxActiveDisruptions,
            int maxConcurrentSimulations
    ) {
        WorkspaceQuotaRecord existing = getOrCreateQuota(workspaceId);
        WorkspaceQuotaRecord updated = existing.withLimits(maxNodes, maxActiveDisruptions, maxConcurrentSimulations);
        quotaRepository.save(updated);
        return updated;
    }

    public WorkspaceQuotaRecord toggleWorkspace(WorkspaceId workspaceId, boolean enabled) {
        WorkspaceQuotaRecord existing = getOrCreateQuota(workspaceId);
        WorkspaceQuotaRecord updated = existing.withEnabled(enabled);
        quotaRepository.save(updated);
        return updated;
    }

    public boolean checkQuotaAllowance(WorkspaceId workspaceId, String resourceType, int currentCount) {
        WorkspaceQuotaRecord quota = getOrCreateQuota(workspaceId);
        if (!quota.enabled()) {
            return false;
        }

        return switch (resourceType.toUpperCase()) {
            case "NODES" -> currentCount < quota.maxNodes();
            case "DISRUPTIONS" -> currentCount < quota.maxActiveDisruptions();
            case "SIMULATIONS" -> currentCount < quota.maxConcurrentSimulations();
            default -> true;
        };
    }
}

