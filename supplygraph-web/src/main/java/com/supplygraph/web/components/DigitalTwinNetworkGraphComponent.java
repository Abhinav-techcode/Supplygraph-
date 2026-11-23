package com.supplygraph.web.components;

import com.supplygraph.application.graph.NetworkGraphView;
import com.supplygraph.domain.model.graph.NodeType;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The digital-twin network map, written entirely in Java (Vaadin components + server-generated SVG + CSS from the theme).
 * Facilities are real components positioned on a scalable, natively scrollable canvas; lanes are an SVG layer drawn from the twin.
 * Selecting a facility traces everything upstream and downstream of it. Overlays show impact, route and agent targets.
 * Nothing is hard-coded: an empty twin shows an explicit empty state.
 */
public class DigitalTwinNetworkGraphComponent extends VerticalLayout {
    private static final int NW = 176, NH = 66, COL_GAP = 280, ROW_GAP = 104, LEFT = 40, TOP = 70;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private final NetworkGraphView view;
    private final int instance = SEQ.incrementAndGet();
    private final Map<String, NetworkGraphView.Node> nodesById = new LinkedHashMap<>();
    private final Map<String, Div> nodeDivs = new LinkedHashMap<>();
    private final Div viewport = new Div();
    private final Div sizer = new Div();
    private final Div world = new Div();
    private final Div lanesLayer = new Div();
    private final VerticalLayout inspector = new VerticalLayout();
    private final Map<String, Button> filterButtons = new LinkedHashMap<>();
    private final Span zoomLabel = new Span();
    private NetworkGraphView.Node selected;
    private boolean traceOn = true;
    private boolean traced;   // trace only after the user picks a facility, so the first view shows the whole network
    private String filter = "ALL";
    private double zoom;
    private int worldW, worldH;
    private final Map<String, int[]> pos = new HashMap<>();   // positions the user dragged nodes to (x, y in world pixels); absent = automatic layout
    private Overlay overlay = Overlay.none();

    /** What to emphasise on top of the twin: nodes reached by the disruption, the recovery route (in order), and nodes an agent or plan acts on. */
    public record Overlay(Set<String> impactedNodes, List<String> routeNodes, Set<String> markedNodes) {
        public static Overlay none() { return new Overlay(Set.of(), List.of(), Set.of()); }
        public boolean isEmpty() { return impactedNodes.isEmpty() && routeNodes.isEmpty() && markedNodes.isEmpty(); }
    }

    public DigitalTwinNetworkGraphComponent(NetworkGraphView view) {
        this.view = view;
        setWidthFull();
        setPadding(false);
        setSpacing(true);
        addClassName("sg-network");
        view.nodes().forEach(n -> nodesById.put(n.id(), n));
        computeWorld();
        zoom = Math.min(1.0, 1000.0 / worldW);

        if (view.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("sg-empty");
            empty.add(new H3("No supply chain loaded"), new Span("This workspace has no facilities yet. Load the sample supply chain or import your own data from the home page (Step 1)."));
            add(empty);
            return;
        }
        selected = view.nodes().stream().filter(NetworkGraphView.Node::disrupted).findFirst().orElse(view.nodes().get(0));
        add(toolbar());
        add(legend());

        viewport.addClassName("sg-graph-canvas");
        sizer.addClassName("sg-graph-sizer");
        world.addClassName("sg-graph-world");
        lanesLayer.addClassName("sg-graph-lanes");
        world.add(lanesLayer);
        for (NetworkGraphView.Node n : view.nodes()) {
            Div d = nodeCard(n);
            nodeDivs.put(n.id(), d);
            world.add(d);
        }
        sizer.add(world);
        viewport.add(sizer);
        viewport.getElement().addEventListener("sg-node-moved", e -> {
            String id = e.getEventData().getString("event.detail.id");
            if (!nodesById.containsKey(id)) return;
            pos.put(id, new int[]{(int) e.getEventData().getNumber("event.detail.x"), (int) e.getEventData().getNumber("event.detail.y")});
            computeWorld();
            refresh();
        }).addEventData("event.detail.id").addEventData("event.detail.x").addEventData("event.detail.y");
        viewport.getElement().executeJs(DRAG_SCRIPT, viewport.getElement(), world.getElement(), NW, NH);
        Div hint = new Div(new Span("Drag a facility to move it · drag the background to pan · + / − to zoom · click a facility to trace its supply path"));
        hint.addClassName("sg-graph-hint");
        viewport.getElement().getStyle().set("position", "relative");

        inspector.setPadding(true);
        inspector.setSpacing(false);
        inspector.addClassName("sg-inspector");
        Div row = new Div(new Div(viewport, hint), inspector);
        row.addClassName("sg-network-row");
        add(row);
        refresh();
    }

    // ---- toolbar ----

    private HorizontalLayout toolbar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);
        bar.addClassName("sg-graph-toolbar");

        Span badge = new Span("Twin v" + view.twinVersion() + " · " + view.nodes().size() + " facilities · " + view.lanes().size() + " lanes");
        badge.getElement().getThemeList().add("badge contrast");
        bar.add(badge);
        if (view.disruptedNodeCount() > 0) {
            Span d = new Span(view.disruptedNodeCount() + " disrupted");
            d.getElement().getThemeList().add("badge error");
            bar.add(d);
        }
        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");

        ComboBox<NetworkGraphView.Node> search = new ComboBox<>();
        search.setPlaceholder("Find a facility…");
        search.setClearButtonVisible(true);
        search.setWidth("240px");
        search.setItems(view.nodes());
        search.setItemLabelGenerator(n -> n.name() + "  ·  " + n.id());
        search.addValueChangeListener(e -> { if (e.getValue() != null) selectNodeById(e.getValue().id()); });
        search.addClassName("sg-graph-search");

        HorizontalLayout filters = new HorizontalLayout();
        filters.setSpacing(false);
        filters.getStyle().set("gap", "6px");
        long suppliers = view.nodes().stream().filter(n -> n.type() == NodeType.SUPPLIER).count();
        addFilter(filters, "ALL", "All");
        addFilter(filters, "SUPPLIER", "Suppliers " + suppliers);
        addFilter(filters, "SITES", "Sites & hubs " + (view.nodes().size() - suppliers));
        addFilter(filters, "DISRUPTED", "Disrupted " + view.disruptedNodeCount());
        styleFilters();

        Button out = new Button("−", e -> setZoom(zoom - 0.1));
        Button in = new Button("+", e -> setZoom(zoom + 0.1));
        Button fit = new Button("Fit", e -> setZoom(Math.min(1.0, 1000.0 / worldW)));
        Button reset = new Button("Reset layout", e -> { pos.clear(); computeWorld(); refresh(); });
        reset.getElement().setAttribute("title", "Put every facility back in its automatic position");
        Button trace = new Button("Trace on", e -> { traceOn = !traceOn; ((Button) e.getSource()).setText(traceOn ? "Trace on" : "Trace off"); refresh(); });
        for (Button b : List.of(out, in, fit, reset, trace)) b.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
        out.getElement().setAttribute("title", "Zoom out");
        in.getElement().setAttribute("title", "Zoom in");
        fit.getElement().setAttribute("title", "Fit the network to the width");
        trace.getElement().setAttribute("title", "Highlight everything upstream and downstream of the selected facility");
        zoomLabel.addClassName("sg-zoom-label");
        HorizontalLayout zoomBar = new HorizontalLayout(out, zoomLabel, in, fit, reset, trace);
        zoomBar.setSpacing(false);
        zoomBar.setAlignItems(Alignment.CENTER);
        zoomBar.getStyle().set("gap", "4px");
        bar.add(spacer, search, filters, zoomBar);
        return bar;
    }

    private void addFilter(HorizontalLayout parent, String key, String label) {
        Button b = new Button(label, e -> { filter = key; styleFilters(); refresh(); });
        b.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterButtons.put(key, b);
        parent.add(b);
    }

    private void styleFilters() {
        filterButtons.forEach((k, b) -> {
            b.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            if (k.equals(filter)) b.addThemeVariants(k.equals("DISRUPTED") ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_PRIMARY);
        });
    }

    private Div legend() {
        Div l = new Div();
        l.addClassName("sg-graph-legend");
        String[][] items = {{"sg-c-disrupted", "Disrupted"}, {"sg-c-impacted", "Impacted downstream"}, {"sg-c-route", "Recovery route"}, {"sg-c-marked", "Agent / plan acts here"},
                {"sg-c-supplier", "Supplier"}, {"sg-c-site", "Manufacturing site"}, {"sg-c-hub", "Logistics hub"}, {"sg-c-wh", "Warehouse"}};
        for (String[] it : items) {
            Span dot = new Span();
            dot.addClassNames("sg-dot", it[0]);
            l.add(new Span(dot, new Span(it[1])));
        }
        return l;
    }

    private void setZoom(double z) {
        zoom = Math.max(0.35, Math.min(1.6, z));
        refresh();
    }

    // ---- layout ----

    private int px(NetworkGraphView.Node n) { int[] p = pos.get(n.id()); return p != null ? p[0] : LEFT + n.layer() * COL_GAP; }
    private int py(NetworkGraphView.Node n) { int[] p = pos.get(n.id()); return p != null ? p[1] : TOP + n.row() * ROW_GAP; }

    /** The drawing area always contains every node, wherever the user has dragged it. */
    private void computeWorld() {
        int layers = Math.max(1, view.layers().size());
        int w = LEFT * 2 + (layers - 1) * COL_GAP + NW, h = TOP + NH + 30;
        for (NetworkGraphView.Node n : view.nodes()) { w = Math.max(w, px(n) + NW + LEFT); h = Math.max(h, py(n) + NH + 30); }
        worldW = w;
        worldH = h;
    }

    /** Browser-side pointer handling only: drag a facility (its lanes follow live and the final position is sent to the server), or drag the background to pan. */
    private static final String DRAG_SCRIPT = """
            const vp = $0, world = $1, W = $2, H = $3;
            if (vp.__sgDrag) return;
            vp.__sgDrag = true;
            const scale = () => { const m = /scale\\(([0-9.]+)\\)/.exec(world.style.transform); return m ? parseFloat(m[1]) : 1; };
            const nodeOf = id => world.querySelector('.sg-gnode[data-id="' + id.replace(/"/g, '') + '"]');
            const route = (a, b) => {
              let x1 = parseFloat(a.style.left) + W, y1 = parseFloat(a.style.top) + H / 2, x2 = parseFloat(b.style.left), y2 = parseFloat(b.style.top) + H / 2;
              if (x2 < x1 + 10) { x1 -= W / 2; x2 += W / 2; y1 += y2 > y1 ? H / 2 : -H / 2; y2 += y2 > y1 ? -H / 2 : H / 2; }
              const dx = Math.max(40, Math.abs(x2 - x1) / 2);
              return 'M' + Math.round(x1) + ' ' + Math.round(y1) + ' C' + Math.round(x1 + dx) + ' ' + Math.round(y1) + ',' + Math.round(x2 - dx) + ' ' + Math.round(y2) + ',' + Math.round(x2) + ' ' + Math.round(y2);
            };
            const reroute = id => world.querySelectorAll('path[data-f]').forEach(p => {
              if (p.dataset.f !== id && p.dataset.t !== id) return;
              const a = nodeOf(p.dataset.f), b = nodeOf(p.dataset.t);
              if (a && b) p.setAttribute('d', route(a, b));
            });
            let mode = null, start = null, node = null, moved = false;
            const move = e => {
              const dx = e.clientX - start.x, dy = e.clientY - start.y;
              if (!moved && Math.abs(dx) + Math.abs(dy) < 4) return;
              moved = true;
              if (mode === 'pan') { vp.scrollLeft = start.sl - dx; vp.scrollTop = start.st - dy; return; }
              const s = scale();
              node.style.left = Math.max(0, start.l + dx / s) + 'px';
              node.style.top = Math.max(0, start.t + dy / s) + 'px';
              node.classList.add('sg-dragging');
              reroute(node.dataset.id);
            };
            const up = () => {
              window.removeEventListener('pointermove', move);
              window.removeEventListener('pointerup', up);
              vp.classList.remove('sg-panning');
              if (mode === 'node' && moved) {
                node.classList.remove('sg-dragging');
                node.addEventListener('click', ev => ev.stopImmediatePropagation(), { capture: true, once: true });
                vp.dispatchEvent(new CustomEvent('sg-node-moved', { detail: { id: node.dataset.id, x: Math.round(parseFloat(node.style.left)), y: Math.round(parseFloat(node.style.top)) } }));
              }
              mode = null;
            };
            vp.addEventListener('pointerdown', e => {
              if (e.button !== 0 || e.target.closest('button,input,a')) return;
              const n = e.target.closest('.sg-gnode');
              if (n && world.contains(n)) { mode = 'node'; node = n; start = { x: e.clientX, y: e.clientY, l: parseFloat(n.style.left), t: parseFloat(n.style.top) }; }
              else { mode = 'pan'; start = { x: e.clientX, y: e.clientY, sl: vp.scrollLeft, st: vp.scrollTop }; vp.classList.add('sg-panning'); }
              moved = false;
              window.addEventListener('pointermove', move);
              window.addEventListener('pointerup', up);
            });
            """;

    // ---- nodes ----

    private Div nodeCard(NetworkGraphView.Node n) {
        Div d = new Div();
        d.addClassNames("sg-gnode", typeClass(n.type()));
        d.getStyle().set("left", px(n) + "px").set("top", py(n) + "px").set("width", NW + "px").set("height", NH + "px");
        d.getElement().setAttribute("data-id", n.id());
        d.getElement().setAttribute("title", n.name() + " (" + n.id() + ")\n" + n.type().name().replace('_', ' ').toLowerCase() + " · " + n.location()
                + (n.disrupted() ? "\nDISRUPTED: " + String.join("; ", n.disruptionTitles()) : "") + "\nClick to inspect and trace");
        Span icon = new Span(switch (n.type()) {
            case SUPPLIER -> VaadinIcon.CUBES.create();
            case MANUFACTURING_SITE -> VaadinIcon.FACTORY.create();
            case LOGISTICS_HUB -> VaadinIcon.TRUCK.create();
            case WAREHOUSE -> VaadinIcon.STORAGE.create();
        });
        icon.addClassName("sg-gnode-icon");
        Span name = new Span(trunc(n.id(), 22));
        name.addClassName("sg-gnode-name");
        Span loc = new Span(trunc(n.name().isBlank() ? n.location() : n.name(), 30));
        loc.addClassName("sg-gnode-loc");
        long ships = view.lanes().stream().filter(l -> l.fromId().equals(n.id())).count();
        boolean hasStock = n.onHandUnits() > 0;
        // A supplier ships to others instead of holding tracked stock, so say what it feeds rather than "no stock recorded".
        Span stock = new Span(hasStock ? String.format("%,.0f units on hand" , n.onHandUnits())
                : n.type() == NodeType.SUPPLIER ? "ships to " + ships + (ships == 1 ? " site" : " sites") : "no stock recorded");
        stock.addClassName(hasStock || n.type() == NodeType.SUPPLIER ? "sg-gnode-stock" : "sg-gnode-nostock");
        Span tier = new Span(n.tierLabel());
        tier.addClassName("sg-gnode-tier");
        d.add(icon, name, loc, stock, tier);
        if (n.disrupted()) {
            Span warn = new Span(VaadinIcon.WARNING.create());
            warn.addClassName("sg-gnode-warn");
            d.add(warn);
        }
        d.addClickListener(e -> selectNodeById(n.id()));
        return d;
    }

    private static String typeClass(NodeType t) {
        return switch (t) { case SUPPLIER -> "sg-t-supplier"; case MANUFACTURING_SITE -> "sg-t-site"; case LOGISTICS_HUB -> "sg-t-hub"; case WAREHOUSE -> "sg-t-wh"; };
    }

    private static String trunc(String t, int max) { return t == null ? "" : t.length() > max ? t.substring(0, max - 1) + "\u2026" : t; }

    // ---- state ----

    public void setOverlay(Overlay o) {
        this.overlay = o == null ? Overlay.none() : o;
        if (!view.isEmpty()) refresh();
    }

    public void selectNodeById(String id) {
        NetworkGraphView.Node n = nodesById.get(id);
        if (n == null || view.isEmpty()) return;
        selected = n;
        traced = true;
        refresh();
        updateInspector();
        Div d = nodeDivs.get(id);
        if (d != null) d.getElement().scrollIntoView();
    }

    private boolean visible(NetworkGraphView.Node n) {
        return switch (filter) {
            case "SUPPLIER" -> n.type() == NodeType.SUPPLIER;
            case "SITES" -> n.type() != NodeType.SUPPLIER;
            case "DISRUPTED" -> n.disrupted();
            default -> true;
        };
    }

    private boolean routeLane(String from, String to) {
        List<String> r = overlay.routeNodes();
        for (int i = 0; i + 1 < r.size(); i++) if (r.get(i).equals(from) && r.get(i + 1).equals(to)) return true;
        return false;
    }

    /** Re-applies zoom, filter, overlay, selection and trace to the nodes and redraws the lane layer. */
    private void refresh() {
        if (view.isEmpty()) return;
        sizer.getStyle().set("width", Math.round(worldW * zoom) + "px").set("height", Math.round(worldH * zoom) + "px");
        world.getStyle().set("width", worldW + "px").set("height", worldH + "px").set("transform", "scale(" + zoom + ")");
        zoomLabel.setText(Math.round(zoom * 100) + "%");
        Set<String> trace = traceOn && traced && selected != null ? traceSet(selected.id()) : Set.of();
        for (NetworkGraphView.Node n : view.nodes()) {
            Div d = nodeDivs.get(n.id());
            d.getStyle().set("left", px(n) + "px").set("top", py(n) + "px");
            var cl = d.getClassNames();
            for (String c : List.of("sg-disrupted", "sg-impacted", "sg-route", "sg-marked", "sg-selected", "sg-dim")) cl.set(c, false);
            cl.set("sg-disrupted", n.disrupted());
            cl.set("sg-impacted", !n.disrupted() && overlay.impactedNodes().contains(n.id()));
            cl.set("sg-route", overlay.routeNodes().contains(n.id()));
            cl.set("sg-marked", overlay.markedNodes().contains(n.id()));
            cl.set("sg-selected", selected != null && selected.id().equals(n.id()));
            cl.set("sg-dim", !visible(n) || (!trace.isEmpty() && !trace.contains(n.id())));
        }
        lanesLayer.getElement().setProperty("innerHTML", lanesSvg(trace));
    }

    private Set<String> traceSet(String id) {
        Set<String> seen = new LinkedHashSet<>(List.of(id));
        seen.addAll(reach(id, true));
        seen.addAll(reach(id, false));
        return seen;
    }

    private String lanesSvg(Set<String> trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(worldW).append("' height='").append(worldH).append("' viewBox='0 0 ").append(worldW).append(' ').append(worldH).append("'>");
        sb.append("<defs>");
        for (String[] m : new String[][]{{"a", "#64748b"}, {"d", "#ef4444"}, {"i", "#f59e0b"}, {"r", "#22c55e"}})
            sb.append("<marker id='m").append(instance).append(m[0]).append("' viewBox='0 0 10 10' refX='9' refY='5' markerWidth='7' markerHeight='7' orient='auto-start-reverse'><path d='M0 1L9 5L0 9z' fill='").append(m[1]).append("'/></marker>");
        sb.append("</defs>");
        for (NetworkGraphView.Layer layer : view.layers()) {
            long suppliers = view.nodes().stream().filter(n -> n.layer() == layer.index() && n.type() == NodeType.SUPPLIER).count();
            long total = view.nodes().stream().filter(n -> n.layer() == layer.index()).count();
            String what = suppliers == total ? "suppliers" : suppliers == 0 ? "sites & hubs" : "suppliers + sites";
            int x = LEFT + layer.index() * COL_GAP + NW / 2;
            sb.append("<text x='").append(x).append("' y='28' text-anchor='middle' class='sg-svg-h1'>Tier ").append(layer.index() + 1).append("</text>");
            sb.append("<text x='").append(x).append("' y='44' text-anchor='middle' class='sg-svg-h2'>").append(total).append(' ').append(what).append("</text>");
        }
        for (NetworkGraphView.Lane l : view.lanes()) {
            NetworkGraphView.Node a = nodesById.get(l.fromId()), b = nodesById.get(l.toId());
            if (a == null || b == null) continue;
            boolean onRoute = routeLane(l.fromId(), l.toId());
            boolean impacted = !onRoute && (a.disrupted() || overlay.impactedNodes().contains(a.id())) && overlay.impactedNodes().contains(b.id());
            boolean dim = !visible(a) || !visible(b) || (!trace.isEmpty() && !(trace.contains(a.id()) && trace.contains(b.id())));
            String color = onRoute ? "#22c55e" : l.disrupted() ? "#ef4444" : impacted ? "#f59e0b" : "#475569";
            String mk = onRoute ? "r" : l.disrupted() ? "d" : impacted ? "i" : "a";
            int x1 = px(a) + NW, y1 = py(a) + NH / 2, x2 = px(b), y2 = py(b) + NH / 2;
            if (x2 < x1 + 10) { x1 -= NW / 2; x2 += NW / 2; y1 += y2 > y1 ? NH / 2 : -NH / 2; y2 += y2 > y1 ? -NH / 2 : NH / 2; }
            int dx = Math.max(40, Math.abs(x2 - x1) / 2);
            String dash = onRoute || impacted ? " class='sg-lane-flow'" : "AIR".equals(l.mode()) ? " stroke-dasharray='3 5'" : "OCEAN".equals(l.mode()) ? " stroke-dasharray='9 5'" : "";
            double width = onRoute ? 3.6 : (l.disrupted() || impacted) ? 2.6 : 1.6;
            double op = dim ? 0.07 : (onRoute || impacted || l.disrupted()) ? 1 : 0.7;
            sb.append("<path data-f='").append(esc(a.id())).append("' data-t='").append(esc(b.id())).append("' d='M").append(x1).append(' ').append(y1).append(" C").append(x1 + dx).append(' ').append(y1).append(',').append(x2 - dx).append(' ').append(y2).append(',').append(x2).append(' ').append(y2)
                    .append("' fill='none' stroke='").append(color).append("' stroke-width='").append(width).append("' stroke-linecap='round' opacity='").append(op).append("' marker-end='url(#m").append(instance).append(mk).append(")'").append(dash).append("/>");
            if (!dim && (onRoute || impacted || l.disrupted() || !trace.isEmpty())) {
                sb.append("<text x='").append((x1 + x2) / 2).append("' y='").append((y1 + y2) / 2 - 5).append("' text-anchor='middle' class='sg-svg-lane' fill='").append(color.equals("#475569") ? "#94a3b8" : color).append("'>")
                        .append(esc(l.mode())).append(' ').append(l.transitHours()).append("h</text>");
            }
        }
        return sb.append("</svg>").toString();
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;"); }

    // ---- inspector ----

    private void updateInspector() {
        inspector.removeAll();
        if (selected == null) return;
        H4 title = new H4(selected.name());
        title.getStyle().set("margin", "0");
        Span id = new Span(selected.id());
        id.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");
        Span status = new Span(selected.disrupted() ? (selected.active() ? "DISRUPTED" : "INACTIVE") : "OPERATIONAL");
        status.getElement().getThemeList().add(selected.disrupted() ? "badge error small" : "badge success small");
        Span tier = new Span(selected.tierLabel());
        tier.getElement().getThemeList().add("badge contrast small");
        HorizontalLayout tags = new HorizontalLayout(status, tier);
        tags.getStyle().set("margin", "6px 0 10px");
        inspector.add(title, id, tags);

        inspector.add(fact("Type", selected.type().name().replace('_', ' ').toLowerCase()), fact("Location", selected.location()),
                fact("Stock on hand", selected.onHandUnits() > 0 ? String.format("%,.0f units (all materials)", selected.onHandUnits()) : "none recorded"),
                fact("Capacity", selected.capacityText()), fact("Network tier", "Tier " + (selected.layer() + 1)));

        if (!selected.disruptionTitles().isEmpty()) {
            Set<String> downstream = reach(selected.id(), true);
            inspector.add(note("#ef4444", "Active disruption: " + String.join("; ", selected.disruptionTitles()) + ". "
                    + (downstream.isEmpty() ? "Nothing downstream depends on it." : downstream.size() + " downstream facilities depend on it: " + String.join(", ", new TreeSet<>(downstream)) + ".")));
        } else {
            Set<String> hit = new LinkedHashSet<>();
            for (String up : reach(selected.id(), false)) if (nodesById.get(up).disrupted()) hit.add(up);
            if (!hit.isEmpty()) inspector.add(note("#f59e0b", "Exposed: downstream of disrupted " + String.join(", ", new TreeSet<>(hit)) + "."));
        }
        long factories = reach(selected.id(), true).stream().filter(i -> nodesById.get(i).type() == NodeType.MANUFACTURING_SITE).count();
        inspector.add(note("#3b82f6", selected.type() == NodeType.MANUFACTURING_SITE ? "This is a factory: it uses up stock every day."
                : factories == 0 ? "No factory depends on this site." : factories + " factor" + (factories == 1 ? "y" : "ies") + " sit downstream of this site."));
        Button simulate = new Button("What if this stops?", com.vaadin.flow.component.icon.VaadinIcon.PLAY.create(),
                e -> com.vaadin.flow.component.UI.getCurrent().navigate("simulation", com.vaadin.flow.router.QueryParameters.simple(Map.of("site", selected.id()))));
        simulate.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        simulate.addClassName("sg-what-if");
        inspector.add(simulate);
        inspector.add(laneList("Inbound lanes", view.lanes().stream().filter(l -> l.toId().equals(selected.id())).toList(), true));
        inspector.add(laneList("Outbound lanes", view.lanes().stream().filter(l -> l.fromId().equals(selected.id())).toList(), false));
    }

    private Div fact(String label, String value) {
        Div d = new Div();
        d.getStyle().set("display", "flex").set("justify-content", "space-between").set("gap", "12px").set("padding", "5px 0").set("border-bottom", "1px solid var(--lumo-contrast-5pct)").set("font-size", "var(--lumo-font-size-s)");
        Span l = new Span(label);
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span v = new Span(value);
        v.getStyle().set("font-weight", "600").set("text-align", "right");
        d.add(l, v);
        return d;
    }

    private Div note(String color, String text) {
        Div box = new Div(new Span(text));
        box.getStyle().set("background", color + "1a").set("border-left", "4px solid " + color).set("padding", "8px 10px").set("border-radius", "4px").set("margin", "10px 0").set("font-size", "var(--lumo-font-size-s)");
        return box;
    }

    private Div laneList(String header, List<NetworkGraphView.Lane> list, boolean inbound) {
        Div col = new Div();
        col.getStyle().set("margin-top", "10px");
        Span h = new Span(header + " (" + list.size() + ")");
        h.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("font-weight", "700").set("color", "var(--lumo-secondary-text-color)").set("text-transform", "uppercase").set("display", "block");
        col.add(h);
        if (list.isEmpty()) {
            Span none = new Span(inbound ? "None: network entry point" : "None: network end point");
            none.getStyle().set("font-size", "var(--lumo-font-size-s)");
            col.add(none);
        }
        for (NetworkGraphView.Lane l : list) {
            String other = inbound ? l.fromId() : l.toId();
            Button b = new Button((inbound ? "← " : "→ ") + other + "  ·  " + l.mode() + " " + l.transitHours() + "h" + (l.disrupted() ? "  ⚠" : ""), e -> selectNodeById(other));
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            b.getStyle().set("display", "block").set("justify-content", "flex-start").set("text-align", "left").set("height", "auto").set("padding", "2px 0");
            if (l.disrupted()) b.getStyle().set("color", "var(--lumo-error-text-color)");
            col.add(b);
        }
        return col;
    }

    Set<String> downstreamOf(String id) { return reach(id, true); }

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
}
