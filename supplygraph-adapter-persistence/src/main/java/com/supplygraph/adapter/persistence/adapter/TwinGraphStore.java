package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Durable storage of the digital twin's graph, bill of materials and inventory balances in the V2 tables
 * (supply_chain_nodes, transportation_lanes, materials, bom_entries, inventory_balances).
 * The twin is saved as a whole (replace-all per workspace) inside one transaction and rebuilt from these rows at load time.
 * Node subtype attributes are stored in details_json as URL-encoded key=value pairs.
 */
@org.springframework.stereotype.Component
public class TwinGraphStore {
    private final JdbcTemplate jdbc;

    public TwinGraphStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void save(WorkspaceId ws, DigitalTwinState state) {
        String w = ws.value();
        jdbc.update("DELETE FROM transportation_lanes WHERE workspace_id = ?", w);
        jdbc.update("DELETE FROM supply_chain_nodes WHERE workspace_id = ?", w);
        jdbc.update("DELETE FROM bom_entries WHERE workspace_id = ?", w);
        jdbc.update("DELETE FROM materials WHERE workspace_id = ?", w);
        Timestamp now = Timestamp.from(Instant.now());
        SupplyChainGraph graph = state.getGraph();
        for (SupplyChainNode n : graph.getAllNodes()) {
            jdbc.update("INSERT INTO supply_chain_nodes (id, workspace_id, name, node_type, location, is_active, details_json, created_at) VALUES (?,?,?,?,?,?,?,?)",
                    scoped(w, n.getId().value()), w, n.getName(), n.getType().name(), n.getLocation(), n.isActive(), enc(details(n)), now);
        }
        for (TransportationLane l : graph.getAllLanes()) {
            jdbc.update("INSERT INTO transportation_lanes (id, workspace_id, source_node_id, target_node_id, transport_mode, transit_duration_seconds, cost_per_kg, is_operational) VALUES (?,?,?,?,?,?,?,?)",
                    scoped(w, l.getId().value()), w, scoped(w, l.getSourceNodeId().value()), scoped(w, l.getTargetNodeId().value()), l.getTransportMode().name(),
                    l.getNominalTransitTime().getSeconds(), l.getCostPerKg(), l.isOperational());
        }
        BillOfMaterials bom = state.getBom();
        for (Material m : bom.getAllMaterials()) {
            Map<String, String> md = new TreeMap<>();
            if (m instanceof Component c) md.put("leadDays", "" + c.getProcurementLeadTimeDays());
            if (m instanceof FinishedProduct f) { md.put("line", f.getProductLine() == null ? "" : f.getProductLine()); md.put("warranty", "" + f.getWarrantyMonths()); }
            jdbc.update("INSERT INTO materials (id, workspace_id, sku, name, material_type, base_unit, details_json) VALUES (?,?,?,?,?,?,?)",
                    scoped(w, m.getId().value()), w, m.getSku(), m.getName(), m.getType().name(), m.getBaseUnit().name(), enc(md));
        }
        int pos = 0;
        for (BomEntry e : bom.getAllEntries()) {
            jdbc.update("INSERT INTO bom_entries (id, workspace_id, parent_material_id, child_material_id, quantity, unit, scrap_rate, position) VALUES (?,?,?,?,?,?,?,?)",
                    w + ":bom:" + (pos++), w, scoped(w, e.parentMaterialId().value()), scoped(w, e.childMaterialId().value()),
                    e.quantityPerParent().amount(), e.quantityPerParent().unit().name(), e.scrapRate(), e.position());
        }
        // Inventory balances: same id scheme as PostgresInventoryLedgerRepositoryAdapter so both writers address the same rows.
        for (Map.Entry<InventoryAccountId, Quantity> b : state.getInventoryLedger().getAllBalances().entrySet()) {
            InventoryAccountId a = b.getKey();
            String id = w + ":" + a.nodeId().value() + ":" + a.materialId().value() + ":" + a.category().name() + a.lotNumber().map(l -> ":" + l.value()).orElse("");
            int n = jdbc.update("UPDATE inventory_balances SET balance_amount = ?, unit = ?, updated_at = ? WHERE id = ?", b.getValue().amount(), b.getValue().unit().name(), now, id);
            if (n == 0) jdbc.update("INSERT INTO inventory_balances (id, workspace_id, node_id, material_id, category, lot_number, balance_amount, unit, updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                    id, w, a.nodeId().value(), a.materialId().value(), a.category().name(), a.lotNumber().map(LotNumber::value).orElse(null), b.getValue().amount(), b.getValue().unit().name(), now);
        }
    }

    /** True when graph rows exist for the workspace (otherwise only a version row exists). */
    @Transactional(readOnly = true)
    public boolean hasGraph(WorkspaceId ws) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM supply_chain_nodes WHERE workspace_id = ?", Integer.class, ws.value());
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public DigitalTwinState load(WorkspaceId ws, long version) {
        String w = ws.value();
        List<SupplyChainNode> nodes = jdbc.query("SELECT * FROM supply_chain_nodes WHERE workspace_id = ? ORDER BY created_at, id", (r, i) -> node(w, r), w);
        List<TransportationLane> lanes = jdbc.query("SELECT * FROM transportation_lanes WHERE workspace_id = ? ORDER BY id", (r, i) -> new TransportationLane(
                EdgeId.of(unscoped(w, r.getString("id"))), NodeId.of(unscoped(w, r.getString("source_node_id"))), NodeId.of(unscoped(w, r.getString("target_node_id"))),
                TransportMode.valueOf(r.getString("transport_mode")), Duration.ofSeconds(r.getLong("transit_duration_seconds")), r.getBigDecimal("cost_per_kg"), r.getBoolean("is_operational")), w);
        List<Material> materials = jdbc.query("SELECT * FROM materials WHERE workspace_id = ? ORDER BY id", (r, i) -> material(w, r), w);
        List<BomEntry> entries = jdbc.query("SELECT * FROM bom_entries WHERE workspace_id = ? ORDER BY position, id", (r, i) -> new BomEntry(
                MaterialId.of(unscoped(w, r.getString("parent_material_id"))), MaterialId.of(unscoped(w, r.getString("child_material_id"))),
                Quantity.of(r.getBigDecimal("quantity"), UnitOfMeasure.valueOf(r.getString("unit"))), r.getBigDecimal("scrap_rate"), r.getInt("position")), w);
        DigitalTwinState state = new DigitalTwinState(ws, StateVersion.of(version), new SupplyChainGraph(nodes, lanes), new BillOfMaterials(materials, entries));
        // Opening balances: one balanced transaction per persisted balance (debit the account, credit the external offset).
        jdbc.query("SELECT * FROM inventory_balances WHERE workspace_id = ?", rs -> {
            BigDecimal amount = rs.getBigDecimal("balance_amount");
            if (amount.signum() <= 0) return;
            UnitOfMeasure unit = UnitOfMeasure.valueOf(rs.getString("unit"));
            NodeId node = NodeId.of(rs.getString("node_id"));
            MaterialId mat = MaterialId.of(rs.getString("material_id"));
            String lot = rs.getString("lot_number");
            BalanceCategory cat = BalanceCategory.valueOf(rs.getString("category"));
            if (cat == BalanceCategory.EXTERNAL_OFFSET) return;   // contra-account, recreated by the opening transaction
            InventoryAccountId account = lot == null ? InventoryAccountId.of(node, mat, cat) : InventoryAccountId.of(node, mat, cat, new LotNumber(lot));
            InventoryAccountId offset = InventoryAccountId.of(node, mat, BalanceCategory.EXTERNAL_OFFSET);
            Quantity q = Quantity.of(amount, unit);
            state.getInventoryLedger().postTransaction(new InventoryTransaction(TransactionId.generate(), Instant.now(), "OPENING_BALANCE", rs.getString("id"),
                    List.of(new InventoryPosting(UUID.randomUUID().toString(), account, PostingType.DEBIT, q),
                            new InventoryPosting(UUID.randomUUID().toString(), offset, PostingType.CREDIT, q))));
        }, w);
        return state;
    }

    // ---- node mapping
    private static Map<String, String> details(SupplyChainNode n) {
        Map<String, String> d = new TreeMap<>();
        if (n instanceof Supplier s) { d.put("tier", "" + s.getTier()); d.put("certifications", String.join(",", new TreeSet<>(s.getCertifications()))); d.put("riskScore", "" + s.getRiskScore()); }
        else if (n instanceof ManufacturingSite m) { d.put("lines", String.join(",", new TreeSet<>(m.getProductionLines()))); d.put("cleanRoom", m.getCleanRoomClass() == null ? "" : m.getCleanRoomClass()); d.put("throughput", "" + m.getMaxThroughputPerHour()); }
        else if (n instanceof Warehouse wh) { d.put("area", wh.getStorageCapacitySquareMeters().toPlainString()); d.put("cold", "" + wh.isColdStorageSupported()); d.put("hazmat", "" + wh.isHazardousMaterialAuthorized()); }
        else if (n instanceof LogisticsHub h) { d.put("crossDock", "" + h.isCrossDockingSupported()); d.put("pallets", "" + h.getDailyThroughputPallets()); d.put("modes", String.join(",", new TreeSet<>(h.getSupportedFreightModes()))); }
        return d;
    }

    private static SupplyChainNode node(String w, java.sql.ResultSet r) throws java.sql.SQLException {
        NodeId id = NodeId.of(unscoped(w, r.getString("id")));
        String name = r.getString("name"), loc = r.getString("location");
        boolean active = r.getBoolean("is_active");
        Map<String, String> d = dec(r.getString("details_json"));
        return switch (NodeType.valueOf(r.getString("node_type"))) {
            case SUPPLIER -> new Supplier(id, name, loc, active, Integer.parseInt(d.getOrDefault("tier", "1")), set(d.get("certifications")), Double.parseDouble(d.getOrDefault("riskScore", "0")));
            case MANUFACTURING_SITE -> new ManufacturingSite(id, name, loc, active, set(d.get("lines")), d.get("cleanRoom"), Integer.parseInt(d.getOrDefault("throughput", "0")));
            case WAREHOUSE -> new Warehouse(id, name, loc, active, new BigDecimal(d.getOrDefault("area", "0")), Boolean.parseBoolean(d.get("cold")), Boolean.parseBoolean(d.get("hazmat")));
            case LOGISTICS_HUB -> new LogisticsHub(id, name, loc, active, Boolean.parseBoolean(d.get("crossDock")), Integer.parseInt(d.getOrDefault("pallets", "0")), set(d.get("modes")));
        };
    }

    private static Material material(String w, java.sql.ResultSet r) throws java.sql.SQLException {
        MaterialId id = MaterialId.of(unscoped(w, r.getString("id")));
        UnitOfMeasure unit = UnitOfMeasure.valueOf(r.getString("base_unit"));
        MaterialType type = MaterialType.valueOf(r.getString("material_type"));
        Map<String, String> d = dec(r.getString("details_json"));
        return switch (type) {
            case FINISHED_PRODUCT -> new FinishedProduct(id, r.getString("sku"), r.getString("name"), unit, d.getOrDefault("line", ""), Integer.parseInt(d.getOrDefault("warranty", "0")));
            case COMPONENT -> new Component(id, r.getString("sku"), r.getString("name"), unit, Integer.parseInt(d.getOrDefault("leadDays", "0")));
            default -> new Material(id, r.getString("sku"), r.getString("name"), type, unit);
        };
    }

    private static Set<String> set(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.asList(csv.split(",")));
    }

    // Primary keys of the V2 tables are global, so ids are namespaced by workspace.
    private static String scoped(String ws, String id) { return ws + "|" + id; }
    private static String unscoped(String ws, String id) { return id.startsWith(ws + "|") ? id.substring(ws.length() + 1) : id; }

    private static String enc(Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        m.forEach((k, v) -> { if (sb.length() > 0) sb.append('&'); sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8)); });
        return sb.toString();
    }
    private static Map<String, String> dec(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String p : s.split("&")) { int i = p.indexOf('='); if (i > 0) m.put(URLDecoder.decode(p.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8)); }
        return m;
    }
}
