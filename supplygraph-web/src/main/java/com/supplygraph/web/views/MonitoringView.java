package com.supplygraph.web.views;

import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.monitor.MonitoringService;
import com.supplygraph.web.monitor.MonitoringService.Check;
import com.supplygraph.web.monitor.MonitoringService.Level;
import com.supplygraph.web.monitor.MonitoringService.Sample;
import com.supplygraph.web.monitor.MonitoringService.Snapshot;
import com.supplygraph.web.monitor.RecentErrors;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Live system monitor: is the website healthy right now, how is it performing, what went wrong recently. Refreshes itself every 5 seconds. */
@Route(value = "monitoring", layout = MainLayout.class)
@PageTitle("System monitor | ORBIX")
public class MonitoringView extends VerticalLayout {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final MonitoringService monitoring;
    private final Div body = new Div();
    private final Span updated = new Span();
    private final Checkbox live = new Checkbox("Live (refresh every 5 s)", true);
    private Registration poll;

    public MonitoringView(MonitoringService monitoring) {
        this.monitoring = monitoring;
        setPadding(true);
        setMaxWidth("1440px");
        getStyle().set("margin", "0 auto");
        addClassName("sg-monitor");

        H2 title = new H2("System monitor");
        title.getStyle().set("margin", "0");
        Paragraph sub = new Paragraph("Is the website healthy right now? Health checks, response times, memory, recent errors and the state of the business data, all measured inside the application.");
        sub.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");
        Button refresh = new Button("Check now", VaadinIcon.REFRESH.create(), e -> render());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        refresh.addClassName("sg-monitor-refresh");
        Button clear = new Button("Clear error list", e -> { RecentErrors.clear(); render(); });
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        updated.addClassName("sg-monitor-updated");
        HorizontalLayout bar = new HorizontalLayout(refresh, live, clear, updated);
        bar.setAlignItems(Alignment.CENTER);
        add(title, sub, bar, body);
        body.setWidthFull();
        render();
    }

    @Override
    protected void onAttach(AttachEvent e) {
        super.onAttach(e);
        UI ui = e.getUI();
        ui.setPollInterval(5000);
        poll = ui.addPollListener(ev -> { if (live.getValue()) render(); });
    }

    @Override
    protected void onDetach(DetachEvent e) {
        if (poll != null) poll.remove();
        e.getUI().setPollInterval(-1);
        super.onDetach(e);
    }

    private void render() {
        Snapshot s = monitoring.snapshot();
        body.removeAll();
        updated.setText("Updated " + TIME.format(s.at()));

        Div banner = new Div();
        banner.addClassNames("sg-mon-banner", "sg-mon-" + s.overall().name().toLowerCase());
        banner.add(new Span(switch (s.overall()) {
            case OK -> "All systems operational";
            case WARN -> "Working, with warnings";
            case DOWN -> "Problem detected: action needed";
        }));
        banner.add(new Span("Up for " + human(s.uptime()) + " · " + s.activeSessions() + " active session(s) · Java " + s.javaVersion() + " · " + s.processors() + " CPU(s)"));

        Div checks = new Div();
        checks.addClassName("sg-mon-grid");
        for (Check c : s.checks()) checks.add(checkCard(c));

        var rq = s.requests();
        Div kpis = new Div();
        kpis.addClassName("sg-mon-grid");
        kpis.add(stat("Requests (5 min)", String.valueOf(rq.last5Min()), rq.byArea().isEmpty() ? "none yet" : rq.byArea().toString().replaceAll("[{}]", "")),
                stat("Average response", String.format("%.0f ms", rq.avgMs()), "95% under " + String.format("%.0f ms", rq.p95Ms())),
                stat("Server errors", String.valueOf(rq.serverErrors()), rq.clientErrors() + " client errors (4xx)"),
                stat("Agent runs", String.valueOf(s.business().agentRuns()), "stored investigations"));

        Div charts = new Div();
        charts.addClassName("sg-mon-grid");
        charts.add(chart("Memory in use (MB)", s.samples(), Sample::heapUsedMb, "#6366f1", false),
                chart("Response time (ms, 5 s average)", s.samples(), Sample::avgLatencyMs, "#0ea5e9", false),
                chart("Database round trip (ms)", s.samples(), x -> Math.max(0, x.dbMs()), "#10b981", false),
                chart("CPU of this process (%)", s.samples(), Sample::cpuPercent, "#f59e0b", false));

        var b = s.business();
        Div biz = card();
        biz.add(new H4("Business data"), new Paragraph(b.facilities() + " facilities · " + b.lanes() + " lanes · " + b.activeIncidents() + " active incident(s) · "
                + b.blockedWorkflows() + " blocked workflow(s) · " + b.outboxPending() + " outbox waiting · " + b.outboxDead() + " dead-lettered · " + b.users() + " user accounts"));

        Grid<RecentErrors.Entry> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.addColumn(x -> TIME.format(x.at())).setHeader("Time").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(RecentErrors.Entry::source).setHeader("Where").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(RecentErrors.Entry::code).setHeader("Kind").setAutoWidth(true);
        grid.addColumn(RecentErrors.Entry::message).setHeader("Message").setFlexGrow(3);
        grid.addColumn(x -> x.reference() == null ? "" : x.reference()).setHeader("Reference").setAutoWidth(true);
        grid.setItems(s.errors());
        Div errors = card();
        errors.add(new H4("Recent errors"));
        errors.add(s.errors().isEmpty() ? new Paragraph("No errors recorded since the application started.") : grid);

        body.add(banner, sectionTitle("Health checks"), checks, sectionTitle("Traffic"), kpis, sectionTitle("Last 10 minutes"), charts, biz, errors);
    }

    private static H4 sectionTitle(String t) {
        H4 h = new H4(t);
        h.getStyle().set("margin", "16px 0 8px");
        return h;
    }

    private static Div card() {
        Div c = new Div();
        c.addClassName("sg-card");
        c.getStyle().set("margin-top", "12px");
        return c;
    }

    private static Div checkCard(Check c) {
        Div d = new Div();
        d.addClassNames("sg-card", "sg-mon-check", "sg-mon-" + c.level().name().toLowerCase());
        Span dot = new Span();
        dot.addClassNames("sg-status-dot", c.level() == Level.OK ? "sg-ok" : c.level() == Level.WARN ? "sg-warn" : "sg-bad");
        Span name = new Span(c.name());
        name.getStyle().set("font-weight", "700");
        Span state = new Span(c.level() == Level.OK ? "OK" : c.level() == Level.WARN ? "Warning" : "Down");
        state.getElement().getThemeList().add(c.level() == Level.OK ? "badge success small" : c.level() == Level.WARN ? "badge warning small" : "badge error small");
        state.getStyle().set("margin-left", "auto");
        Div top = new Div(dot, name, state);
        top.getStyle().set("display", "flex").set("align-items", "center").set("gap", "8px");
        Span detail = new Span(c.detail());
        detail.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)").set("display", "block").set("margin-top", "6px");
        d.add(top, detail);
        return d;
    }

    private static Div stat(String label, String value, String note) {
        Div d = new Div();
        d.addClassName("sg-card");
        Span v = new Span(value);
        v.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "700").set("display", "block");
        Span l = new Span(label);
        l.getStyle().set("font-size", "var(--lumo-font-size-s)").set("display", "block");
        Span n = new Span(note);
        n.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-tertiary-text-color)");
        d.add(v, l, n);
        return d;
    }

    /** A small server-drawn line chart (SVG) of the recent samples with the latest value and the maximum. */
    private static Div chart(String title, List<Sample> samples, ToDoubleFunction<Sample> f, String color, boolean unused) {
        Div d = new Div();
        d.addClassName("sg-card");
        double max = samples.stream().mapToDouble(f).max().orElse(0), last = samples.isEmpty() ? 0 : f.applyAsDouble(samples.get(samples.size() - 1));
        Span t = new Span(title);
        t.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        Span v = new Span(String.format("%.0f", last) + "   (max " + String.format("%.0f", max) + ")");
        v.getStyle().set("font-weight", "700").set("display", "block").set("margin", "2px 0 6px");
        StringBuilder pts = new StringBuilder();
        int w = 300, h = 60;
        for (int i = 0; i < samples.size(); i++) {
            double x = samples.size() == 1 ? 0 : (double) i / (samples.size() - 1) * w;
            double y = h - (max <= 0 ? 0 : f.applyAsDouble(samples.get(i)) / max * (h - 4)) - 2;
            pts.append(String.format("%.1f,%.1f ", x, y));
        }
        String svg = "<svg viewBox='0 0 " + w + " " + h + "' preserveAspectRatio='none' style='width:100%;height:60px;display:block'>"
                + "<rect width='" + w + "' height='" + h + "' fill='none' stroke='#e2e8f0'/>"
                + (samples.size() > 1 ? "<polyline fill='none' stroke='" + color + "' stroke-width='2' points='" + pts + "'/>" : "") + "</svg>";
        d.add(t, v, new Html("<div>" + svg + "</div>"));
        return d;
    }

    private static String human(java.time.Duration d) {
        long h = d.toHours(), m = d.toMinutesPart(), s = d.toSecondsPart();
        return h > 0 ? h + " h " + m + " min" : m > 0 ? m + " min " + s + " s" : s + " s";
    }
}
