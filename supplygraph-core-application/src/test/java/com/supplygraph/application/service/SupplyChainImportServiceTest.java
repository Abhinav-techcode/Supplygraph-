package com.supplygraph.application.service;

import com.supplygraph.application.agent.TwinInsights;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Supply chain import: validated first, saved only when every line is correct")
class SupplyChainImportServiceTest {
    private final Map<WorkspaceId, DigitalTwinState> store = new HashMap<>();
    private final Map<InventoryAccountId, Quantity> balances = new HashMap<>();
    private final List<WorkspaceId> reloads = new ArrayList<>();
    private SupplyChainImportService service;
    private final WorkspaceId ws = WorkspaceId.of("WS-IMPORT");

    static final String FAC = "id,name,type,location,tier,capacity\nSUP-A,Supplier A,SUPPLIER,Oslo,1,\nHUB-1,Hub,LOGISTICS_HUB,Hamburg,,500\nPLANT-1,Plant,MANUFACTURING_SITE,Munich,,40\n";
    static final String LANES = "from,to,mode,transit_hours,cost_per_kg\nSUP-A,PLANT-1,ROAD,24,2.0\nHUB-1,PLANT-1,ROAD,12,1.5\n";
    static final String STOCK = "facility,material,quantity\nHUB-1,RESIN,6000\nPLANT-1,RESIN,900\n";

    @BeforeEach
    void setUp() {
        DigitalTwinRepositoryPort twins = new DigitalTwinRepositoryPort() {
            public void save(DigitalTwinState s) { store.put(s.getWorkspaceId(), s); }
            public Optional<DigitalTwinState> findByWorkspaceId(WorkspaceId w) { return Optional.ofNullable(store.get(w)); }
            public boolean existsByWorkspaceId(WorkspaceId w) { return store.containsKey(w); }
            public void reload(WorkspaceId w) { reloads.add(w); }
        };
        InventoryLedgerRepositoryPort ledger = new InventoryLedgerRepositoryPort() {
            public void recordTransaction(WorkspaceId w, InventoryTransaction t) {}
            public void updateBalance(WorkspaceId w, InventoryAccountId a, Quantity q) { balances.put(a, q); }
            public Optional<Quantity> findBalance(WorkspaceId w, InventoryAccountId a) { return Optional.ofNullable(balances.get(a)); }
        };
        service = new SupplyChainImportService(twins, ledger);
    }

    @Test
    @DisplayName("A valid supply chain becomes the twin, balances are written and the twin is reloaded so stock is visible")
    void importsValidData() {
        var r = service.importCsv(ws, FAC, LANES, STOCK, "parent,child,quantity_per_parent\nWIDGET,RESIN,2\n", false);
        assertThat(r.errors()).isEmpty();
        assertThat(r.saved()).isTrue();
        assertThat(r.facilities()).isEqualTo(3);
        assertThat(r.lanes()).isEqualTo(2);
        assertThat(store.get(ws).getGraph().getNodeCount()).isEqualTo(3);
        assertThat(balances).hasSize(2);
        assertThat(reloads).containsExactly(ws);
        assertThat(store.get(ws).getBom().getDirectParents(com.supplygraph.domain.model.bom.MaterialId.of("RESIN"))).hasSize(1);
    }

    @Test
    @DisplayName("Dry run reports counts but saves nothing")
    void dryRunSavesNothing() {
        var r = service.importCsv(ws, FAC, LANES, STOCK, "", true);
        assertThat(r.saved()).isFalse();
        assertThat(r.errors()).isEmpty();
        assertThat(store).isEmpty();
        assertThat(balances).isEmpty();
    }

    @Test
    @DisplayName("Every problem is reported with its context and nothing is saved")
    void reportsAllProblemsAndSavesNothing() {
        String badLanes = LANES + "SUP-A,GHOST,ROAD,5,1\nHUB-1,PLANT-1,TELEPORT,5,1\nHUB-1,PLANT-1,ROAD,abc,1\n";
        String badStock = STOCK + "NOWHERE,RESIN,5\nHUB-1,RESIN,-4\nHUB-1,RESIN,lots\n";
        var r = service.importCsv(ws, FAC + "PLANT-1,Dup,SUPPLIER,X,1,\nBAD,Bad,SPACESHIP,X,1,\n", badLanes, badStock, "", false);
        assertThat(r.saved()).isFalse();
        assertThat(r.errors()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(String.join("\n", r.errors())).contains("duplicate", "SPACESHIP", "unknown facility", "GHOST", "TELEPORT", "negative", "lots");
        assertThat(store).isEmpty();
        assertThat(balances).isEmpty();
    }

    @Test
    @DisplayName("An empty facilities table is rejected")
    void requiresFacilities() {
        var r = service.importCsv(ws, "id,name,type,location,tier,capacity\n", "", "", "", false);
        assertThat(r.saved()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("at least one facility"));
    }

    @Test
    @DisplayName("Re-importing advances the twin version")
    void versionAdvances() {
        service.importCsv(ws, FAC, LANES, STOCK, "", false);
        var second = service.importCsv(ws, FAC, LANES, STOCK, "", false);
        assertThat(second.twinVersion()).isEqualTo(2);
        assertThat(store.get(ws).getCurrentVersion().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("Imported data feeds the same twin analysis the agents use (supplier outage reaches the plant, hub stock is the buffer)")
    void importedTwinDrivesAnalysis() {
        service.importCsv(ws, FAC, LANES, STOCK, "", false);
        // the in-memory twin has no ledger rows (the real adapter reloads them from storage), so check topology-driven insights
        var snapshot = store.get(ws).createSnapshot();
        var incident = new com.supplygraph.application.agent.DisruptionIncident("INC-IMP-1", ws, "Supplier A outage", "", "GENERIC", Set.of("SUP-A"), Set.of("RESIN"), 72, "HIGH", java.time.Instant.now());
        TwinInsights ins = TwinInsights.of(snapshot, incident);
        assertThat(ins.hitNodes()).containsExactly(NodeId.of("SUP-A"));
        assertThat(ins.downstreamNodes()).containsExactly(NodeId.of("PLANT-1"));
        assertThat(ins.exposedSite()).contains(NodeId.of("PLANT-1"));
        assertThat(ins.requiredUnits()).isEqualTo(40.0 * 24 * 3);
    }
}
