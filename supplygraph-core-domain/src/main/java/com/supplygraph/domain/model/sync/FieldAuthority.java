package com.supplygraph.domain.model.sync;

/** Which side owns a field's truth. EXTERNAL fields may be auto-updated from the source system; INTERNAL fields are never overwritten by sync. */
public enum FieldAuthority { EXTERNAL, INTERNAL }
