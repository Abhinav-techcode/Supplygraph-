package com.supplygraph.domain.model.sync;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorCategory;

import java.util.Map;
import java.util.Set;

/** Workspace override of the default field-authority rules. field "*" matches any field of the entity type. */
public record FieldAuthorityPolicy(WorkspaceId workspaceId, String entityType, String field, FieldAuthority authority) {

    private static final Map<ConnectorCategory, Set<String>> DEFAULT_OWNED = Map.of(
            ConnectorCategory.ERP, Set.of("PURCHASE_ORDER"),
            ConnectorCategory.WMS, Set.of("STOCK"),
            ConnectorCategory.TMS, Set.of("SHIPMENT"),
            ConnectorCategory.SUPPLIER_SYSTEM, Set.of("SUPPLIER"),
            ConnectorCategory.MES, Set.of("PRODUCTION_LINE"),
            ConnectorCategory.QMS, Set.of("QUALITY_HOLD"),
            ConnectorCategory.PLM, Set.of("PART_REVISION"));

    /** Defaults: a connector is authoritative only for entity types its category owns. Everything else (incl. recovery plans, simulations) stays INTERNAL. */
    public static FieldAuthority defaultAuthority(ConnectorCategory category, String entityType) {
        return DEFAULT_OWNED.getOrDefault(category, Set.of()).contains(entityType) ? FieldAuthority.EXTERNAL : FieldAuthority.INTERNAL;
    }
}
