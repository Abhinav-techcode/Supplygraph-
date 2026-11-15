package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ResourceAliasRegistry;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * Unified deterministic conflict coordinator evaluating all 7 canonical conflict classes.
 */
public class ConcurGuardConflictDetector {

    private final QuantityConflictDetector quantityDetector;
    private final CapacityConflictDetector capacityDetector;
    private final TemporalConflictDetector temporalDetector;
    private final SemanticAliasConflictDetector semanticAliasDetector;
    private final DependencyConflictDetector dependencyDetector;
    private final StaleStateConflictDetector staleStateDetector;
    private final IndustryPolicyConflictDetector industryPolicyDetector;

    public ConcurGuardConflictDetector(ResourceAliasRegistry aliasRegistry) {
        this.quantityDetector = new QuantityConflictDetector();
        this.capacityDetector = new CapacityConflictDetector();
        this.temporalDetector = new TemporalConflictDetector();
        this.semanticAliasDetector = new SemanticAliasConflictDetector(aliasRegistry);
        this.dependencyDetector = new DependencyConflictDetector();
        this.staleStateDetector = new StaleStateConflictDetector();
        this.industryPolicyDetector = IndustryPolicyConflictDetector.withDefaults();
    }

    public List<Conflict> detectAllConflicts(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> allConflicts = new ArrayList<>();
        allConflicts.addAll(quantityDetector.detect(intents, snapshot));
        allConflicts.addAll(capacityDetector.detect(intents, snapshot));
        allConflicts.addAll(temporalDetector.detect(intents, snapshot));
        allConflicts.addAll(semanticAliasDetector.detect(intents, snapshot));
        allConflicts.addAll(dependencyDetector.detect(intents, snapshot));
        allConflicts.addAll(staleStateDetector.detect(intents, snapshot));
        allConflicts.addAll(industryPolicyDetector.detect(intents, snapshot));
        return Collections.unmodifiableList(allConflicts);
    }
}
