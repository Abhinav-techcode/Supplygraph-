package com.supplygraph.application.service;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.BomEntry;
import com.supplygraph.domain.model.bom.Component;
import com.supplygraph.domain.model.bom.Material;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * Loads a customer's own supply chain into the digital twin from three small CSV tables (facilities, lanes, stock), with an optional bill of
 * materials. Everything is validated first; if anything is wrong nothing is saved and every problem is reported with its line number.
 *
 * facilities: id,name,type,location,tier,capacity   (type = SUPPLIER | MANUFACTURING_SITE | LOGISTICS_HUB | WAREHOUSE; capacity = units/hour for sites, pallets/day for hubs, m2 for warehouses)
 * lanes:      from,to,mode,transit_hours,cost_per_kg   (mode = ROAD | RAIL | AIR | OCEAN)
 * stock:      facility,material,quantity
 * bom:        parent,child,quantity_per_parent         (optional)
 */
@Service
public class SupplyChainImportService {
    public record Result(boolean saved, int facilities, int lanes, int stockRows, int bomRows, long twinVersion, List<String> errors) {}

    private final DigitalTwinRepositoryPort twins;
    private final InventoryLedgerRepositoryPort ledger;

    public SupplyChainImportService(DigitalTwinRepositoryPort twins, InventoryLedgerRepositoryPort ledger) {
        this.twins = twins;
        this.ledger = ledger;
    }

    @Transactional
    public Result importCsv(WorkspaceId ws, String facilitiesCsv, String lanesCsv, String stockCsv, String bomCsv, boolean dryRun) {
        return importCsv(ws, facilitiesCsv, lanesCsv, stockCsv, bomCsv, dryRun, false);
    }

    /** With merge=true the rows are added to the workspace's existing twin (existing facilities, lanes, materials and bill of materials are kept); otherwise they replace it. */
    @Transactional
    public Result importCsv(WorkspaceId ws, String facilitiesCsv, String lanesCsv, String stockCsv, String bomCsv, boolean dryRun, boolean merge) {
        List<String> errors = new ArrayList<>();
        Map<String, SupplyChainNode> nodes = new LinkedHashMap<>();
        List<TransportationLane> lanes = new ArrayList<>();
        Set<String> laneIds = new HashSet<>();
        Map<String, Material> materials = new LinkedHashMap<>();
        List<BomEntry> bomEntries = new ArrayList<>();
        if (merge) {
            twins.findByWorkspaceId(ws).ifPresent(t -> {
                t.getGraph().getAllNodes().forEach(n -> nodes.put(n.getId().value(), n));
                t.getGraph().getAllLanes().forEach(l -> { lanes.add(l); laneIds.add(l.getId().value()); });
                t.getBom().getAllMaterials().forEach(m -> materials.put(m.getId().value(), m));
                bomEntries.addAll(t.getBom().getAllEntries());
            });
        }
        for (String[] r : rows(facilitiesCsv, 6, "facilities", errors)) {
            String id = r[0];
            try {
                if (id.isBlank() || nodes.containsKey(id)) { errors.add("facilities: duplicate or empty id '" + id + "'"); continue; }
                NodeType type = enumOf(NodeType.class, "type", r[2]);
                double cap = r[5].isBlank() ? 0 : num("capacity", r[5]).doubleValue();
                int tier = r[4].isBlank() ? 1 : num("tier", r[4]).intValue();
                SupplyChainNode n = switch (type) {
                    case SUPPLIER -> new Supplier(NodeId.of(id), r[1], r[3], true, tier, Set.of(), 0.1);
                    case MANUFACTURING_SITE -> new ManufacturingSite(NodeId.of(id), r[1], r[3], true, Set.of("L1"), "NONE", (int) cap);
                    case LOGISTICS_HUB -> new LogisticsHub(NodeId.of(id), r[1], r[3], true, true, (int) cap, Set.of("ROAD", "RAIL", "AIR", "OCEAN"));
                    case WAREHOUSE -> new Warehouse(NodeId.of(id), r[1], r[3], true, BigDecimal.valueOf(cap), false, false);
                };
                nodes.put(id, n);
            } catch (RuntimeException e) {
                errors.add("facilities: '" + id + "': " + e.getMessage());
            }
        }
        if (nodes.isEmpty() && errors.isEmpty()) errors.add("facilities: at least one facility is required");

        for (String[] r : rows(lanesCsv, 5, "lanes", errors)) {
            try {
                if (!nodes.containsKey(r[0]) || !nodes.containsKey(r[1])) { errors.add("lanes: " + r[0] + " -> " + r[1] + " references an unknown facility"); continue; }
                String id = r[0] + "->" + r[1] + (laneIds.contains(r[0] + "->" + r[1]) ? "#" + (laneIds.size() + 1) : "");
                laneIds.add(id);
                lanes.add(new TransportationLane(EdgeId.of(id), NodeId.of(r[0]), NodeId.of(r[1]), enumOf(TransportMode.class, "mode", r[2]),
                        Duration.ofMinutes(Math.round(num("transit_hours", r[3]).doubleValue() * 60)), num("cost_per_kg", r[4]), true));
            } catch (RuntimeException e) {
                errors.add("lanes: " + r[0] + " -> " + r[1] + ": " + e.getMessage());
            }
        }

        Map<InventoryAccountId, BigDecimal> balances = new LinkedHashMap<>();
        for (String[] r : rows(stockCsv, 3, "stock", errors)) {
            try {
                if (!nodes.containsKey(r[0])) { errors.add("stock: unknown facility '" + r[0] + "'"); continue; }
                BigDecimal q = num("quantity", r[2]);
                if (q.signum() < 0) { errors.add("stock: negative quantity for " + r[0] + "/" + r[1]); continue; }
                materials.computeIfAbsent(r[1], m -> new Component(MaterialId.of(m), m + "-SKU", m, UnitOfMeasure.PIECE, 14));
                balances.merge(InventoryAccountId.of(NodeId.of(r[0]), MaterialId.of(r[1]), BalanceCategory.ON_HAND_AVAILABLE), q, BigDecimal::add);
            } catch (RuntimeException e) {
                errors.add("stock: " + r[0] + "/" + r[1] + ": " + e.getMessage());
            }
        }
        if (bomCsv != null && !bomCsv.isBlank()) {
            int pos = bomEntries.size() + 1;
            for (String[] r : rows(bomCsv, 3, "bom", errors)) {
                try {
                    for (String m : new String[]{r[0], r[1]}) materials.computeIfAbsent(m, k -> new Component(MaterialId.of(k), k + "-SKU", k, UnitOfMeasure.PIECE, 14));
                    bomEntries.add(new BomEntry(MaterialId.of(r[0]), MaterialId.of(r[1]), Quantity.of(num("quantity_per_parent", r[2]), UnitOfMeasure.PIECE), BigDecimal.ZERO, pos++));
                } catch (RuntimeException e) {
                    errors.add("bom: " + r[0] + " -> " + r[1] + ": " + e.getMessage());
                }
            }
        }
        long next = twins.findByWorkspaceId(ws).map(t -> t.getCurrentVersion().value() + 1).orElse(1L);
        if (!errors.isEmpty() || dryRun) return new Result(false, nodes.size(), lanes.size(), balances.size(), bomEntries.size(), next, errors);

        SupplyChainGraph graph = new SupplyChainGraph(nodes.values(), lanes);
        BillOfMaterials bom = new BillOfMaterials(materials.values(), bomEntries);
        twins.save(new DigitalTwinState(ws, StateVersion.of(next), graph, bom));
        balances.forEach((acc, q) -> ledger.updateBalance(ws, acc, Quantity.of(q, UnitOfMeasure.PIECE)));
        twins.reload(ws);   // balances were written through the ledger port: rebuild the twin so they are visible
        return new Result(true, nodes.size(), lanes.size(), balances.size(), bomEntries.size(), next, List.of());
    }

    private static BigDecimal num(String field, String v) {
        try { return new BigDecimal(v.trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("'" + v + "' is not a number (" + field + ")"); }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String field, String v) {
        try { return Enum.valueOf(type, v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("'" + v + "' is not a valid " + field + " (use " + java.util.Arrays.toString(type.getEnumConstants()) + ")"); }
    }

    /** Parses a CSV table: first non-blank line is the header; '#' lines and blank lines are skipped; short rows are reported. */
    private static List<String[]> rows(String csv, int cols, String table, List<String> errors) {
        List<String[]> out = new ArrayList<>();
        if (csv == null) return out;
        String[] lines = csv.split("\\R");
        boolean header = true;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (header) { header = false; continue; }
            String[] p = line.split(",", -1);
            if (p.length < cols - (table.equals("facilities") ? 2 : 0)) { errors.add(table + " line " + (i + 1) + ": expected " + cols + " columns, found " + p.length); continue; }
            String[] r = new String[cols];
            for (int c = 0; c < cols; c++) r[c] = c < p.length ? p[c].trim() : "";
            out.add(r);
        }
        return out;
    }
}
