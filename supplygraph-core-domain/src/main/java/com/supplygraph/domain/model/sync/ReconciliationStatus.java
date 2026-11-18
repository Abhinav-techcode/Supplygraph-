package com.supplygraph.domain.model.sync;

public enum ReconciliationStatus {
    /** Applied automatically because the source system is authoritative and SupplyGraph had not diverged. */
    AUTO_ACCEPTED,
    /** Both sides changed since the last sync; awaiting a human decision. */
    CONFLICT,
    /** External value ignored because SupplyGraph is authoritative for the field. */
    REJECTED_BY_POLICY,
    /** Conflict resolved by a human in favour of the external value. */
    ACCEPTED,
    /** Conflict resolved by a human keeping the internal value. */
    REJECTED;

    public boolean isOpen() { return this == CONFLICT; }
}
