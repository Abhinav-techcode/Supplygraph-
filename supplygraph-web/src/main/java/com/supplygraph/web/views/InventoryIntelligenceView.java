package com.supplygraph.web.views;

import com.supplygraph.application.intelligence.MaterialIntelligenceService;
import com.supplygraph.domain.model.intelligence.MaterialIntelligenceProfile;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "inventory-intelligence", layout = MainLayout.class)
@PageTitle("Inventory Ledger Intelligence | SupplyGraph")
public class InventoryIntelligenceView extends VerticalLayout {

    private final MaterialIntelligenceService materialService;
    private final Grid<MaterialIntelligenceProfile> ledgerGrid;

    public InventoryIntelligenceView(MaterialIntelligenceService materialService) {
        this.materialService = materialService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Double-Entry Inventory Ledger & Stock Balance Intelligence");
        Paragraph desc = new Paragraph("Strict non-double-counted segregation across on-hand, available, production-reserved, shipment-reserved, in-transit, and quarantined accounts.");

        ledgerGrid = new Grid<>();
        ledgerGrid.setSizeFull();

        ledgerGrid.addColumn(MaterialIntelligenceProfile::materialId).setHeader("Material ID").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::materialName).setHeader("Material Name").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::onHandQuantity).setHeader("Physical On-Hand").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::availableQuantity).setHeader("Available Free Stock").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::reservedQuantity).setHeader("Committed Reservations").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::inTransitQuantity).setHeader("In-Transit Pipeline").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::quarantinedQuantity).setHeader("QA Quarantine Hold").setAutoWidth(true);
        ledgerGrid.addColumn(MaterialIntelligenceProfile::dailyConsumptionRate).setHeader("Daily Burn Rate").setAutoWidth(true);
        ledgerGrid.addColumn(p -> p.inventoryRunwayDays() + " days").setHeader("Runway Days").setAutoWidth(true);

        add(title, desc, ledgerGrid);
        loadBalances();
    }

    private void loadBalances() {
        List<MaterialIntelligenceProfile> profiles = materialService.getProfiles("WS-AUTO-01");
        ledgerGrid.setItems(profiles);
    }
}

