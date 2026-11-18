package com.supplygraph.domain.model.sync;

import com.supplygraph.domain.model.connector.ConnectorCategory;
import org.junit.jupiter.api.Test;

import static com.supplygraph.domain.model.sync.FieldAuthority.*;
import static com.supplygraph.domain.model.sync.ReconciliationEngine.Decision.*;
import static com.supplygraph.domain.model.sync.ReconciliationEngine.decide;
import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationEngineTest {
    @Test void identicalValuesNeedNoChange() { assertThat(decide(EXTERNAL, "A", "A", "A")).isEqualTo(NO_CHANGE); }

    @Test void initialSyncAutoAcceptsWhenExternalIsAuthoritative() { assertThat(decide(EXTERNAL, "OPEN", null, null)).isEqualTo(AUTO_ACCEPT); }

    @Test void initialSyncIsRejectedByPolicyWhenInternalIsAuthoritative() { assertThat(decide(INTERNAL, "OPEN", null, null)).isEqualTo(REJECT_BY_POLICY); }

    @Test void externalOnlyChangeFollowsAuthority() {
        assertThat(decide(EXTERNAL, "SHIPPED", "OPEN", "OPEN")).isEqualTo(AUTO_ACCEPT);
        assertThat(decide(INTERNAL, "SHIPPED", "OPEN", "OPEN")).isEqualTo(REJECT_BY_POLICY);
    }

    @Test void internalAheadOfUnchangedExternalIsKept() { assertThat(decide(EXTERNAL, "OPEN", "HOLD", "OPEN")).isEqualTo(NO_CHANGE); }

    @Test void divergenceOnBothSidesIsAlwaysAConflictEvenWhenExternalIsAuthoritative() {
        assertThat(decide(EXTERNAL, "SHIPPED", "HOLD", "OPEN")).isEqualTo(CONFLICT);
        assertThat(decide(INTERNAL, "SHIPPED", "HOLD", "OPEN")).isEqualTo(CONFLICT);
    }

    @Test void defaultAuthorityFollowsCategoryOwnership() {
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.ERP, "PURCHASE_ORDER")).isEqualTo(EXTERNAL);
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.WMS, "STOCK")).isEqualTo(EXTERNAL);
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.TMS, "SHIPMENT")).isEqualTo(EXTERNAL);
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.WMS, "SHIPMENT")).isEqualTo(INTERNAL);
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.ERP, "RECOVERY_PLAN")).isEqualTo(INTERNAL);
        assertThat(FieldAuthorityPolicy.defaultAuthority(ConnectorCategory.ERP, "SIMULATION")).isEqualTo(INTERNAL);
    }
}
