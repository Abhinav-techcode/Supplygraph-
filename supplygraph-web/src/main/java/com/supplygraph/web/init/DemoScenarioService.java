package com.supplygraph.web.init;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.application.service.DisruptionApplicationService;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Demo helper: takes one of the seeded candidate plans through the REAL validation (ConcurGuard) and approval steps, as the current
 * operator, so the execution views have an approved plan to work with. It uses the same services and the same permission as the
 * Decision Centre; it does not bypass any rule and the approval is recorded under the acting user.
 */
@Service
public class DemoScenarioService {
    private final RecoveryPlanningApplicationService planning;
    private final WorkspaceAccessGuard guard;
    private final DisruptionApplicationService disruptions;

    public DemoScenarioService(RecoveryPlanningApplicationService planning, WorkspaceAccessGuard guard, DisruptionApplicationService disruptions) {
        this.disruptions = disruptions;
        this.planning = planning;
        this.guard = guard;
    }

    /** @return the plan that is now APPROVED (a plan that already was approved is returned as is). */
    public RecoveryPlan prepareApprovedPlan(WorkspaceId ws, java.util.Set<String> alreadyConverted) {
        guard.enforceWorkspaceAndPermission(ws, Permission.PLAN_APPROVE);
        UserProfile user = SecurityContext.getCurrentUser();
        List<RecoveryPlan> existing = planning.listPlans(ws);
        for (RecoveryPlan p : existing) if (p.status() == RecoveryPlanStatus.APPROVED && !alreadyConverted.contains(p.id().value())) return p;
        if (existing.isEmpty()) {   // same call the Decision Centre's "Generate Candidate Alternatives" button makes
            var disruption = disruptions.listDisruptions(ws).stream().findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No disruption exists in this workspace to plan a recovery for."));
            planning.generateDefaultAlternatives(disruption.id(), ws);
        }
        final List<RecoveryPlan> plans = planning.listPlans(ws);
        RecoveryPlan candidate = plans.stream().filter(p -> p.status() == RecoveryPlanStatus.PENDING_APPROVAL).findFirst()
                .or(() -> plans.stream().filter(p -> p.status() == RecoveryPlanStatus.DRAFT || p.status() == RecoveryPlanStatus.SIMULATED || p.status() == RecoveryPlanStatus.VALIDATED).findFirst())
                .orElseThrow(() -> new NoSuchElementException("No candidate recovery plan is available. Generate alternatives in the Decision Centre first."));
        try {
            return validateAndApprove(candidate, user);
        } catch (IllegalStateException stale) {
            if (stale.getMessage() == null || !stale.getMessage().startsWith("Stale plan")) throw stale;
            // The seeded candidates were built on an older twin version. Do what an operator would: generate fresh alternatives, then validate + approve one.
            var disruption = disruptions.listDisruptions(ws).stream().findFirst().orElseThrow(() -> stale);
            java.util.Set<String> before = new java.util.HashSet<>();
            planning.listPlans(ws).forEach(p -> before.add(p.id().value()));
            planning.generateDefaultAlternatives(disruption.id(), ws);
            RecoveryPlan fresh = planning.listPlans(ws).stream().filter(p -> !before.contains(p.id().value())).findFirst().orElseThrow(() -> stale);
            return validateAndApprove(fresh, user);
        }
    }

    private RecoveryPlan validateAndApprove(RecoveryPlan candidate, UserProfile user) {
        if (candidate.status() != RecoveryPlanStatus.PENDING_APPROVAL) {
            candidate = planning.validatePlanWithConcurGuard(candidate.id());
            if (candidate.status() != RecoveryPlanStatus.PENDING_APPROVAL)
                throw new IllegalStateException("ConcurGuard found conflicts, so the plan was not approved: " + candidate.concurGuardConflictIds());
        }
        return planning.approvePlan(candidate.id(), user.id().value(), "Approved by " + user.displayName() + " via the demo helper (same checks as the Decision Centre)");
    }
}
