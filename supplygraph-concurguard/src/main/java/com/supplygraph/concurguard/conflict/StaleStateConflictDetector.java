package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * 6. Stale State Conflict Detector:
 * Flags intents formulated against obsolete digital twin versions.
 */
public class StaleStateConflictDetector {

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();

        for (ActionIntent intent : intents) {
            if (intent.expectedStateVersion().value() < snapshot.version().value()) {
                conflicts.add(Conflict.unary(
                        ConflictType.STALE_STATE_CONFLICT,
                        intent.id(),
                        "STATE_VERSION",
                        "Intent " + intent.id() + " was formulated against stale state " +
                                intent.expectedStateVersion() + ", but authoritative snapshot is now at " + snapshot.version(),
                        Severity.HIGH
                ));
            }
        }

        return conflicts;
    }
}
