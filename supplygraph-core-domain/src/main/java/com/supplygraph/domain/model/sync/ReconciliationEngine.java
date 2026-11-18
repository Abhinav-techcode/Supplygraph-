package com.supplygraph.domain.model.sync;

import java.util.Objects;

/** Pure three-way decision: external value vs SupplyGraph internal value vs the value at last successful sync. */
public final class ReconciliationEngine {
    public enum Decision { NO_CHANGE, AUTO_ACCEPT, REJECT_BY_POLICY, CONFLICT }

    private ReconciliationEngine() {}

    public static Decision decide(FieldAuthority authority, String external, String internal, String lastSynced) {
        if (Objects.equals(external, internal)) return Decision.NO_CHANGE;
        boolean externalChanged = !Objects.equals(external, lastSynced);
        boolean internalChanged = !Objects.equals(internal, lastSynced);
        if (externalChanged && internalChanged) return Decision.CONFLICT;       // diverged on both sides: never silently pick one
        if (!externalChanged) return Decision.NO_CHANGE;                        // internal is ahead of an unchanged source value
        return authority == FieldAuthority.EXTERNAL ? Decision.AUTO_ACCEPT : Decision.REJECT_BY_POLICY;
    }
}
