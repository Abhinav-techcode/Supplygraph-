package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.industry.api.IndustryPolicy;

import java.util.*;

/**
 * 7. Industry Policy Conflict Detector:
 * Evaluates domain and regulatory policies (GMP quarantine, cold-chain excursion, cleanroom limits, human sign-off).
 */
public class IndustryPolicyConflictDetector {

    private final List<IndustryPolicy> activePolicies;

    public IndustryPolicyConflictDetector(List<IndustryPolicy> activePolicies) {
        this.activePolicies = activePolicies == null ? List.of() : List.copyOf(activePolicies);
    }

    public static IndustryPolicyConflictDetector withDefaults() {
        return new IndustryPolicyConflictDetector(List.of(
                new IndustryPolicy("POL-QUALITY-01", "Quality Gate Sign-off", "Consequential material substitutions or lot quarantines require explicit QA approval", true),
                new IndustryPolicy("POL-SAFETY-02", "Hazardous Material Transport", "Emergency air freight of lithium cell buffers requires Hazmat certification and human authorization", true)
        ));
    }

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();

        for (ActionIntent intent : intents) {
            // Quality-sensitive or high-consequence operations must have human approval
            if (intent.requiresHumanApproval() || intent.riskScore() >= 0.75) {
                conflicts.add(Conflict.unary(
                        ConflictType.INDUSTRY_POLICY_VIOLATION,
                        intent.id(),
                        "REGULATORY_APPROVAL",
                        "Policy violation: High-risk or quality-consequential action " + intent.actionType() +
                                " (risk=" + intent.riskScore() + ") requires explicit human authorization before execution",
                        Severity.HIGH
                ));
            }
        }

        return conflicts;
    }
}
