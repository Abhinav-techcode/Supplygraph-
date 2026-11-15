package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * 5. Dependency Conflict Detector:
 * Identifies cyclic dependency deadlocks and unresolved missing dependencies.
 */
public class DependencyConflictDetector {

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();
        Map<IntentId, ActionIntent> map = new LinkedHashMap<>();
        for (ActionIntent i : intents) {
            map.put(i.id(), i);
        }

        // 1. Missing dependencies
        for (ActionIntent i : intents) {
            for (IntentId dep : i.dependencies()) {
                if (!map.containsKey(dep)) {
                    conflicts.add(Conflict.unary(
                            ConflictType.DEPENDENCY_CONFLICT,
                            i.id(),
                            dep.value(),
                            "Unresolved dependency: Intent " + i.id() + " depends on absent intent " + dep,
                            Severity.HIGH
                    ));
                }
            }
        }

        // 2. Cycle detection via 3-color DFS
        Map<IntentId, Integer> visitState = new HashMap<>(); // 0: unvisited, 1: visiting, 2: visited
        List<IntentId> currentPath = new ArrayList<>();

        for (IntentId root : map.keySet()) {
            if (visitState.getOrDefault(root, 0) == 0) {
                dfsCycle(root, map, visitState, currentPath, conflicts);
            }
        }

        return conflicts;
    }

    private void dfsCycle(IntentId current, Map<IntentId, ActionIntent> map,
                          Map<IntentId, Integer> visitState, List<IntentId> path,
                          List<Conflict> conflicts) {
        visitState.put(current, 1);
        path.add(current);

        ActionIntent intent = map.get(current);
        if (intent != null) {
            for (IntentId next : intent.dependencies()) {
                if (!map.containsKey(next)) {
                    continue;
                }
                int state = visitState.getOrDefault(next, 0);
                if (state == 1) { // Cycle detected!
                    int cycleStart = path.indexOf(next);
                    List<IntentId> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(next);

                    conflicts.add(Conflict.of(
                            ConflictType.DEPENDENCY_CONFLICT,
                            current,
                            next,
                            "INTENT_DEPENDENCY_CYCLE",
                            "Deadlock circular dependency detected among intents: " + cycle,
                            Severity.CRITICAL
                    ));
                } else if (state == 0) {
                    dfsCycle(next, map, visitState, path, conflicts);
                }
            }
        }

        path.remove(path.size() - 1);
        visitState.put(current, 2);
    }
}
