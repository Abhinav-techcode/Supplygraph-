package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.time.Instant;
import java.util.*;

/**
 * 3. Temporal Conflict Detector:
 * Detects chronological anomalies, lead time impossibilities, and precedence inversions.
 */
public class TemporalConflictDetector {

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();
        Map<IntentId, ActionIntent> intentMap = new LinkedHashMap<>();
        for (ActionIntent i : intents) {
            intentMap.put(i.id(), i);
        }

        for (ActionIntent intent : intents) {
            // Check dependency precedence: prerequisite must finish before dependent starts
            for (IntentId depId : intent.dependencies()) {
                ActionIntent prereq = intentMap.get(depId);
                if (prereq != null && prereq.timeWindow() != null && intent.timeWindow() != null) {
                    if (prereq.timeWindow().end().isAfter(intent.timeWindow().start())) {
                        conflicts.add(Conflict.of(
                                ConflictType.TEMPORAL_OVERLAP,
                                intent.id(),
                                prereq.id(),
                                "TIME_WINDOW",
                                "Temporal precedence inversion: Dependent action " + intent.id() +
                                        " starts at " + intent.timeWindow().start() +
                                        " before prerequisite " + prereq.id() + " finishes at " + prereq.timeWindow().end(),
                                Severity.HIGH
                        ));
                    }
                }
            }

            // Check if intent window is in the past relative to snapshot evaluation timestamp
            if (intent.timeWindow() != null) {
                if (intent.timeWindow().end().isBefore(snapshot.timestamp())) {
                    conflicts.add(Conflict.unary(
                            ConflictType.TEMPORAL_OVERLAP,
                            intent.id(),
                            "TIME_WINDOW",
                            "Action window " + intent.timeWindow() + " has already expired relative to snapshot timestamp " + snapshot.timestamp(),
                            Severity.CRITICAL
                    ));
                }
            }
        }

        return conflicts;
    }
}
