package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ResourceAliasRegistry;
import com.supplygraph.concurguard.model.ResourceIdentity;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * 4. Semantic Alias Conflict Detector:
 * Resolves divergent text aliases into canonical identities and flags hidden collisions.
 */
public class SemanticAliasConflictDetector {

    private final ResourceAliasRegistry aliasRegistry;

    public SemanticAliasConflictDetector(ResourceAliasRegistry aliasRegistry) {
        this.aliasRegistry = Objects.requireNonNull(aliasRegistry, "AliasRegistry must not be null");
    }

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();
        List<ActionIntent> intentList = new ArrayList<>(intents);

        for (int i = 0; i < intentList.size(); i++) {
            ActionIntent a = intentList.get(i);
            ResourceIdentity canonicalA = aliasRegistry.resolveOrFallback(a.targetEntityId(), "ENTITY", a.location());

            for (int j = i + 1; j < intentList.size(); j++) {
                ActionIntent b = intentList.get(j);
                ResourceIdentity canonicalB = aliasRegistry.resolveOrFallback(b.targetEntityId(), "ENTITY", b.location());

                // Different raw text strings pointing to the exact same canonical asset
                if (!a.targetEntityId().equalsIgnoreCase(b.targetEntityId()) && canonicalA.equals(canonicalB)) {
                    if (a.timeWindow() != null && b.timeWindow() != null && a.timeWindow().overlaps(b.timeWindow())) {
                        conflicts.add(Conflict.of(
                                ConflictType.SEMANTIC_ALIAS,
                                a.id(),
                                b.id(),
                                canonicalA.canonicalId(),
                                "Hidden semantic alias conflict: Intent " + a.id() + " ('" + a.targetEntityId() + "') and Intent " +
                                        b.id() + " ('" + b.targetEntityId() + "') both resolve to canonical asset " + canonicalA,
                                Severity.CRITICAL
                        ));
                    }
                }
            }
        }

        return conflicts;
    }
}
