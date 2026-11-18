package com.supplygraph.application.planning;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.planning.port.out.PlanningScenarioRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.planning.PlanningPlanStatus;
import com.supplygraph.domain.model.planning.PlanningScenarioComparison;
import com.supplygraph.domain.model.planning.PlanningScenarioId;
import com.supplygraph.domain.model.planning.PlanningScenarioType;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PlanningScenarioService {

    private final PlanningScenarioRepositoryPort scenarioRepository;
    private final AuditGovernanceService auditGovernanceService;

    public PlanningScenarioService(
            PlanningScenarioRepositoryPort scenarioRepository,
            AuditGovernanceService auditGovernanceService
    ) {
        this.scenarioRepository = scenarioRepository;
        this.auditGovernanceService = auditGovernanceService;
    }

    public PlanningScenarioComparison createScenario(
            WorkspaceId workspaceId,
            String scenarioName,
            PlanningScenarioType type,
            BigDecimal projectedCost,
            double fillRate,
            double avgLeadTimeDays,
            double totalCo2Tons,
            String creatorUser
    ) {
        PlanningScenarioComparison scenario = new PlanningScenarioComparison(
                PlanningScenarioId.generate(),
                workspaceId,
                scenarioName,
                type,
                PlanningPlanStatus.EVALUATED,
                projectedCost,
                fillRate,
                avgLeadTimeDays,
                totalCo2Tons,
                creatorUser,
                Instant.now()
        );

        scenarioRepository.save(scenario);
        return scenario;
    }

    public PlanningScenarioComparison approvePlan(
            PlanningScenarioId scenarioId,
            UserProfile approver,
            String rationale
    ) {
        if (!approver.role().hasPermission(Permission.PLAN_APPROVE)) {
            throw new SecurityException("User " + approver.id().value() + " lacks required permission: " + Permission.PLAN_APPROVE);
        }

        PlanningScenarioComparison existing = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Planning scenario not found: " + scenarioId.value()));

        PlanningScenarioComparison approved = new PlanningScenarioComparison(
                existing.scenarioId(),
                existing.workspaceId(),
                existing.scenarioName(),
                existing.scenarioType(),
                PlanningPlanStatus.APPROVED,
                existing.totalProjectedCost(),
                existing.customerServiceFillRate(),
                existing.averageLeadTimeDays(),
                existing.carbonFootprintCo2Tons(),
                approver.id().value(),
                Instant.now()
        );

        scenarioRepository.save(approved);

        auditGovernanceService.logAction(
                approver.id(),
                existing.workspaceId(),
                AuditAction.PLAN_APPROVED,
                existing.scenarioId().value(),
                existing.status().name(),
                PlanningPlanStatus.APPROVED.name(),
                "Human approval handoff for supply chain plan: " + rationale,
                null
        );

        return approved;
    }

    public Optional<PlanningScenarioComparison> getScenario(PlanningScenarioId id) {
        return scenarioRepository.findById(id);
    }

    public List<PlanningScenarioComparison> listScenarios(WorkspaceId workspaceId) {
        return scenarioRepository.findByWorkspaceId(workspaceId);
    }
}
