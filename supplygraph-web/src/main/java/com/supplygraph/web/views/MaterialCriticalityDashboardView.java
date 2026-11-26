package com.supplygraph.web.views;

import com.supplygraph.application.intelligence.MaterialIntelligenceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.intelligence.MaterialCriticalityLevel;
import com.supplygraph.domain.model.intelligence.MaterialIntelligenceProfile;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Route(value = "material-criticality", layout = MainLayout.class)
@PageTitle("Material Criticality Dashboard | ORBIX")
public class MaterialCriticalityDashboardView extends VerticalLayout {

    private final MaterialIntelligenceService materialService;
    private final DigitalTwinRepositoryPort digitalTwinRepository;

    private final Grid<MaterialIntelligenceProfile> materialGrid;
    private final TextField searchField;
    private final ComboBox<MaterialCriticalityLevel> criticalityFilter;
    private final Paragraph summaryText;

    public MaterialCriticalityDashboardView(
            MaterialIntelligenceService materialService,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.materialService = materialService;
        this.digitalTwinRepository = digitalTwinRepository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Material Criticality & Inventory Runway Intelligence");
        Paragraph desc = new Paragraph("Continuous classification of Bill of Materials components by criticality, procurement lead times, sole-source dependencies, and inventory depletion runways.");

        Button recomputeBtn = new Button("Recompute Material Runways", e -> recomputeProfiles());
        recomputeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        searchField = new TextField();
        searchField.setPlaceholder("Filter by component name or ID...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshData());

        criticalityFilter = new ComboBox<>();
        criticalityFilter.setPlaceholder("Filter Criticality");
        criticalityFilter.setItems(MaterialCriticalityLevel.values());
        criticalityFilter.setItemLabelGenerator(MaterialCriticalityLevel::displayName);
        criticalityFilter.setClearButtonVisible(true);
        criticalityFilter.addValueChangeListener(e -> refreshData());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, criticalityFilter, recomputeBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        summaryText = new Paragraph("Loading component runway intelligence...");

        materialGrid = new Grid<>();
        materialGrid.setSizeFull();

        materialGrid.addColumn(MaterialIntelligenceProfile::materialId).setHeader("Material ID").setAutoWidth(true);
        materialGrid.addColumn(MaterialIntelligenceProfile::materialName).setHeader("Material Name").setAutoWidth(true);
        materialGrid.addColumn(MaterialIntelligenceProfile::category).setHeader("Category").setAutoWidth(true);
        materialGrid.addColumn(p -> p.criticality().displayName()).setHeader("Criticality").setAutoWidth(true);
        materialGrid.addColumn(p -> p.leadTimeDays() + " days").setHeader("Procurement Lead Time").setAutoWidth(true);
        materialGrid.addColumn(MaterialIntelligenceProfile::availableQuantity).setHeader("Available Stock").setAutoWidth(true);
        materialGrid.addColumn(MaterialIntelligenceProfile::dailyConsumptionRate).setHeader("Daily Burn Rate").setAutoWidth(true);
        materialGrid.addColumn(p -> p.inventoryRunwayDays() + " days").setHeader("Runway Days").setAutoWidth(true);
        materialGrid.addColumn(p -> p.isSingleSourced() ? "SOLE SOURCE (" + p.singleSourceSupplierId() + ")" : "Multi-Sourced").setHeader("Sourcing Status").setAutoWidth(true);
        materialGrid.addColumn(p -> p.isRunwayBelowLeadTime() ? "STOCKOUT RISK" : "Adequate Buffer").setHeader("Buffer Margin").setAutoWidth(true);

        add(title, desc, toolbar, summaryText, materialGrid);
        refreshData();
    }

    private void recomputeProfiles() {
        Optional<DigitalTwinState> stateOpt = digitalTwinRepository.findByWorkspaceId(WorkspaceId.of("WS-AUTO-01"));
        if (stateOpt.isEmpty()) {
            Notification.show("Digital twin state not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        List<MaterialIntelligenceProfile> profiles = materialService.computeMaterialProfiles("WS-AUTO-01", stateOpt.get(), BigDecimal.valueOf(30));
        materialGrid.setItems(profiles);
        updateSummary(profiles);
        Notification.show("Recomputed runways for " + profiles.size() + " materials", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshData() {
        List<MaterialIntelligenceProfile> profiles = materialService.getProfiles("WS-AUTO-01");
        if (profiles.isEmpty()) {
            recomputeProfiles();
            return;
        }

        String query = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        MaterialCriticalityLevel filter = criticalityFilter.getValue();

        List<MaterialIntelligenceProfile> filtered = profiles.stream()
                .filter(p -> query.isEmpty() || p.materialName().toLowerCase().contains(query) || p.materialId().toLowerCase().contains(query))
                .filter(p -> filter == null || p.criticality() == filter)
                .toList();

        materialGrid.setItems(filtered);
        updateSummary(profiles);
    }

    private void updateSummary(List<MaterialIntelligenceProfile> profiles) {
        long criticalCount = profiles.stream().filter(p -> p.criticality() == MaterialCriticalityLevel.VERY_HIGH || p.criticality() == MaterialCriticalityLevel.HIGH).count();
        long stockoutRisk = profiles.stream().filter(MaterialIntelligenceProfile::isRunwayBelowLeadTime).count();
        summaryText.setText("Tracked Materials: " + profiles.size() +
                " | High/Very High Criticality: " + criticalCount +
                " | Materials with Runway < Lead Time: " + stockoutRisk);
    }
}

