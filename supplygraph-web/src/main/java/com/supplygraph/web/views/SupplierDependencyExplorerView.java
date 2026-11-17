package com.supplygraph.web.views;

import com.supplygraph.application.intelligence.SupplierIntelligenceService;
import com.supplygraph.domain.model.intelligence.SupplierRiskProfile;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "supplier-dependencies", layout = MainLayout.class)
@PageTitle("Supplier Dependency Explorer | SupplyGraph")
public class SupplierDependencyExplorerView extends VerticalLayout {

    private final SupplierIntelligenceService supplierService;
    private final Grid<SupplierRiskProfile> dependencyGrid;

    public SupplierDependencyExplorerView(SupplierIntelligenceService supplierService) {
        this.supplierService = supplierService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Multi-Tier Supplier Dependency & Downstream Exposure Explorer");
        Paragraph desc = new Paragraph("Identify sole-source manufacturing bottlenecks, multi-tier dependencies, and downstream assembly lines vulnerable to catastrophic cascade failures.");

        dependencyGrid = new Grid<>();
        dependencyGrid.setSizeFull();

        dependencyGrid.addColumn(SupplierRiskProfile::supplierId).setHeader("Supplier ID").setAutoWidth(true);
        dependencyGrid.addColumn(SupplierRiskProfile::supplierName).setHeader("Supplier Name").setAutoWidth(true);
        dependencyGrid.addColumn(SupplierRiskProfile::country).setHeader("Country").setWidth("90px");
        dependencyGrid.addColumn(p -> p.isSingleSourceForCriticalMaterials() ? "SOLE QUALIFIED SOURCE" : "Multi-Sourced").setHeader("Sourcing Exposure").setAutoWidth(true);
        dependencyGrid.addColumn(SupplierRiskProfile::downstreamSitesImpacted).setHeader("Downstream Plants Impacted").setAutoWidth(true);
        dependencyGrid.addColumn(SupplierRiskProfile::alternativeSuppliersCount).setHeader("Qualified Backups").setAutoWidth(true);
        dependencyGrid.addComponentColumn(p -> {
            Button simBtn = new Button("Test Outage", e -> UI.getCurrent().navigate("disruptions"));
            simBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return simBtn;
        }).setHeader("Simulation Action").setAutoWidth(true);

        add(title, desc, dependencyGrid);
        loadData();
    }

    private void loadData() {
        List<SupplierRiskProfile> profiles = supplierService.getProfiles("default-workspace");
        dependencyGrid.setItems(profiles);
    }
}
