package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillOfMaterialsTest {

    @Test
    @DisplayName("BOM detects and rejects cycles in dependency structure")
    void shouldDetectAndRejectCycles() {
        Material a = new Material(MaterialId.of("MAT-A"), "SKU-A", "Subassembly A", MaterialType.INTERMEDIATE, UnitOfMeasure.PIECE);
        Material b = new Material(MaterialId.of("MAT-B"), "SKU-B", "Subassembly B", MaterialType.INTERMEDIATE, UnitOfMeasure.PIECE);
        Material c = new Material(MaterialId.of("MAT-C"), "SKU-C", "Subassembly C", MaterialType.INTERMEDIATE, UnitOfMeasure.PIECE);

        // A -> B -> C -> A (cycle!)
        BomEntry entry1 = BomEntry.of(a.getId(), b.getId(), Quantity.of(1, UnitOfMeasure.PIECE), 1);
        BomEntry entry2 = BomEntry.of(b.getId(), c.getId(), Quantity.of(1, UnitOfMeasure.PIECE), 1);
        BomEntry entry3 = BomEntry.of(c.getId(), a.getId(), Quantity.of(1, UnitOfMeasure.PIECE), 1);

        assertThatThrownBy(() -> new BillOfMaterials(List.of(a, b, c), List.of(entry1, entry2, entry3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cycle detected in Bill of Materials");
    }

    @Test
    @DisplayName("BOM entry rejects self-loops")
    void shouldRejectSelfCycle() {
        assertThatThrownBy(() -> BomEntry.of(MaterialId.of("X"), MaterialId.of("X"), Quantity.of(1, UnitOfMeasure.PIECE), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A material cannot be a component of itself");
    }

    @Test
    @DisplayName("Topological sort returns deterministic bottom-up evaluation order")
    void shouldComputeDeterministicTopologicalOrdering() {
        FinishedProduct batteryPack = new FinishedProduct(
                MaterialId.of("EV-PACK"), "SKU-PACK", "100kWh Battery Pack", UnitOfMeasure.PIECE, "Model S", 96);
        Component cellModule = new Component(
                MaterialId.of("EV-MOD"), "SKU-MOD", "2170 Cell Module", UnitOfMeasure.PIECE, 10);
        Component batteryCell = new Component(
                MaterialId.of("EV-CELL"), "SKU-CELL", "2170 Lithium-Ion Cell", UnitOfMeasure.PIECE, 25);
        Material cathodePowder = new Material(
                MaterialId.of("RAW-NMC"), "SKU-NMC", "NMC811 Cathode Powder", MaterialType.RAW_MATERIAL, UnitOfMeasure.KILOGRAM);

        // EV-PACK -> EV-MOD -> EV-CELL -> RAW-NMC
        BomEntry e1 = BomEntry.of(batteryPack.getId(), cellModule.getId(), Quantity.of(16, UnitOfMeasure.PIECE), 1);
        BomEntry e2 = BomEntry.of(cellModule.getId(), batteryCell.getId(), Quantity.of(280, UnitOfMeasure.PIECE), 1);
        BomEntry e3 = BomEntry.of(batteryCell.getId(), cathodePowder.getId(), Quantity.of("0.085", UnitOfMeasure.KILOGRAM), 1);

        BillOfMaterials bom = new BillOfMaterials(
                List.of(batteryPack, cellModule, batteryCell, cathodePowder),
                List.of(e1, e2, e3)
        );

        List<MaterialId> order = bom.getTopologicalOrder();

        // Leaves must appear before parents: RAW-NMC before EV-CELL, EV-CELL before EV-MOD, EV-MOD before EV-PACK
        assertThat(order.indexOf(cathodePowder.getId())).isLessThan(order.indexOf(batteryCell.getId()));
        assertThat(order.indexOf(batteryCell.getId())).isLessThan(order.indexOf(cellModule.getId()));
        assertThat(order.indexOf(cellModule.getId())).isLessThan(order.indexOf(batteryPack.getId()));
    }

    @Test
    @DisplayName("Multi-tier BOM explosion correctly calculates quantities and factors in scrap rates")
    void shouldExplodeRequirementsWithScrapRate() {
        FinishedProduct finished = new FinishedProduct(
                MaterialId.of("FP-01"), "SKU-FP", "Finished Inverter", UnitOfMeasure.PIECE, "Inverters", 24);
        Component pcb = new Component(
                MaterialId.of("CMP-PCB"), "SKU-PCB", "Populated PCB", UnitOfMeasure.PIECE, 5);
        Component chip = new Component(
                MaterialId.of("CMP-CHIP"), "SKU-CHIP", "MOSFET Driver IC", UnitOfMeasure.PIECE, 14);

        // 1 Inverter requires 2 PCBs with 5% scrap rate (0.05)
        BomEntry e1 = BomEntry.of(finished.getId(), pcb.getId(),
                Quantity.of(2, UnitOfMeasure.PIECE), new BigDecimal("0.05"), 1);

        // 1 PCB requires 4 Chips with 10% scrap rate (0.10)
        BomEntry e2 = BomEntry.of(pcb.getId(), chip.getId(),
                Quantity.of(4, UnitOfMeasure.PIECE), new BigDecimal("0.10"), 1);

        BillOfMaterials bom = new BillOfMaterials(List.of(finished, pcb, chip), List.of(e1, e2));

        // For 10 Finished Inverters:
        // PCB required: 10 * 2 * (1 + 0.05) = 10 * 2 * 1.05 = 21.0000 PCBs
        // Chip required: 21.0000 * 4 * (1 + 0.10) = 84.0000 * 1.10 = 92.4000 Chips
        Map<MaterialId, Quantity> requirements = bom.explodeRequirements(finished.getId(), Quantity.of(10, UnitOfMeasure.PIECE));

        assertThat(requirements.get(pcb.getId()).amount()).isEqualByComparingTo("21.0000");
        assertThat(requirements.get(chip.getId()).amount()).isEqualByComparingTo("92.4000");
    }
}

