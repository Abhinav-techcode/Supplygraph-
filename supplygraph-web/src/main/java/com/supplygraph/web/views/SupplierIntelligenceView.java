package com.supplygraph.web.views;

import com.supplygraph.application.intelligence.SupplierIntelligenceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.intelligence.SupplierIntelligenceMetric;
import com.supplygraph.domain.model.intelligence.SupplierMetricType;
import com.supplygraph.domain.model.intelligence.SupplierRiskProfile;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import java.util.List;
import java.util.Optional;

@Route(value = "supplier-intelligence", layout = MainLayout.class)
@PageTitle("Supplier Intelligence & Risk Profiling | SupplyGraph")
public class SupplierIntelligenceView extends VerticalLayout {

    private final SupplierIntelligenceService supplierService;
    private final DigitalTwinRepositoryPort digitalTwinRepository;

    private final Grid<SupplierRiskProfile> supplierGrid;
    private final TextField searchField;
    private final Paragraph summaryText;

    public SupplierIntelligenceView(
            SupplierIntelligenceService supplierService,
            DigitalTwinRepositoryPort digitalTwinRepository
    ) {
        this.supplierService = supplierService;
        this.digitalTwinRepository = digitalTwinRepository;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Supplier Intelligence & Operational Risk Profiling");
        Paragraph desc = new Paragraph("Quantitative supplier indicators evaluating On-Time In-Full (OTIF) delivery reliability, lead-time variance, capacity utilization, and single-source dependencies.");

        Button recomputeBtn = new Button("Recompute Supplier Metrics", e -> recomputeMetrics());
        recomputeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        searchField = new TextField();
        searchField.setPlaceholder("Filter by supplier name or ID...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshData());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, recomputeBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        summaryText = new Paragraph("Loading supplier metrics...");

        supplierGrid = new Grid<>();
        supplierGrid.setSizeFull();

        supplierGrid.addColumn(SupplierRiskProfile::supplierId).setHeader("Supplier ID").setAutoWidth(true);
        supplierGrid.addColumn(SupplierRiskProfile::supplierName).setHeader("Legal Entity Name").setAutoWidth(true);
        supplierGrid.addColumn(SupplierRiskProfile::tier).setHeader("Tier").setWidth("70px");
        supplierGrid.addColumn(SupplierRiskProfile::country).setHeader("Country").setWidth("90px");
        supplierGrid.addColumn(p -> String.format("%.2f", p.overallRiskScore())).setHeader("Risk Score").setAutoWidth(true);
        supplierGrid.addColumn(p -> p.getMetricValueOrDefault(SupplierMetricType.DELIVERY_RELIABILITY, 95.0) + "%").setHeader("OTIF Reliability").setAutoWidth(true);
        supplierGrid.addColumn(p -> p.getMetricValueOrDefault(SupplierMetricType.LEAD_TIME_VARIABILITY, 2.0) + "d").setHeader("LT Variance").setAutoWidth(true);
        supplierGrid.addColumn(p -> p.getMetricValueOrDefault(SupplierMetricType.CAPACITY_UTILIZATION, 75.0) + "%").setHeader("Cap Utilization").setAutoWidth(true);
        supplierGrid.addColumn(p -> p.isSingleSourceForCriticalMaterials() ? "YES (CRITICAL)" : "No").setHeader("Single Source").setAutoWidth(true);
        supplierGrid.addColumn(SupplierRiskProfile::downstreamSitesImpacted).setHeader("Downstream Sites").setAutoWidth(true);
        supplierGrid.addColumn(SupplierRiskProfile::qualificationStatus).setHeader("Status").setAutoWidth(true);

        add(title, desc, toolbar, summaryText, supplierGrid);
        refreshData();
    }

    private void recomputeMetrics() {
        Optional<DigitalTwinState> stateOpt = digitalTwinRepository.findByWorkspaceId(WorkspaceId.of("default-workspace"));
        if (stateOpt.isEmpty()) {
            Notification.show("Digital twin state not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        List<SupplierRiskProfile> profiles = supplierService.computeSupplierRiskProfiles("default-workspace", stateOpt.get());
        supplierGrid.setItems(profiles);
        updateSummary(profiles);
        Notification.show("Recomputed metrics for " + profiles.size() + " suppliers", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshData() {
        List<SupplierRiskProfile> profiles = supplierService.getProfiles("default-workspace");
        if (profiles.isEmpty()) {
            // Auto compute if not yet present
            recomputeMetrics();
            return;
        }

        String query = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        List<SupplierRiskProfile> filtered = profiles.stream()
                .filter(p -> query.isEmpty() || p.supplierName().toLowerCase().contains(query) || p.supplierId().toLowerCase().contains(query))
                .toList();

        supplierGrid.setItems(filtered);
        updateSummary(profiles);
    }

    private void updateSummary(List<SupplierRiskProfile> profiles) {
        long highRisk = profiles.stream().filter(p -> p.overallRiskScore() > 0.5).count();
        long singleSource = profiles.stream().filter(SupplierRiskProfile::isSingleSourceForCriticalMaterials).count();
        summaryText.setText("Total Suppliers: " + profiles.size() +
                " | High-Risk Entities: " + highRisk +
                " | Single-Source Feeder Dependencies: " + singleSource);
    }
}
