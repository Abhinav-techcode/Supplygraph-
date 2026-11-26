package com.supplygraph.web.views;

import com.supplygraph.application.graph.NetworkGraphView;
import com.supplygraph.application.graph.NetworkGraphViewService;
import com.supplygraph.application.service.SimulationApplicationService;
import com.supplygraph.application.service.SimulationApplicationService.WhatIfOutcome;
import com.supplygraph.application.service.SupplyChainImportService;
import com.supplygraph.application.service.SyntheticNetworkGenerator;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.NodeType;
import com.supplygraph.simulation.engine.SimulationResult;
import com.supplygraph.simulation.state.TrajectoryPoint;
import com.supplygraph.web.init.DemoDataInitializer;
import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Business simulator, in three plain steps: (1) choose the kind of supply chain, (2) choose which site stops and for how long, (3) read what it costs you and what helps.
 * Every number comes from running the simulation engine on the current digital twin.
 */
@Route(value = "simulation", layout = MainLayout.class)
@PageTitle("Business Simulation | ORBIX")
public class SimulationWorkspaceView extends VerticalLayout implements com.vaadin.flow.router.BeforeEnterObserver {
    private static final String[] COLOURS = {"#6b7280", "#2563eb", "#16a34a", "#d97706", "#9333ea", "#dc2626"};

    private final SimulationApplicationService simulation;
    private final NetworkGraphViewService graphService;
    private final SupplyChainImportService importService;
    private final SyntheticNetworkGenerator generator;

    private final Div network = new Div();
    /** One thing that can go wrong: a set of sites that stop together. */
    private record Choice(String phrase, Set<String> siteIds) {}

    private final ComboBox<Choice> site = new ComboBox<>("What goes wrong?");
    private final Select<Integer> outage = new Select<>();
    private final Select<Integer> horizon = new Select<>();
    private final Div results = new Div();
    private List<Choice> choices = new ArrayList<>();
    private Map<Choice, BigDecimal> impact = new HashMap<>();

    public SimulationWorkspaceView() {
        this(null, null, null, null);
    }

    @Autowired
    public SimulationWorkspaceView(@Autowired(required = false) SimulationApplicationService simulation,
                                   @Autowired(required = false) NetworkGraphViewService graphService,
                                   @Autowired(required = false) SupplyChainImportService importService,
                                   @Autowired(required = false) SyntheticNetworkGenerator generator) {
        this.simulation = simulation;
        this.graphService = graphService;
        this.importService = importService;
        this.generator = generator;
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        add(new H2("Business simulator"),
                new Paragraph("The question this page answers: if one site in your supply chain stops for a while, how many units can you not supply to customers, and would any recovery action help? "
                        + "It runs your supply chain forward day by day on the stock you actually hold. Nothing you do here changes your real data."),
                new H3("1. Your supply chain"), network, buildPicker(),
                new H3("2. Pick what goes wrong"), buildQuestion(),
                results);
        refreshNetwork();
    }

    /** Arriving from the map with ?site=ID pre-selects "that site stops". */
    @Override
    public void beforeEnter(com.vaadin.flow.router.BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getParameters().getOrDefault("site", List.of()).stream().findFirst()
                .flatMap(id -> choices.stream().filter(c -> c.siteIds().equals(Set.of(id))).findFirst())
                .ifPresent(site::setValue);
    }

    private static WorkspaceId workspace() {
        return WorkspaceId.of(DemoDataInitializer.DEMO_WORKSPACE_ID);
    }

    /** Lets you pick a different kind of supply chain (industry, region, size) and load it. */
    private Component buildPicker() {
        Select<SyntheticNetworkGenerator.Industry> industry = new Select<>();
        industry.setLabel("Industry");
        industry.setItems(SyntheticNetworkGenerator.Industry.values());
        industry.setItemLabelGenerator(i -> capitalise(i.name()));
        industry.setValue(SyntheticNetworkGenerator.Industry.AUTOMOTIVE);
        Select<SyntheticNetworkGenerator.Region> region = new Select<>();
        region.setLabel("Where");
        region.setItems(SyntheticNetworkGenerator.Region.values());
        region.setItemLabelGenerator(r -> capitalise(r.name().replace('_', ' ')));
        region.setValue(SyntheticNetworkGenerator.Region.EUROPE);
        Select<SyntheticNetworkGenerator.Size> size = new Select<>();
        size.setLabel("How big");
        size.setItems(SyntheticNetworkGenerator.Size.values());
        size.setItemLabelGenerator(s -> capitalise(s.name()));
        size.setValue(SyntheticNetworkGenerator.Size.MEDIUM);
        Button build = new Button("Use this kind of supply chain instead", VaadinIcon.REFRESH.create(), e -> {
            if (generator == null || importService == null) return;
            try {
                var d = generator.generate(industry.getValue(), region.getValue(), size.getValue(), System.nanoTime());
                var r = importService.importCsv(workspace(), d.facilities(), d.lanes(), d.stock(), d.bom(), false);
                if (!r.errors().isEmpty()) {
                    Notification.show("That supply chain could not be loaded: " + r.errors().getFirst(), 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                Notification.show("New supply chain loaded.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                results.removeAll();
                refreshNetwork();
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage(), 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        HorizontalLayout row = new HorizontalLayout(industry, region, size, build);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        row.getStyle().set("flex-wrap", "wrap");
        Paragraph note = new Paragraph("Want a different kind of business? Pick an industry, a region and a size. Real cities and distances are used; company names, capacities and stock levels are invented for the demo. "
                + "This replaces the supply chain shown above. You can also load your own on \"Add your data\".");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return new Div(row, note);
    }

    private Component buildQuestion() {
        site.setWidth("460px");
        outage.setLabel("Stops for");
        outage.setItems(3, 7, 14, 21);
        outage.setItemLabelGenerator(d -> d + " days");
        outage.setValue(7);
        outage.addValueChangeListener(e -> { if (e.isFromClient()) rankSites(site.getValue()); });
        horizon.setLabel("Look ahead");
        horizon.setItems(14, 28, 60, 90);
        horizon.setItemLabelGenerator(d -> d + " days");
        horizon.setValue(28);
        Button run = new Button("Run the simulation", VaadinIcon.PLAY.create(), e -> runSimulation());
        run.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        run.addClassName("sg-run-simulation");
        HorizontalLayout bar = new HorizontalLayout(site, outage, horizon, run);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.getStyle().set("flex-wrap", "wrap");
        return bar;
    }

    private void refreshNetwork() {
        network.removeAll();
        site.clear();
        if (graphService == null) return;
        NetworkGraphView view = graphService.build(workspace());
        if (view.isEmpty()) {
            network.add(new Paragraph("There is no supply chain yet. Pick a kind below, or load your own data."));
            site.setItems(List.of());
            return;
        }
        long suppliers = view.nodes().stream().filter(n -> n.type() == NodeType.SUPPLIER).count();
        long plants = view.nodes().stream().filter(n -> n.type() == NodeType.MANUFACTURING_SITE).count();
        long hubs = view.nodes().stream().filter(n -> n.type() == NodeType.LOGISTICS_HUB || n.type() == NodeType.WAREHOUSE).count();
        network.add(new Paragraph("You have " + view.nodes().size() + " sites: " + suppliers + " suppliers, " + hubs + " hubs and warehouses, and " + plants
                + " factories, joined by " + view.lanes().size() + " transport routes. Factories use up stock every day, and everything else exists to keep them supplied."));
        Map<String, NetworkGraphView.Node> byId = new HashMap<>();
        view.nodes().forEach(n -> byId.put(n.id(), n));
        choices = new ArrayList<>();
        for (NetworkGraphView.Node n : view.nodes()) {
            choices.add(new Choice(n.name() + " (" + kind(n.type()) + ") stops", Set.of(n.id())));
            if (n.type() == NodeType.MANUFACTURING_SITE) {
                Set<String> feeders = new TreeSet<>();
                view.lanes().stream().filter(l -> l.toId().equals(n.id())).forEach(l -> feeders.add(l.fromId()));
                if (!feeders.isEmpty()) choices.add(new Choice("supply into " + n.name() + " is cut off", feeders));
            }
        }
        rankSites(null);
    }

    /** Orders the scenarios by how much damage they do, so the biggest risks come first. */
    private void rankSites(Choice keep) {
        if (choices.isEmpty() || simulation == null) return;
        int days = outage.getValue() == null ? 7 : outage.getValue();
        List<BigDecimal> losses = simulation.impactOfEach(workspace(),
                choices.stream().map(c -> c.siteIds().stream().map(NodeId::of).collect(java.util.stream.Collectors.toSet())).toList(), days, Math.max(28, days + 7));
        impact = new HashMap<>();
        for (int i = 0; i < choices.size(); i++) impact.put(choices.get(i), losses.get(i));
        List<Choice> sorted = new ArrayList<>(choices);
        sorted.sort(Comparator.comparing((Choice c) -> impact.getOrDefault(c, BigDecimal.ZERO)).reversed().thenComparing(Choice::phrase));
        site.setItemLabelGenerator(c -> {
            BigDecimal q = impact.getOrDefault(c, BigDecimal.ZERO);
            return Character.toUpperCase(c.phrase().charAt(0)) + c.phrase().substring(1) + (q.signum() > 0 ? " · " + units(q) + " units at risk" : " · no supply lost");
        });
        site.setItems(sorted);
        // Start on a supply cut-off if one does damage: that is the case where recovery options exist and can be compared.
        Choice fallback = sorted.stream().filter(c -> c.phrase().startsWith("supply into") && impact.getOrDefault(c, BigDecimal.ZERO).signum() > 0).findFirst().orElse(sorted.getFirst());
        site.setValue(keep != null && sorted.contains(keep) ? keep : fallback);
    }

    private void runSimulation() {
        results.removeAll();
        Choice chosen = site.getValue();
        if (chosen == null || simulation == null) {
            Notification.show("Choose which site stops first.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        int days = outage.getValue() == null ? 7 : outage.getValue();
        int ahead = Math.max(horizon.getValue() == null ? 28 : horizon.getValue(), days + 7);
        try {
            List<WhatIfOutcome> futures = simulation.whatIf(workspace(), chosen.siteIds().stream().map(NodeId::of).collect(java.util.stream.Collectors.toSet()), days, ahead);
            render(chosen, days, ahead, futures);
        } catch (RuntimeException ex) {
            Notification.show("The simulation could not run: " + ex.getMessage(), 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void render(Choice chosen, int days, int ahead, List<WhatIfOutcome> futures) {
        results.add(new H3("3. What happens"));
        WhatIfOutcome base = futures.getFirst();
        results.add(headline(chosen, days, futures));
        if (base.result().totalShortageQuantity().signum() == 0) {
            results.add(new Paragraph("Nothing to recover from in this scenario, so there are no recovery options to compare."));
            return;
        }

        Div cards = new Div();
        cards.getStyle().set("display", "grid").set("grid-template-columns", "repeat(auto-fit, minmax(240px, 1fr))").set("gap", "12px").set("margin", "12px 0");
        for (int i = 0; i < futures.size(); i++) cards.add(card(futures.get(i), COLOURS[i % COLOURS.length], i == 0 ? null : base));
        results.add(cards);
        if (futures.size() == 1) {
            results.add(new Paragraph("No recovery option could be worked out for this site: nothing else in the network can supply what it provides over a working route, or no stock is held to move."));
        }

        Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        results.add(new H3("Stock over time"),
                new Paragraph("Total units of stock held across all sites each day. The stock is what keeps the factories running. A line that sinks to zero means they run out."),
                new Html(chart(futures, start, ahead)));

        Div how = new Div(new H3("How this was worked out"));
        how.add(new Paragraph("Each factory uses up its stated production rate every day, and normally its suppliers replace exactly that, so stock stays level. When the chosen site stops, "
                + "any factory it fed that has no other working route runs on its own stock until the outage ends. Recovery options move stock that really exists or switch on another supplier after its lead time. "
                + "\"Customers served\" is units used divided by units needed."));
        for (String a : base.result().assumptions()) {
            Paragraph p = new Paragraph("• " + a);
            p.getStyle().set("margin", "2px 0").set("color", "var(--lumo-secondary-text-color)");
            how.add(p);
        }
        for (String w : base.result().warnings()) how.add(new Paragraph("Note: " + w));
        results.add(how);
    }

    private Component headline(Choice chosen, int days, List<WhatIfOutcome> futures) {
        SimulationResult base = futures.getFirst().result();
        String text;
        if (base.totalShortageQuantity().signum() == 0) {
            text = "If " + chosen.phrase() + " for " + days + " days, supply is not interrupted: every factory keeps getting what it needs, either over other routes or from stock already on hand. Try a scenario nearer the top of the list.";
        } else {
            text = String.format(Locale.ROOT, "If %s for %d days and you do nothing, you cannot supply %s units and customers are served %.0f%% of the time; the longest stock-out lasts %d days.",
                    chosen.phrase(), days, units(base.totalShortageQuantity()), base.serviceLevelPercentage(), base.maxDelaySeconds() / 86400L);
            WhatIfOutcome best = futures.stream().skip(1).max(Comparator.comparingDouble(f -> f.result().serviceLevelPercentage())).orElse(null);
            if (best != null && best.result().serviceLevelPercentage() > base.serviceLevelPercentage() + 0.05) {
                text += String.format(Locale.ROOT, " The best option, \"%s\", lifts that to %.0f%% and cuts the unsupplied units to %s.",
                        best.name(), best.result().serviceLevelPercentage(), units(best.result().totalShortageQuantity()));
            } else if (futures.size() > 1) {
                text += " None of the recovery options improves on that in this model.";
            }
        }
        Div box = new Div(new Span(text));
        box.getStyle().set("background", "var(--lumo-primary-color-10pct)").set("border-radius", "8px").set("padding", "14px 16px").set("font-weight", "600");
        box.addClassName("sg-sim-headline");
        return box;
    }

    private Component card(WhatIfOutcome o, String colour, WhatIfOutcome base) {
        SimulationResult r = o.result();
        Div c = new Div();
        c.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-left", "6px solid " + colour).set("border-radius", "8px").set("padding", "12px 14px");
        Span name = new Span(o.name());
        name.getStyle().set("font-weight", "700").set("display", "block").set("margin-bottom", "4px");
        String firstSentence = o.description().contains(". ") ? o.description().substring(0, o.description().indexOf(". ") + 1) : o.description();
        Span desc = new Span(firstSentence);
        desc.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)").set("display", "block").set("margin-bottom", "8px");
        c.add(name, desc,
                line("Customers served", String.format(Locale.ROOT, "%.0f%%", r.serviceLevelPercentage()), base == null ? null : r.serviceLevelPercentage() - base.result().serviceLevelPercentage(), true),
                line("Units not supplied", units(r.totalShortageQuantity()), base == null ? null : r.totalShortageQuantity().subtract(base.result().totalShortageQuantity()).doubleValue(), false),
                line("Longest stock-out", (r.maxDelaySeconds() / 86400L) + " days", null, false));
        return c;
    }

    private static Div line(String label, String value, Double change, boolean higherIsBetter) {
        Div row = new Div();
        row.getStyle().set("display", "flex").set("justify-content", "space-between").set("padding", "2px 0");
        Span l = new Span(label);
        l.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span v = new Span(value);
        v.getStyle().set("font-weight", "600");
        if (change != null && Math.abs(change) > 0.05) {
            boolean good = higherIsBetter == change > 0;
            Span delta = new Span(String.format(Locale.ROOT, " (%+,.0f)", change));
            delta.getStyle().set("color", good ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
            v.add(delta);
        }
        row.add(l, v);
        return row;
    }

    private static String units(BigDecimal q) {
        return String.format(Locale.ROOT, "%,d", q.setScale(0, java.math.RoundingMode.HALF_UP).longValue());
    }

    private static String kind(NodeType t) {
        return switch (t) {
            case SUPPLIER -> "supplier";
            case MANUFACTURING_SITE -> "factory";
            case LOGISTICS_HUB -> "hub";
            case WAREHOUSE -> "warehouse";
        };
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT).replace("north america", "North America");
    }

    /** Sums the latest known stock of every site for each simulated day and draws one line per future. */
    private static String chart(List<WhatIfOutcome> futures, Instant start, int days) {
        List<double[]> series = new ArrayList<>();
        double max = 1;
        for (WhatIfOutcome s : futures) {
            double[] byDay = new double[days + 1];
            Map<String, Double> latest = new HashMap<>();
            List<TrajectoryPoint> pts = new ArrayList<>(s.result().inventoryTrajectories());
            pts.sort(Comparator.comparing(TrajectoryPoint::timestamp));
            int idx = 0;
            for (int day = 0; day <= days; day++) {
                Instant end = start.plus(Duration.ofDays(day + 1L));
                while (idx < pts.size() && pts.get(idx).timestamp().isBefore(end)) {
                    latest.put(pts.get(idx).entityKey(), pts.get(idx).value().doubleValue());
                    idx++;
                }
                byDay[day] = latest.values().stream().mapToDouble(Double::doubleValue).sum();
                max = Math.max(max, byDay[day]);
            }
            series.add(byDay);
        }
        int w = 800, h = 280, left = 70, bottom = 30, top = 12, right = 12;
        double pw = w - left - right, ph = h - top - bottom;
        StringBuilder svg = new StringBuilder("<svg viewBox=\"0 0 " + w + " " + h + "\" style=\"width:100%;max-width:900px;font-family:sans-serif;font-size:11px\" role=\"img\" aria-label=\"Stock on hand per day\">");
        for (int g = 0; g <= 4; g++) {
            double y = top + ph - ph * g / 4;
            svg.append(String.format(Locale.ROOT, "<line x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\" stroke=\"#d1d5db\" stroke-width=\"1\"/>", left, w - right, y, y));
            svg.append(String.format(Locale.ROOT, "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" fill=\"#6b7280\">%,.0f</text>", left - 6, y + 4, max * g / 4));
        }
        for (int t = 0; t <= 4; t++) {
            double x = left + pw * t / 4;
            svg.append(String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" fill=\"#6b7280\">day %d</text>", x, h - 8, Math.round(days * t / 4.0)));
        }
        for (int i = 0; i < series.size(); i++) {
            StringBuilder pts = new StringBuilder();
            double[] v = series.get(i);
            for (int day = 0; day <= days; day++) {
                pts.append(String.format(Locale.ROOT, "%.1f,%.1f ", left + pw * day / days, top + ph - ph * v[day] / max));
            }
            svg.append("<polyline fill=\"none\" stroke=\"").append(COLOURS[i % COLOURS.length]).append("\" stroke-width=\"").append(i == 0 ? 3 : 2).append("\" points=\"").append(pts).append("\"/>");
        }
        svg.append("</svg>");
        StringBuilder legend = new StringBuilder("<div style=\"display:flex;flex-wrap:wrap;gap:16px;margin-top:4px\">");
        for (int i = 0; i < futures.size(); i++) {
            legend.append("<span><span style=\"display:inline-block;width:14px;height:4px;background:").append(COLOURS[i % COLOURS.length]).append(";margin-right:6px;vertical-align:middle\"></span>")
                    .append(escape(futures.get(i).name())).append("</span>");
        }
        legend.append("</div>");
        return "<div>" + svg + legend + "</div>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
