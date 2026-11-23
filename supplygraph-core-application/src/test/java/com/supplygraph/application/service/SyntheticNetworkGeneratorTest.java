package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.application.service.SyntheticNetworkGenerator.*;
import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Generated networks are valid, deterministic and honestly labelled")
class SyntheticNetworkGeneratorTest {
    private final SyntheticNetworkGenerator gen = new SyntheticNetworkGenerator();
    private final Map<WorkspaceId, DigitalTwinState> store = new HashMap<>();
    private final SupplyChainImportService importer = new SupplyChainImportService(new DigitalTwinRepositoryPort() {
        public void save(DigitalTwinState s) { store.put(s.getWorkspaceId(), s); }
        public Optional<DigitalTwinState> findByWorkspaceId(WorkspaceId w) { return Optional.ofNullable(store.get(w)); }
        public boolean existsByWorkspaceId(WorkspaceId w) { return store.containsKey(w); }
        public void reload(WorkspaceId w) {}
    }, new InventoryLedgerRepositoryPort() {
        public void recordTransaction(WorkspaceId w, InventoryTransaction t) {}
        public void updateBalance(WorkspaceId w, InventoryAccountId a, Quantity q) {}
        public Optional<Quantity> findBalance(WorkspaceId w, InventoryAccountId a) { return Optional.empty(); }
    });

    @Test
    @DisplayName("Every industry x region x size passes the real importer with no errors")
    void everyCombinationImports() {
        for (Industry i : Industry.values()) for (Region r : Region.values()) for (Size s : Size.values()) {
            var d = gen.generate(i, r, s, 7);
            var result = importer.importCsv(WorkspaceId.of("WS-GEN"), d.facilities(), d.lanes(), d.stock(), d.bom(), false);
            assertThat(result.errors()).as(i + "/" + r + "/" + s).isEmpty();
            assertThat(result.saved()).isTrue();
            assertThat(result.facilities()).isEqualTo(s == Size.SMALL ? 7 : s == Size.MEDIUM ? 13 : 20);
            assertThat(result.lanes()).isGreaterThan(result.facilities() - 1);
        }
    }

    @Test
    @DisplayName("Same seed gives the same network; a different seed gives a different one")
    void deterministic() {
        var a = gen.generate(Industry.AUTOMOTIVE, Region.GLOBAL, Size.MEDIUM, 42);
        assertThat(gen.generate(Industry.AUTOMOTIVE, Region.GLOBAL, Size.MEDIUM, 42).facilities()).isEqualTo(a.facilities());
        assertThat(gen.generate(Industry.AUTOMOTIVE, Region.GLOBAL, Size.MEDIUM, 43).facilities()).isNotEqualTo(a.facilities());
    }

    @Test
    @DisplayName("The summary says what is real and what is invented")
    void honestSummary() {
        assertThat(gen.generate(Industry.ELECTRONICS, Region.ASIA, Size.SMALL, 1).summary()).contains("invented").contains("estimated").contains("real");
    }

    @Test
    @DisplayName("Missing choices are reported as a validation error")
    void rejectsMissingChoice() {
        assertThatThrownBy(() -> gen.generate(null, Region.ASIA, Size.SMALL, 1)).isInstanceOf(ValidationException.class);
    }
}
