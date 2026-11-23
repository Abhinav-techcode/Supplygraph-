package com.supplygraph.web.views;

import com.supplygraph.application.service.SupplyChainImportService;
import com.supplygraph.application.service.SyntheticNetworkGenerator;
import com.vaadin.flow.component.select.Select;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Step 1 of any real use: put your own facilities, lanes and stock into the digital twin. */
@Route(value = "network-import", layout = MainLayout.class)
@PageTitle("Add your data | SupplyGraph")
public class SupplyChainImportView extends VerticalLayout {
    static final String FACILITIES = "id,name,type,location,tier,capacity\n"
            + "SUP-A,Resin Works Oslo,SUPPLIER,Oslo NO,1,\n"
            + "SUP-B,Polymer Lyon,SUPPLIER,Lyon FR,1,\n"
            + "HUB-1,Hamburg Distribution Hub,LOGISTICS_HUB,Hamburg DE,,500\n"
            + "PLANT-1,Munich Assembly Plant,MANUFACTURING_SITE,Munich DE,,40\n";
    static final String LANES = "from,to,mode,transit_hours,cost_per_kg\n"
            + "SUP-A,PLANT-1,ROAD,24,2.0\n"
            + "SUP-B,HUB-1,RAIL,48,1.0\n"
            + "HUB-1,PLANT-1,ROAD,12,1.5\n";
    static final String STOCK = "facility,material,quantity\n"
            + "HUB-1,RESIN-X,6000\n"
            + "PLANT-1,RESIN-X,900\n";
    static final String BOM = "parent,child,quantity_per_parent\n"
            + "WIDGET,RESIN-X,2\n";

    public SupplyChainImportView(SupplyChainImportService importService, SyntheticNetworkGenerator generator) {
        setPadding(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");
        add(new H2("Add your data"), new Paragraph("Describe your supply chain in four small tables. SupplyGraph validates everything first and saves nothing unless every line is correct. "
                + "The result becomes the digital twin that the map, impact analysis, agents and simulations work from."));

        TextArea fac = area("1 · Facilities (id, name, type, location, tier, capacity)", FACILITIES);
        TextArea lan = area("2 · Lanes (from, to, mode, transit_hours, cost_per_kg)", LANES);
        TextArea stk = area("3 · Stock on hand (facility, material, quantity)", STOCK);
        TextArea bom = area("4 · Bill of materials, optional (parent, child, quantity_per_parent)", BOM);
        Div result = new Div();
        result.addClassName("sg-import-result");
        add(generatorPanel(importService, generator, fac, lan, stk, bom, result));

        Button validate = new Button("Check my data", e -> run(importService, fac, lan, stk, bom, true, result));
        Button save = new Button("Save as my digital twin", e -> run(importService, fac, lan, stk, bom, false, result));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClassName("sg-import-save");
        Button clear = new Button("Clear", e -> { fac.clear(); lan.clear(); stk.clear(); bom.clear(); result.removeAll(); });
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(fac, lan, stk, bom, new HorizontalLayout(validate, save, clear), result,
                new Paragraph("Types: SUPPLIER, MANUFACTURING_SITE (capacity = units per hour), LOGISTICS_HUB (pallets per day), WAREHOUSE (m²). Modes: ROAD, RAIL, AIR, OCEAN. Lines starting with # are ignored. Saving replaces the twin of this workspace."));
    }

    /** For people with no data yet: pick an industry, region and size and the app builds a complete network, validated like any import. */
    private Div generatorPanel(SupplyChainImportService svc, SyntheticNetworkGenerator generator, TextArea fac, TextArea lan, TextArea stk, TextArea bom, Div result) {
        Div panel = new Div();
        panel.addClassName("sg-generator");
        panel.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px").set("padding", "16px").set("margin-bottom", "8px");
        H3 title = new H3("No data yet? Let SupplyGraph create a network for you");
        Paragraph help = new Paragraph("Real city locations, with distances, transit times and freight costs worked out from the coordinates. Company names, capacities and stock levels are invented, "
                + "and labelled as generated. Loading it replaces the current twin; swap in your own data any time on this page.");
        Select<SyntheticNetworkGenerator.Industry> industry = new Select<>();
        industry.setLabel("Industry");
        industry.setItems(SyntheticNetworkGenerator.Industry.values());
        industry.setValue(SyntheticNetworkGenerator.Industry.AUTOMOTIVE);
        Select<SyntheticNetworkGenerator.Region> region = new Select<>();
        region.setLabel("Region");
        region.setItems(SyntheticNetworkGenerator.Region.values());
        region.setValue(SyntheticNetworkGenerator.Region.GLOBAL);
        Select<SyntheticNetworkGenerator.Size> size = new Select<>();
        size.setLabel("Size");
        size.setItems(SyntheticNetworkGenerator.Size.values());
        size.setValue(SyntheticNetworkGenerator.Size.MEDIUM);
        Button load = new Button("Generate and load", e -> {
            try {
                var d = generator.generate(industry.getValue(), region.getValue(), size.getValue(), System.nanoTime());
                fac.setValue(d.facilities());
                lan.setValue(d.lanes());
                stk.setValue(d.stock());
                bom.setValue(d.bom());
                run(svc, fac, lan, stk, bom, false, result);
                if (result.getChildren().findAny().isPresent()) result.add(new Paragraph(d.summary()));
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        load.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        load.addClassName("sg-generate-load");
        HorizontalLayout row = new HorizontalLayout(industry, region, size, load);
        row.setAlignItems(Alignment.END);
        panel.add(title, help, row);
        return panel;
    }

    private static TextArea area(String label, String value) {
        TextArea t = new TextArea(label);
        t.setWidthFull();
        t.setMinHeight("120px");
        t.setValue(value);
        t.getStyle().set("font-family", "monospace");
        return t;
    }

    private void run(SupplyChainImportService svc, TextArea fac, TextArea lan, TextArea stk, TextArea bom, boolean dry, Div out) {
        out.removeAll();
        try {
            var r = svc.importCsv(WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID), fac.getValue(), lan.getValue(), stk.getValue(), bom.getValue(), dry);
            if (!r.errors().isEmpty()) {
                Span b = new Span(r.errors().size() + " problem(s): nothing was saved");
                b.getElement().getThemeList().add("badge error");
                UnorderedList ul = new UnorderedList();
                r.errors().forEach(x -> ul.add(new ListItem(x)));
                out.add(b, ul);
                return;
            }
            String counts = r.facilities() + " facilities, " + r.lanes() + " lanes, " + r.stockRows() + " stock rows, " + r.bomRows() + " bill-of-materials rows";
            if (dry) {
                Span b = new Span("Looks good");
                b.getElement().getThemeList().add("badge success");
                out.add(b, new Paragraph(counts + ". Press \"Save as my digital twin\" to load them."));
            } else {
                Notification.show("Digital twin saved (v" + r.twinVersion() + ").", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                Span b = new Span("Saved as twin v" + r.twinVersion());
                b.getElement().getThemeList().add("badge success");
                Button map = new Button("See your network map", e -> UI.getCurrent().navigate(DigitalTwinGraphExplorerView.class));
                map.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                out.add(b, new Paragraph(counts + "."), map);
            }
        } catch (RuntimeException ex) {
            Notification.show("Import failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
