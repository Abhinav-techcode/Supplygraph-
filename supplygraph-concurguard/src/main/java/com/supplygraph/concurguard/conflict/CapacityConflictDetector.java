package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.CapacityClaim;
import com.supplygraph.concurguard.model.ClaimType;
import com.supplygraph.concurguard.model.ResourceClaim;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.*;

/**
 * 2. Capacity Conflict Detector:
 * Detects when aggregate machine/line claims in overlapping half-open windows exceed available capacity.
 */
public class CapacityConflictDetector {

    public List<Conflict> detect(Collection<ActionIntent> intents, DigitalTwinSnapshot snapshot) {
        List<Conflict> conflicts = new ArrayList<>();
        List<ActionIntent> intentList = new ArrayList<>(intents);

        for (int i = 0; i < intentList.size(); i++) {
            ActionIntent a = intentList.get(i);
            for (int j = i + 1; j < intentList.size(); j++) {
                ActionIntent b = intentList.get(j);

                // Check capacity claim overlap
                if (a.capacityClaim() != null && b.capacityClaim() != null) {
                    CapacityClaim ca = a.capacityClaim();
                    CapacityClaim cb = b.capacityClaim();

                    if (ca.resourceId().equals(cb.resourceId()) && ca.window().overlaps(cb.window())) {
                        conflicts.add(Conflict.of(
                                ConflictType.CAPACITY_CONFLICT,
                                a.id(),
                                b.id(),
                                ca.resourceId().value(),
                                "Overlapping capacity reservation on resource " + ca.resourceId() +
                                        " during window " + ca.window().intersection(cb.window()).orElse(ca.window()),
                                Severity.HIGH
                        ));
                    }
                }

                // Check exclusive resource claims
                for (ResourceClaim claimA : a.resourceClaims()) {
                    for (ResourceClaim claimB : b.resourceClaims()) {
                        if (claimA.resourceIdentity().equals(claimB.resourceIdentity()) &&
                                (claimA.claimType() == ClaimType.EXCLUSIVE || claimB.claimType() == ClaimType.EXCLUSIVE)) {
                            if (claimA.claimWindow().overlaps(claimB.claimWindow())) {
                                conflicts.add(Conflict.of(
                                        ConflictType.CAPACITY_CONFLICT,
                                        a.id(),
                                        b.id(),
                                        claimA.resourceIdentity().canonicalId(),
                                        "Exclusive claim collision on " + claimA.resourceIdentity() +
                                                " during overlapping window " + claimA.claimWindow(),
                                        Severity.CRITICAL
                                ));
                            }
                        }
                    }
                }
            }
        }

        return conflicts;
    }
}
