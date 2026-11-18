package com.supplygraph.web.components;

import com.supplygraph.application.graph.NetworkGraphView;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.*;

/**
 * Renders the digital twin network from a {@link NetworkGraphView} (persisted twin + active disruptions). Nothing on this component is
 * hard-coded: nodes, lanes, layers, disruption overlay, inventory and capacity all come from the view model. An empty twin shows an
 * explicit empty state instead of sample data.
 */
public class DigitalTwinNetworkGraphComponent extends VerticalLayout {
    private final NetworkGraphView view;
    private final Map<String, NetworkGraphView.Node> nodesById = new LinkedHashMap<>();
    private String filter = "ALL";
    private NetworkGraphView.Node selected;

    private final ComboBox<NetworkGraphView.Node> selector = new ComboBox<>();
    private final Div svgContainer = new Div();
    private final VerticalLayout inspector = new VerticalLayout();
    private final Map<String, Button> filterButtons = new LinkedHashMap<>();

    public DigitalTwinNetworkGraphComponent(NetworkGraphView view) {
        this.view = view;
        setWidthFull();
        setPadding(false);
        setSpacing(true);
        view.nodes().forEach(n -> nodesById.put(n.id(), n));

        if (view.isEmpty()) {
            H3 title = new H3("Supply chain network");
            Span empty = new Span("No supply chain is loaded for this workspace. Load one from Data Integration (import nodes, lanes and bill of materials) or load the sample network.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(title, empty);
            return;
        }
        selected = view.nodes().stream().filter(NetworkGraphView.Node::disrupted).findFirst().orElse(view.nodes().get(0));
        buildToolbar();
        buildSelectorBar();
        svgContainer.setWidthFull();
        svgContainer.getStyle().set("background", "#0b1220").set("border-radius", "10px").set("padding", "8px").set("overflow-x", "auto");
        add(svgContainer);
        inspector.setPadding(true);
        inspector.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "8px");
        add(inspector);
        render();
        updateInspector();
    }

    private void buildToolbar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);
        bar.getStyle().set("flex-wrap", "wrap");
        H3 title = new H3("Supply chain network");
        title.getStyle().set("margin", "0");
        Span badge = new Span("Twin v" + view.twinVersion() + " · " + view.nodes().size() + " facilities · " + view.lanes().size() + " lanes");
        badge.getElement().getThemeList().add("badge primary small");
        bar.add(title, badge);
        HorizontalLayout filters = new HorizontalLayout();
        filters.getStyle().set("margin-left", "auto").set("flex-wrap", "wrap");
        long suppliers = view.nodes().stream().filter(n -> n.type() == com.supplygraph.domain.model.graph.NodeType.SUPPLIER).count();
        addFilter(filters, "ALL", "All (" + view.nodes().size() + ")");
        addFilter(filters, "UPSTREAM", "Suppliers (" + suppliers + ")");
        addFilter(filters, "OTHER", "Sites, hubs & warehouses (" + (view.nodes().size() - suppliers) + ")");
        Button dis = addFilter(filters, "DISRUPTED", "Disrupted (" + view.disruptedNodeCount() + ")");
        dis.setIcon(VaadinIcon.WARNING.create());
        bar.add(filters);
        add(bar);
        styleFilters();
    }

    private Button addFilter(HorizontalLayout parent, String key, String label) {
        Button b = new Button(label, e -> { filter = key; styleFilters(); render(); });
        b.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterButtons.put(key, b);
        parent.add(b);
        return b;
    }

    private void styleFilters() {
        filterButtons.forEach((k, b) -> {
            b.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            if (k.equals(filter)) b.addThemeVariants(k.equals("DISRUPTED") ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
        });
    }

    private void buildSelectorBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);
        Span lbl = new Span("Inspect facility:");
        lbl.getStyle().set("font-weight", "600").set("white-space", "nowrap");
        selector.setWidth("360px");
        selector.setPlaceholder("Select a facility…");
        selector.setItemLabelGenerator(n -> n.id() + " — " + n.name());
        selector.setItems(view.nodes());
        selector.setValue(selected);
        selector.addValueChangeListener(e -> { if (e.getValue() != null) { selected = e.getValue(); updateInspector(); } });
        bar.add(lbl, selector);
        add(bar);
    }

    public void selectNodeById(String id) {
        NetworkGraphView.Node n = nodesById.get(id);
        if (n != null && !view.isEmpty()) { selected = n; selector.setValue(n); updateInspector(); }
    }

    private void updateInspector() {
        inspector.removeAll();
        if (selected == null) return;
        HorizontalLayout head = new HorizontalLayout();
        head.setAlignItems(Alignment.BASELINE);
        H4 title = new H4(selected.name() + " (" + selected.id() + ")");
        title.getStyle().set("margin", "0");
        Span status = new Span(selected.disrupted() ? (selected.active() ? "DISRUPTED" : "INACTIVE") : "OPERATIONAL");
        status.getElement().getThemeList().add(selected.disrupted() ? "badge error" : "badge success");
        Span tier = new Span(selected.tierLabel());
        tier.getElement().getThemeList().add("badge contrast");
        head.add(title, status, tier);
        inspector.add(head);

        HorizontalLayout kpis = new HorizontalLayout();
        kpis.getStyle().set("flex-wrap", "wrap").set("gap", "24px");
        kpis.add(kpi("Facility type", selected.type().name().replace('_', ' ')), kpi("Location", selected.location()),
                kpi("On-hand inventory (all materials)", String.format("%,.0f units", selected.onHandUnits())), kpi("Capacity / attributes", selected.capacityText()),
                kpi("Network stage", "Stage " + (selected.layer() + 1)));
        inspector.add(kpis);

        if (!selected.disruptionTitles().isEmpty()) {
            Div box = new Div();
            box.getStyle().set("background", "rgba(239,68,68,0.1)").set("border-left", "4px solid #ef4444").set("padding", "8px 12px").set("border-radius", "4px");
            Set<String> downstream = downstreamOf(selected.id());
            box.add(new Span("Active disruption(s): " + String.join("; ", selected.disruptionTitles()) + ". "
                    + (downstream.isEmpty() ? "No downstream facilities depend on this node via lanes."
                    : "Downstream facilities reachable through lanes (" + downstream.size() + "): " + String.join(", ", new TreeSet<>(downstream)) + ".")));
            inspector.add(box);
        } else {
            Set<String> hit = upstreamDisrupted(selected.id());
            if (!hit.isEmpty()) {
                Div box = new Div();
                box.getStyle().set("background", "rgba(245,158,11,0.1)").set("border-left", "4px solid #f59e0b").set("padding", "8px 12px").set("border-radius", "4px");
                box.add(new Span("Exposed: this facility is downstream of disrupted facility " + String.join(", ", new TreeSet<>(hit)) + "."));
                inspector.add(box);
            }
        }

        HorizontalLayout lanes = new HorizontalLayout();
        lanes.setWidthFull();
        lanes.add(laneColumn("Inbound lanes", view.lanes().stream().filter(l -> l.toId().equals(selected.id())).toList(), "None (network entry point)"),
                laneColumn("Outbound lanes", view.lanes().stream().filter(l -> l.fromId().equals(selected.id())).toList(), "None (network end point)"));
        inspector.add(lanes);
    }

    private VerticalLayout laneColumn(String header, List<NetworkGraphView.Lane> list, String none) {
        VerticalLayout col = new VerticalLayout();
        col.setPadding(false);
        col.setSpacing(false);
        Span h = new Span(header + " (" + list.size() + "):");
        h.getStyle().set("font-size", "11px").set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
        col.add(h);
        if (list.isEmpty()) col.add(new Span(none));
        for (NetworkGraphView.Lane l : list) {
            Span s = new Span(l.fromId() + " ➔ " + l.toId() + " (" + l.mode() + ", " + l.transitHours() + "h)" + (l.disrupted() ? " ⚠ DISRUPTED" : ""));
            s.getStyle().set("font-size", "12px");
            if (l.disrupted()) s.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "600");
            col.add(s);
        }
        return col;
    }

    private VerticalLayout kpi(String label, String value) {
        VerticalLayout v = new VerticalLayout();
        v.setPadding(false);
        v.setSpacing(false);
        Span l = new Span(label);
        l.getStyle().set("font-size", "11px").set("color", "var(--lumo-secondary-text-color)").set("text-transform", "uppercase");
        Span val = new Span(value);
        val.getStyle().set("font-weight", "600").set("font-size", "14px");
        v.add(l, val);
        return v;
    }

    Set<String> downstreamOf(String id) { return reach(id, true); }

    private Set<String> upstreamDisrupted(String id) {
        Set<String> out = new LinkedHashSet<>();
        for (String up : reach(id, false)) if (nodesById.get(up).disrupted()) out.add(up);
        return out;
    }

    private Set<String> reach(String start, boolean forward) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>(List.of(start));
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (NetworkGraphView.Lane l : view.lanes()) {
                String next = forward ? (l.fromId().equals(cur) ? l.toId() : null) : (l.toId().equals(cur) ? l.fromId() : null);
                if (next != null && !next.equals(start) && seen.add(next)) stack.push(next);
            }
        }
        return seen;
    }

    private boolean visible(NetworkGraphView.Node n) {
        return switch (filter) {
            case "UPSTREAM" -> n.type() == com.supplygraph.domain.model.graph.NodeType.SUPPLIER;
            case "OTHER" -> n.type() != com.supplygraph.domain.model.graph.NodeType.SUPPLIER;
            case "DISRUPTED" -> n.disrupted();
            default -> true;
        };
    }

    private void render() {
        svgContainer.removeAll();
        int layers = view.layers().size();
        int maxRows = view.nodes().stream().mapToInt(NetworkGraphView.Node::row).max().orElse(0) + 1;
        int colSpacing = 230, rowSpacing = 100;
        int width = Math.max(900, layers * colSpacing + 60), height = 90 + maxRows * rowSpacing + 40;
        int[] colX = new int[layers];
        for (int c = 0; c < layers; c++) colX[c] = 110 + c * colSpacing;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(' ').append(height).append("' width='").append(width).append("' height='").append(height)
                .append("' style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;'>\n<defs>")
                .append("<marker id='arrow' viewBox='0 0 10 10' refX='8' refY='5' markerWidth='6' markerHeight='6' orient='auto-start-reverse'><path d='M0 1L9 5L0 9z' fill='#64748b'/></marker>")
                .append("<marker id='arrow-d' viewBox='0 0 10 10' refX='8' refY='5' markerWidth='6' markerHeight='6' orient='auto-start-reverse'><path d='M0 1L9 5L0 9z' fill='#ef4444'/></marker>")
                .append("</defs>\n");
        for (NetworkGraphView.Layer layer : view.layers()) {
            sb.append("<text x='").append(colX[layer.index()]).append("' y='28' fill='#94a3b8' font-size='11' font-weight='700' text-anchor='middle'>").append(esc(layer.label())).append("</text>\n");
        }
        for (NetworkGraphView.Lane lane : view.lanes()) {
            NetworkGraphView.Node s = nodesById.get(lane.fromId()), d = nodesById.get(lane.toId());
            if (s == null || d == null || !visible(s) || !visible(d)) continue;
            int x1 = colX[s.layer()] + 75, y1 = 90 + s.row() * rowSpacing + 29, x2 = colX[d.layer()] - 75, y2 = 90 + d.row() * rowSpacing + 29, dx = Math.abs(x2 - x1) / 2;
            String color = lane.disrupted() ? "#ef4444" : "#475569";
            String dash = "AIR".equals(lane.mode()) ? " stroke-dasharray='4,4'" : "OCEAN".equals(lane.mode()) ? " stroke-dasharray='8,4'" : "";
            sb.append("<path d='M").append(x1).append(' ').append(y1).append(" C").append(x1 + dx).append(' ').append(y1).append(',').append(x2 - dx).append(' ').append(y2).append(',').append(x2).append(' ').append(y2)
                    .append("' fill='none' stroke='").append(color).append("' stroke-width='").append(lane.disrupted() ? "2.5" : "1.6").append("'").append(dash)
                    .append(" marker-end='url(#").append(lane.disrupted() ? "arrow-d" : "arrow").append(")' opacity='").append(lane.disrupted() ? "1" : "0.75").append("'/>\n");
            int mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
            sb.append("<rect x='").append(mx - 26).append("' y='").append(my - 9).append("' width='52' height='16' rx='3' fill='#0f172a' stroke='").append(color).append("'/>")
                    .append("<text x='").append(mx).append("' y='").append(my + 3).append("' fill='#cbd5e1' font-size='9' text-anchor='middle'>").append(esc(lane.mode())).append(' ').append(lane.transitHours()).append("h</text>\n");
        }
        for (NetworkGraphView.Node n : view.nodes()) {
            if (!visible(n)) continue;
            int rx = colX[n.layer()] - 75, ry = 90 + n.row() * rowSpacing;
            String stroke = n.disrupted() ? "#ef4444" : switch (n.type()) {
                case SUPPLIER -> "#3b82f6"; case MANUFACTURING_SITE -> "#10b981"; case LOGISTICS_HUB -> "#f59e0b"; case WAREHOUSE -> "#06b6d4";
            };
            sb.append("<g><rect x='").append(rx).append("' y='").append(ry).append("' width='150' height='58' rx='8' fill='").append(n.disrupted() ? "#450a0a" : "#1e293b")
                    .append("' stroke='").append(stroke).append("' stroke-width='").append(n.disrupted() ? "2" : "1.2").append("'/>")
                    .append("<rect x='").append(rx + 8).append("' y='").append(ry + 8).append("' width='52' height='14' rx='3' fill='").append(stroke).append("' opacity='0.9'/>")
                    .append("<text x='").append(rx + 34).append("' y='").append(ry + 19).append("' fill='#fff' font-size='9' font-weight='bold' text-anchor='middle'>").append(esc(n.tierLabel())).append("</text>");
            if (n.disrupted()) sb.append("<text x='").append(rx + 138).append("' y='").append(ry + 20).append("' fill='#ef4444' font-size='12' text-anchor='middle'>⚠</text>");
            String id = n.id().length() > 22 ? n.id().substring(0, 20) + ".." : n.id();
            String loc = n.location().length() > 22 ? n.location().substring(0, 20) + ".." : n.location();
            sb.append("<text x='").append(rx + 8).append("' y='").append(ry + 37).append("' fill='#f8fafc' font-size='11' font-weight='700'>").append(esc(id)).append("</text>")
                    .append("<text x='").append(rx + 8).append("' y='").append(ry + 50).append("' fill='#94a3b8' font-size='9'>").append(esc(loc)).append("</text></g>\n");
        }
        sb.append("</svg>");
        svgContainer.add(new Html("<div>" + sb + "</div>"));
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"); }
}
