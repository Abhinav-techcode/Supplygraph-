package com.supplygraph.web.layout;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.shared.Registration;
import jakarta.servlet.http.Cookie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Interactive first-run tour written in Java only. A card tells the user what to do; the element to use is highlighted with a pulsing halo
 * (theme CSS). Steps that need a click advance when the user clicks the highlighted button. Completion is remembered in a cookie.
 */
public final class GuidedTour {
    private static final String COOKIE = "sgTourDone";

    /** @param clickToAdvance when true the tour moves on when the user clicks the highlighted component (no Next button). */
    private record Step(String route, String targetClass, String title, String text, boolean clickToAdvance) {}

    private static final List<Step> STEPS = List.of(
            new Step("", null, "Welcome to SupplyGraph", "This short tour takes you from an empty system to a recovered disruption. It tells you exactly what to click. Leave any time with \"Skip tour\".", false),
            new Step("", "sg-data-panel", "Step 1 · Add your data", "Everything is built from your supply chain. Press \"Load sample supply chain\" here to explore right away, or \"Import my own data\" to describe your facilities, lanes and stock. Do this first.", false),
            new Step("", "sg-graph-canvas", "Your supply chain as a living map", "Each card is a facility from the digital twin. Red = disrupted, orange = downstream impact, green = recovery route. Click a facility to trace its supply path; scroll to pan; + / − to zoom.", false),
            new Step("", "sg-run-agents", "Step 2 · Wake the AI agents", "Eight specialist agents read the twin and propose actions with evidence. Press \"Run all 8 agents\".", true),
            new Step("", "sg-run-simulation", "Step 3 · Test it with simulation", "Replay the disruption over 28 days and run 200 random Monte Carlo trials to see the risk. Press the highlighted button.", true),
            new Step("incident-workspace", "sg-stepper", "Step 4 · Recover one incident, end to end", "Detect, Impact, Agents, Options, ConcurGuard, Approve, Execute. The map above always shows where you are.", false),
            new Step("incident-workspace", "sg-next-step", "Detect → Impact", "An active incident is already selected. Click \"Next\" to see what it reaches.", true),
            new Step("incident-workspace", "sg-graph-canvas", "Impact", "Orange lines on the map show where freight is blocked and orange facilities are hit downstream. Read the numbers below, then continue.", false),
            new Step("incident-workspace", "sg-agent-grid", "Agents", "Click an agent row to read the evidence behind its proposal. Then click \"Next\".", false),
            new Step("incident-workspace", "sg-next-step", "Agents → Options", "Click \"Next\" to compare recovery options.", true),
            new Step("incident-workspace", "sg-choose-plan", "Options", "Each option is derived from real stock, suppliers and lanes. Click \"Choose this option\": the green route appears on the map.", true),
            new Step("incident-workspace", "sg-next-step", "Options → ConcurGuard", "Click \"Next\".", true),
            new Step("incident-workspace", "sg-validate", "ConcurGuard", "ConcurGuard checks the plan does not overdraw stock or double-book a lane. Click to run it.", true),
            new Step("incident-workspace", "sg-next-step", "ConcurGuard → Approve", "Click \"Next\".", true),
            new Step("incident-workspace", "sg-approve", "Approve", "Enter your name and a reason, then press \"Approve plan\". The decision is audited and the agents learn from it.", false),
            new Step("incident-workspace", "sg-dispatch", "Execute", "Start the governed workflow, then confirm the physical move: the twin updates and the map shows the new stock. That is the full loop.", false));

    private static final class State {
        int index;
        Div card;
        Element target;
        Registration clickReg;
        boolean active;
    }

    private GuidedTour() {}

    public static boolean isDone() {
        VaadinRequest req = VaadinRequest.getCurrent();
        if (req == null || req.getCookies() == null) return false;
        for (Cookie c : req.getCookies()) if (COOKIE.equals(c.getName())) return true;
        return false;
    }

    public static void start(UI ui, int from) {
        State s = state(ui);
        clear(s);
        s.active = true;
        s.index = Math.max(0, Math.min(from, STEPS.size() - 1));
        show(ui, s);
    }

    private static State state(UI ui) {
        State s = ComponentUtil.getData(ui, State.class);
        if (s == null) { s = new State(); ComponentUtil.setData(ui, State.class, s); }
        return s;
    }

    private static void show(UI ui, State s) {
        clearTarget(s);
        Step step = STEPS.get(s.index);
        if (!normalized(ui).equals(step.route())) ui.navigate(step.route());
        Optional<Element> target = step.targetClass() == null ? Optional.empty() : find(viewRoot(ui), step.targetClass());
        target.ifPresent(el -> {
            s.target = el;
            el.getClassList().add("sg-tour-target");
            el.scrollIntoView();
            if (step.clickToAdvance()) {
                Component c = el.getComponent().orElse(null);
                if (c instanceof ClickNotifier<?> cn) s.clickReg = cn.addClickListener(e -> next(ui));
            }
        });
        renderCard(ui, s, step, target.isPresent());
    }

    /** The element of the page currently shown (the routed view), where all tour targets live. */
    private static Element viewRoot(UI ui) {
        var chain = ui.getInternals().getActiveRouterTargetsChain();
        return chain.isEmpty() ? ui.getElement() : chain.get(0).getElement();
    }

    private static String normalized(UI ui) {
        return ui.getInternals().getActiveViewLocation().getPath();
    }

    private static void renderCard(UI ui, State s, Step step, boolean found) {
        if (s.card != null) ui.remove(s.card);
        Div card = new Div();
        card.addClassName("sg-tour-card");
        Span count = new Span("Step " + (s.index + 1) + " of " + STEPS.size());
        count.addClassName("sg-tour-step");
        Span arrow = new Span(found ? "➡" : "");
        arrow.addClassName("sg-tour-arrow");
        H4 title = new H4();
        title.add(arrow, new Span(step.title()));
        Paragraph text = new Paragraph(step.text());
        text.getStyle().set("margin", "0 0 10px");
        Button skip = new Button("Skip tour", e -> finish(ui, true));
        skip.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        Button back = new Button("Back", e -> go(ui, -1));
        back.addThemeVariants(ButtonVariant.LUMO_SMALL);
        back.setEnabled(s.index > 0);
        boolean last = s.index == STEPS.size() - 1;
        HorizontalLayout bar = new HorizontalLayout(skip, back);
        bar.setWidthFull();
        bar.addClassName("sg-nowrap");
        if (step.clickToAdvance() && found) {
            Span hint = new Span("Click the highlighted button");
            hint.getStyle().set("color", "#0369a1").set("font-size", "var(--lumo-font-size-xs)").set("margin-left", "auto");
            bar.add(hint);
            Button skipStep = new Button("Skip step", e -> go(ui, 1));
            skipStep.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            bar.add(skipStep);
        } else {
            Button next = new Button(last ? "Finish" : "Next", e -> { if (last) finish(ui, true); else go(ui, 1); });
            next.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            next.getStyle().set("margin-left", "auto");
            next.addClassName("sg-tour-next");
            bar.add(next);
        }
        card.add(count, title, text, bar);
        ui.add(card);
        s.card = card;
    }

    private static void next(UI ui) { go(ui, 1); }

    private static void go(UI ui, int delta) {
        State s = state(ui);
        if (!s.active) return;
        int i = s.index + delta;
        if (i >= STEPS.size()) { finish(ui, true); return; }
        s.index = Math.max(0, i);
        // let the clicked component finish its own listeners (e.g. re-render the step) before the next target is looked up
        ui.access(() -> show(ui, s));
    }

    private static void finish(UI ui, boolean remember) {
        State s = state(ui);
        clear(s);
        if (s.card != null) { ui.remove(s.card); s.card = null; }
        s.active = false;
        if (remember) {
            VaadinResponse resp = VaadinResponse.getCurrent();
            if (resp != null) {
                Cookie c = new Cookie(COOKIE, "1");
                c.setPath("/");
                c.setMaxAge(60 * 60 * 24 * 365);
                resp.addCookie(c);
            }
        }
    }

    private static void clearTarget(State s) {
        if (s.target != null) { s.target.getClassList().remove("sg-tour-target"); s.target = null; }
        if (s.clickReg != null) { s.clickReg.remove(); s.clickReg = null; }
    }

    private static void clear(State s) { clearTarget(s); }

    private static Optional<Element> find(Element root, String cls) {
        List<Element> stack = new ArrayList<>(List.of(root));
        int visited = 0;
        while (!stack.isEmpty()) {
            Element e = stack.remove(0);
            visited++;
            if (e.isTextNode()) continue;
            if (e.getClassList().contains(cls)) return Optional.of(e);
            e.getChildren().forEach(stack::add);
        }
        org.slf4j.LoggerFactory.getLogger(GuidedTour.class).warn("Tour target '{}' not found after visiting {} elements", cls, visited);
        return Optional.empty();
    }
}
