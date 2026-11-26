package com.supplygraph.web.views;

import com.supplygraph.industry.api.IndustryPackExtension;
import com.supplygraph.industry.api.IndustryPolicy;
import com.supplygraph.industry.automotive.AutomotiveIndustryPack;
import com.supplygraph.industry.ev.EvBatteryIndustryPack;
import com.supplygraph.industry.pharma.PharmaIndustryPack;
import com.supplygraph.industry.semiconductor.SemiconductorIndustryPack;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "industry-config", layout = MainLayout.class)
@PageTitle("Industry Configuration & Packs | ORBIX")
public class IndustryConfigurationView extends VerticalLayout {

    private final List<IndustryPackExtension> packs = List.of(
            new EvBatteryIndustryPack(),
            new PharmaIndustryPack(),
            new SemiconductorIndustryPack(),
            new AutomotiveIndustryPack()
    );

    private final ComboBox<IndustryPackExtension> packSelector;
    private final Span versionBadge;
    private final Paragraph descriptionText;
    private final Grid<IndustryPolicy> policyGrid;
    private final UnorderedList criticalMaterialsList;
    private final UnorderedList qualityRulesList;

    public IndustryConfigurationView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Industry Intelligence Packs & Domain Policies");
        Paragraph desc = new Paragraph("Configurable industry extensions defining domain-specific constraints, critical minerals/APIs, quality validation rules, and qualification policies behind clean SPI boundaries.");

        packSelector = new ComboBox<>("Select Industry Intelligence Pack", packs);
        packSelector.setItemLabelGenerator(IndustryPackExtension::getDisplayName);
        packSelector.setValue(packs.get(0));
        packSelector.setWidth("380px");

        versionBadge = new Span("v0.1.0");
        versionBadge.getElement().getThemeList().add("badge success");

        HorizontalLayout headerRow = new HorizontalLayout(packSelector, versionBadge);
        headerRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        descriptionText = new Paragraph();

        policyGrid = new Grid<>();
        policyGrid.setHeight("220px");
        policyGrid.addColumn(IndustryPolicy::policyCode).setHeader("Policy Code").setWidth("140px");
        policyGrid.addColumn(IndustryPolicy::name).setHeader("Policy Name").setAutoWidth(true);
        policyGrid.addColumn(IndustryPolicy::description).setHeader("Constraint Definition").setAutoWidth(true);
        policyGrid.addColumn(p -> p.requiresHumanApproval() ? "MANDATORY" : "Optional").setHeader("Human Approval").setWidth("140px");

        criticalMaterialsList = new UnorderedList();
        qualityRulesList = new UnorderedList();

        VerticalLayout materialsCol = new VerticalLayout(new H4("Designated Critical Materials & Commodities"), criticalMaterialsList);
        materialsCol.setWidth("50%");

        VerticalLayout rulesCol = new VerticalLayout(new H4("Industry Quality Validation Rules"), qualityRulesList);
        rulesCol.setWidth("50%");

        HorizontalLayout listsRow = new HorizontalLayout(materialsCol, rulesCol);
        listsRow.setWidthFull();

        packSelector.addValueChangeListener(e -> renderPack(e.getValue()));

        add(title, desc, headerRow, descriptionText, new H4("Active Industry Policies & Constraints"), policyGrid, listsRow);
        renderPack(packs.get(0));
    }

    private void renderPack(IndustryPackExtension pack) {
        if (pack == null) return;
        versionBadge.setText("v" + pack.getVersion() + " (Active)");
        descriptionText.setText(pack.getDescription() + " | Supported Scenarios: " + pack.getSupportedScenarioCodes().size());

        policyGrid.setItems(pack.getIndustryPolicies());

        criticalMaterialsList.removeAll();
        for (String mat : pack.getCriticalMaterials()) {
            criticalMaterialsList.add(new ListItem(mat));
        }

        qualityRulesList.removeAll();
        for (String rule : pack.getQualityValidationRules()) {
            qualityRulesList.add(new ListItem(rule));
        }
    }
}

