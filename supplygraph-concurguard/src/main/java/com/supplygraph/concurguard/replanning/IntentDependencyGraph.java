package com.supplygraph.concurguard.replanning;

import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;

import java.util.*;

/**
 * Directed dependency graph across ActionIntents.
 * Computes transitive downstream dependent closures for minimal replanning subgraphs.
 */
public class IntentDependencyGraph {

    private final Map<IntentId, ActionIntent> intents = new LinkedHashMap<>();
    private final Map<IntentId, Set<IntentId>> directDependents = new HashMap<>(); // A -> {B where B depends on A}

    public IntentDependencyGraph(Collection<ActionIntent> actionIntents) {
        for (ActionIntent intent : actionIntents) {
            intents.put(intent.id(), intent);
            directDependents.put(intent.id(), new LinkedHashSet<>());
        }

        for (ActionIntent intent : actionIntents) {
            for (IntentId prereqId : intent.dependencies()) {
                if (directDependents.containsKey(prereqId)) {
                    directDependents.get(prereqId).add(intent.id());
                }
            }
        }
    }

    public Optional<ActionIntent> findIntent(IntentId id) {
        return Optional.ofNullable(intents.get(id));
    }

    /**
     * Computes the transitive downstream dependent closure for a set of invalidated root intents.
     * All intents in this closure must be replanned because their prerequisites were invalidated.
     */
    public Set<IntentId> computeTransitiveDependents(Collection<IntentId> rootInvalidated) {
        Set<IntentId> closure = new LinkedHashSet<>(rootInvalidated);
        Deque<IntentId> queue = new ArrayDeque<>(rootInvalidated);

        while (!queue.isEmpty()) {
            IntentId current = queue.poll();
            Set<IntentId> dependents = directDependents.getOrDefault(current, Set.of());
            for (IntentId dep : dependents) {
                if (closure.add(dep)) {
                    queue.add(dep);
                }
            }
        }

        return Collections.unmodifiableSet(closure);
    }
}
