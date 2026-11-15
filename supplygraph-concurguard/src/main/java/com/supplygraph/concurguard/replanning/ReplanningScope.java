package com.supplygraph.concurguard.replanning;

import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.model.IntentId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal affected subgraph isolating preserved intents from invalidated intents.
 */
public record ReplanningScope(
        Set<IntentId> preservedIntents,
        Set<IntentId> invalidatedIntents,
        List<Conflict> triggeringConflicts,
        int iteration
) {
    public ReplanningScope {
        preservedIntents = preservedIntents == null ? Set.of() : Set.copyOf(preservedIntents);
        invalidatedIntents = invalidatedIntents == null ? Set.of() : Set.copyOf(invalidatedIntents);
        triggeringConflicts = triggeringConflicts == null ? List.of() : List.copyOf(triggeringConflicts);
    }

    public boolean isClean() {
        return invalidatedIntents.isEmpty();
    }
}
