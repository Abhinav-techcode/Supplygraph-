package com.supplygraph.application.sample;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Factory for creating the canonical multi-tier automotive and electronics supply chain scenario.
 * Provides 18 multi-tier nodes, 18 multimodal lanes, multi-tier BOM, and realistic inventory balances.
 */
public final class SampleSupplyChainFactory {

    private SampleSupplyChainFactory() {}

    public static DigitalTwinState createSampleTwin(WorkspaceId workspaceId) {
        // 1. Facilities across 6 Tiered Columns
        // Column 0: Tier 3 Raw Materials & Mines
        Supplier mineAtacama = new Supplier(NodeId.of("MINE-ATACAMA"), "Atacama Lithium Brine Operation", "Antofagasta, CL", true, 3, Set.of("ISO14001"), 0.05);
        Supplier ingotDresden = new Supplier(NodeId.of("INGOT-DRESDEN"), "Dresden High-Purity Silicon Ingot", "Dresden, DE", true, 3, Set.of("ISO9001"), 0.08);
        Supplier miningNorilsk = new Supplier(NodeId.of("MINING-NORILSK"), "Norilsk Nickel & Cobalt Complex", "Norilsk, RU", true, 3, Set.of("ISO9001"), 0.22);

        // Column 1: Tier 2 Component & Material Fabs
        Supplier tier2Fab = new Supplier(NodeId.of("FAB-MUNICH"), "Munich Advanced Silicon Fab", "Munich, DE", true, 2, Set.of("ISO9001", "IATF16949"), 0.15);
        Supplier chemLyon = new Supplier(NodeId.of("CHEM-LYON"), "Lyon Cathode Precursor Synthesis", "Lyon, FR", true, 2, Set.of("ISO9001", "REACH"), 0.12);
        Supplier cellGothenburg = new Supplier(NodeId.of("CELL-GOTHENBURG"), "Gothenburg Battery Cell Gigafactory", "Gothenburg, SE", true, 2, Set.of("ISO9001", "IATF16949"), 0.09);
        Supplier fabHsinchu = new Supplier(NodeId.of("FAB-HSINCHU"), "Hsinchu Wafer Packaging & Test", "Hsinchu, TW", true, 2, Set.of("ISO9001", "IATF16949"), 0.11);

        // Column 2: Tier 1 Subsystems
        Supplier tier1Supplier = new Supplier(NodeId.of("SUP-KYOTO"), "Kyoto Microelectronics Corp", "Kyoto, JP", true, 1, Set.of("ISO9001", "IATF16949"), 0.10);
        Supplier harnessSibiu = new Supplier(NodeId.of("HARNESS-SIBIU"), "Sibiu High-Voltage Wiring Systems", "Sibiu, RO", true, 1, Set.of("IATF16949"), 0.07);
        Supplier moduleAustin = new Supplier(NodeId.of("MODULE-AUSTIN"), "Austin Power Electronics Inverters", "Austin, US", true, 1, Set.of("ISO9001", "IATF16949"), 0.06);
        Supplier brakesBrescia = new Supplier(NodeId.of("BRAKES-BRESCIA"), "Brescia Electro-Hydraulic Braking", "Brescia, IT", true, 1, Set.of("IATF16949"), 0.08);

        // Column 3: Logistics Hubs & Ports
        LogisticsHub hub = new LogisticsHub(NodeId.of("HUB-FRANKFURT"), "Frankfurt Global Logistics Hub", "Frankfurt, DE", true, true, 3500, Set.of("AIR", "RAIL", "ROAD"));
        LogisticsHub portRotterdam = new LogisticsHub(NodeId.of("PORT-ROTTERDAM"), "Rotterdam Maritime Deepwater Port", "Rotterdam, NL", true, true, 8000, Set.of("OCEAN", "RAIL", "ROAD", "INLAND_WATERWAY"));
        LogisticsHub hubChicago = new LogisticsHub(NodeId.of("HUB-CHICAGO"), "Chicago Midwest Rail & Freight Hub", "Chicago, US", true, true, 2800, Set.of("RAIL", "ROAD", "AIR"));

        // Column 4: Tier 0 Manufacturing Plants
        ManufacturingSite assemblyPlant = new ManufacturingSite(NodeId.of("PLANT-STUTTGART"), "Stuttgart Powertrain Assembly", "Stuttgart, DE", true, Set.of("LINE-POWERTRAIN-1", "LINE-POWERTRAIN-2"), "ISO-7", 650);
        ManufacturingSite assemblyBerlin = new ManufacturingSite(NodeId.of("ASSEMBLY-BERLIN"), "Berlin High-Voltage Integration", "Berlin, DE", true, Set.of("LINE-BATTERY-1", "LINE-PACK-2"), "ISO-8", 500);
        ManufacturingSite plantDetroit = new ManufacturingSite(NodeId.of("PLANT-DETROIT"), "Detroit Electric Truck Complex", "Detroit, US", true, Set.of("LINE-TRUCK-1"), "ISO-8", 400);

        // Column 5: Customer Delivery & Flagship OEM Terminal
        LogisticsHub oemWolfsburg = new LogisticsHub(NodeId.of("OEM-WOLFSBURG"), "Wolfsburg Flagship Assembly Terminal", "Wolfsburg, DE", true, true, 1200, Set.of("RAIL", "ROAD"));

        List<SupplyChainNode> allNodesList = List.of(
                mineAtacama, ingotDresden, miningNorilsk,
                tier2Fab, chemLyon, cellGothenburg, fabHsinchu,
                tier1Supplier, harnessSibiu, moduleAustin, brakesBrescia,
                hub, portRotterdam, hubChicago,
                assemblyPlant, assemblyBerlin, plantDetroit,
                oemWolfsburg
        );

        List<TransportationLane> allLanesList = List.of(
                new TransportationLane(EdgeId.of("L-1"), NodeId.of("MINE-ATACAMA"), NodeId.of("CHEM-LYON"), TransportMode.OCEAN, Duration.ofHours(336), BigDecimal.valueOf(0.42), true),
                new TransportationLane(EdgeId.of("L-2"), NodeId.of("MINING-NORILSK"), NodeId.of("CHEM-LYON"), TransportMode.RAIL, Duration.ofHours(72), BigDecimal.valueOf(0.68), true),
                new TransportationLane(EdgeId.of("L-3"), NodeId.of("CHEM-LYON"), NodeId.of("CELL-GOTHENBURG"), TransportMode.ROAD, Duration.ofHours(24), BigDecimal.valueOf(1.40), true),
                new TransportationLane(EdgeId.of("L-4"), NodeId.of("INGOT-DRESDEN"), NodeId.of("FAB-MUNICH"), TransportMode.ROAD, Duration.ofHours(5), BigDecimal.valueOf(0.80), true),
                new TransportationLane(EdgeId.of("LANE-MUNICH-STUTTGART"), NodeId.of("FAB-MUNICH"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(6), BigDecimal.valueOf(1.20), true),
                new TransportationLane(EdgeId.of("L-6"), NodeId.of("FAB-MUNICH"), NodeId.of("HUB-FRANKFURT"), TransportMode.RAIL, Duration.ofHours(4), BigDecimal.valueOf(0.65), true),
                new TransportationLane(EdgeId.of("L-7"), NodeId.of("FAB-HSINCHU"), NodeId.of("HUB-FRANKFURT"), TransportMode.AIR, Duration.ofHours(28), BigDecimal.valueOf(7.80), true),
                new TransportationLane(EdgeId.of("LANE-KYOTO-FRA"), NodeId.of("SUP-KYOTO"), NodeId.of("HUB-FRANKFURT"), TransportMode.AIR, Duration.ofHours(36), BigDecimal.valueOf(8.50), true),
                new TransportationLane(EdgeId.of("L-9"), NodeId.of("HARNESS-SIBIU"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(18), BigDecimal.valueOf(1.90), true),
                new TransportationLane(EdgeId.of("L-10"), NodeId.of("BRAKES-BRESCIA"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(7), BigDecimal.valueOf(0.95), true),
                new TransportationLane(EdgeId.of("L-11"), NodeId.of("CELL-GOTHENBURG"), NodeId.of("ASSEMBLY-BERLIN"), TransportMode.ROAD, Duration.ofHours(14), BigDecimal.valueOf(1.60), true),
                new TransportationLane(EdgeId.of("L-12"), NodeId.of("MODULE-AUSTIN"), NodeId.of("HUB-CHICAGO"), TransportMode.RAIL, Duration.ofHours(28), BigDecimal.valueOf(1.15), true),
                new TransportationLane(EdgeId.of("L-13"), NodeId.of("HUB-CHICAGO"), NodeId.of("PLANT-DETROIT"), TransportMode.ROAD, Duration.ofHours(6), BigDecimal.valueOf(0.70), true),
                new TransportationLane(EdgeId.of("LANE-FRA-STUTTGART"), NodeId.of("HUB-FRANKFURT"), NodeId.of("PLANT-STUTTGART"), TransportMode.ROAD, Duration.ofHours(4), BigDecimal.valueOf(0.90), true),
                new TransportationLane(EdgeId.of("L-15"), NodeId.of("PORT-ROTTERDAM"), NodeId.of("HUB-FRANKFURT"), TransportMode.RAIL, Duration.ofHours(8), BigDecimal.valueOf(0.85), true),
                new TransportationLane(EdgeId.of("L-16"), NodeId.of("PORT-ROTTERDAM"), NodeId.of("ASSEMBLY-BERLIN"), TransportMode.ROAD, Duration.ofHours(9), BigDecimal.valueOf(1.30), true),
                new TransportationLane(EdgeId.of("L-17"), NodeId.of("PLANT-STUTTGART"), NodeId.of("OEM-WOLFSBURG"), TransportMode.RAIL, Duration.ofHours(8), BigDecimal.valueOf(1.05), true),
                new TransportationLane(EdgeId.of("L-18"), NodeId.of("ASSEMBLY-BERLIN"), NodeId.of("OEM-WOLFSBURG"), TransportMode.ROAD, Duration.ofHours(3), BigDecimal.valueOf(0.60), true)
        );

        SupplyChainGraph graph = new SupplyChainGraph(allNodesList, allLanesList);

        // 2. Multi-Tier Bill of Materials
        Component mcu = new Component(
                MaterialId.of("MCU-AUTO-32"), "MCU-AUTO-32-SKU", "Automotive 32-bit Microcontroller MCU",
                UnitOfMeasure.PIECE, 14
        );
        Component lithiumCarbonate = new Component(
                MaterialId.of("LITHIUM-HYDROXIDE"), "LI-OH-BATTERY-SKU", "Battery-Grade Lithium Hydroxide",
                UnitOfMeasure.KILOGRAM, 30
        );
        Component batteryCell = new Component(
                MaterialId.of("CELL-EV-2170"), "CELL-2170-SKU", "2170 High-Density Lithium-Ion Cell",
                UnitOfMeasure.PIECE, 21
        );
        Component batteryPack = new Component(
                MaterialId.of("PACK-80KWH"), "PACK-80KWH-SKU", "80kWh Liquid-Cooled Battery Pack",
                UnitOfMeasure.PIECE, 10
        );
        FinishedProduct ecu = new FinishedProduct(
                MaterialId.of("ECU-POWERTRAIN-V8"), "ECU-V8-SKU", "Powertrain Controller ECU",
                UnitOfMeasure.PIECE, "POWERTRAIN", 36
        );
        FinishedProduct evPowertrain = new FinishedProduct(
                MaterialId.of("POWERTRAIN-EV-DUAL"), "EV-DUAL-MOTOR-SKU", "Dual-Motor Electric Powertrain System",
                UnitOfMeasure.PIECE, "DRIVE_UNIT", 48
        );

        // BOM Hierarchy:
        // LITHIUM -> CELL -> PACK -> POWERTRAIN
        // MCU -> ECU -> POWERTRAIN
        BomEntry bomCellLithium = new BomEntry(batteryCell.getId(), lithiumCarbonate.getId(), Quantity.of(BigDecimal.valueOf(1.2), UnitOfMeasure.KILOGRAM), BigDecimal.valueOf(0.02), 1);
        BomEntry bomPackCell = new BomEntry(batteryPack.getId(), batteryCell.getId(), Quantity.of(BigDecimal.valueOf(440), UnitOfMeasure.PIECE), BigDecimal.valueOf(0.01), 1);
        BomEntry bomEcuMcu = new BomEntry(ecu.getId(), mcu.getId(), Quantity.of(BigDecimal.valueOf(2), UnitOfMeasure.PIECE), BigDecimal.ZERO, 1);
        BomEntry bomPowertrainPack = new BomEntry(evPowertrain.getId(), batteryPack.getId(), Quantity.of(BigDecimal.ONE, UnitOfMeasure.PIECE), BigDecimal.ZERO, 1);
        BomEntry bomPowertrainEcu = new BomEntry(evPowertrain.getId(), ecu.getId(), Quantity.of(BigDecimal.ONE, UnitOfMeasure.PIECE), BigDecimal.ZERO, 2);

        BillOfMaterials bom = new BillOfMaterials(
                List.of(evPowertrain, ecu, batteryPack, batteryCell, lithiumCarbonate, mcu),
                List.of(bomCellLithium, bomPackCell, bomEcuMcu, bomPowertrainPack, bomPowertrainEcu)
        );

        // 3. Digital Twin State
        DigitalTwinState state = new DigitalTwinState(workspaceId, StateVersion.of(1L), graph, bom);

        // 4. Opening Inventory Balances
        postOpeningBalance(state, NodeId.of("PLANT-STUTTGART"), MaterialId.of("MCU-AUTO-32"), BigDecimal.valueOf(4500), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("HUB-FRANKFURT"), MaterialId.of("MCU-AUTO-32"), BigDecimal.valueOf(1500), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("FAB-MUNICH"), MaterialId.of("MCU-AUTO-32"), BigDecimal.valueOf(4200), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("PORT-ROTTERDAM"), MaterialId.of("MCU-AUTO-32"), BigDecimal.valueOf(65000), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("CELL-GOTHENBURG"), MaterialId.of("CELL-EV-2170"), BigDecimal.valueOf(32000), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("ASSEMBLY-BERLIN"), MaterialId.of("PACK-80KWH"), BigDecimal.valueOf(850), UnitOfMeasure.PIECE);
        postOpeningBalance(state, NodeId.of("CHEM-LYON"), MaterialId.of("LITHIUM-HYDROXIDE"), BigDecimal.valueOf(18500), UnitOfMeasure.KILOGRAM);
        postOpeningBalance(state, NodeId.of("MINE-ATACAMA"), MaterialId.of("LITHIUM-HYDROXIDE"), BigDecimal.valueOf(85000), UnitOfMeasure.KILOGRAM);

        return state;
    }

    private static void postOpeningBalance(DigitalTwinState state, NodeId node, MaterialId material, BigDecimal amount, UnitOfMeasure unit) {
        InventoryAccountId account = InventoryAccountId.of(node, material, BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId offset = InventoryAccountId.of(node, material, BalanceCategory.EXTERNAL_OFFSET);
        Quantity q = Quantity.of(amount, unit);
        state.getInventoryLedger().postTransaction(new InventoryTransaction(
                TransactionId.generate(),
                Instant.now(),
                "OPENING_BALANCE",
                "SAMPLE-SETUP",
                List.of(
                        new InventoryPosting(UUID.randomUUID().toString(), account, PostingType.DEBIT, q),
                        new InventoryPosting(UUID.randomUUID().toString(), offset, PostingType.CREDIT, q)
                )
        ));
    }
}

