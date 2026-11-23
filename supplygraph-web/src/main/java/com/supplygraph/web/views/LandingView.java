package com.supplygraph.web.views;

import com.supplygraph.web.status.SystemStatusPanel;
import com.supplygraph.web.status.SystemStatusService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

/** Public welcome page: what SupplyGraph is, how to use it, what it will and will not do. */
@Route("welcome")
@PageTitle("SupplyGraph | Supply chain digital twin and governed recovery")
public class LandingView extends Div {
    private record Card(String title, String text) {}
    private record Step(String title, String text) {}

    public LandingView(SystemStatusService status) {
        addClassName("sg-land");

        Div nav = new Div();
        nav.addClassName("sg-land-nav");
        Span brand = new Span("SupplyGraph");
        brand.addClassName("sg-brand");
        Button signIn = new Button("Sign in", e -> UI.getCurrent().navigate(LoginView.class));
        signIn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        signIn.addClassName("sg-land-signin");
        nav.add(brand, signIn);

        Div hero = new Div();
        hero.addClassName("sg-land-hero");
        H1 h1 = new H1("See the disruption coming. Recover with confidence.");
        Paragraph lead = new Paragraph("SupplyGraph turns your supply chain into a living digital twin. It shows what a disruption reaches, lets specialist analysis agents propose recoveries, "
                + "checks them for conflicts, and keeps people in charge with approval, execution tracking and a full audit trail.");
        lead.addClassName("sg-land-lead");
        Button start = new Button("Sign in to get started", e -> UI.getCurrent().navigate(LoginView.class));
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        hero.add(h1, lead, new Div(start));

        add(nav, hero,
                section("What you can do", "Four jobs, one connected system.", List.of(
                        new Card("Model your network", "Describe facilities, lanes, stock and the bill of materials once. SupplyGraph keeps them as a versioned digital twin, so every other screen works from the same facts."),
                        new Card("Understand impact", "When something breaks, see which facilities, materials and products are reached, how long stock will last, and which facilities have no way around the problem."),
                        new Card("Decide with agents and checks", "Eight rule-based specialist agents (plain Java analysis, not a language model) read the twin and propose actions with their evidence. ConcurGuard checks proposals do not overdraw stock or double-book lanes. Simulation shows the risk."),
                        new Card("Execute under control", "A person approves. Execution runs as a governed workflow with separation of duties, and confirmed outcomes update the twin so the next decision starts from reality."))),
                how(),
                trust(),
                footer(status));
    }

    private static Div section(String title, String sub, List<Card> cards) {
        Div s = new Div();
        s.addClassName("sg-land-section");
        s.add(new H2(title), new Paragraph(sub));
        Div grid = new Div();
        grid.addClassName("sg-land-grid");
        for (Card c : cards) {
            Div d = new Div(new H3(c.title()), new Paragraph(c.text()));
            d.addClassName("sg-land-card");
            grid.add(d);
        }
        s.add(grid);
        return s;
    }

    private static Div how() {
        Div s = new Div();
        s.addClassNames("sg-land-section", "sg-land-alt");
        s.getElement().setAttribute("id", "how");
        s.add(new H2("How to use SupplyGraph"), new Paragraph("Six steps from an empty system to a recovered disruption. The built-in tour will point at each of them."));
        List<Step> steps = List.of(
                new Step("Sign in", "Use the account you were given. What you can do depends on your role."),
                new Step("Add your data", "On the home page, load the sample supply chain to explore, or import your own facilities, lanes and stock from CSV."),
                new Step("Look at the network map", "Click a facility to see its stock and lanes and to trace everything upstream and downstream of it."),
                new Step("Wake the agents and test the risk", "Run the eight agents on an incident, then run the simulation and Monte Carlo analysis to see how likely a bad outcome is."),
                new Step("Open Guided recovery", "Follow one incident: impact, agent findings, options, ConcurGuard validation, then a person approves."),
                new Step("Execute and learn", "Start the governed workflow and confirm the move. The twin updates, and each agent's track record counts the decisions people accepted."));
        Div list = new Div();
        list.addClassName("sg-land-steps");
        int i = 1;
        for (Step st : steps) {
            Span n = new Span(String.valueOf(i++));
            n.addClassName("sg-land-stepno");
            Div body = new Div(new H3(st.title()), new Paragraph(st.text()));
            Div row = new Div(n, body);
            row.addClassName("sg-land-step");
            list.add(row);
        }
        s.add(list);
        return s;
    }

    private static Div trust() {
        Div s = new Div();
        s.addClassName("sg-land-section");
        s.add(new H2("Built to be trusted"));
        UnorderedList ul = new UnorderedList(
                new ListItem("People decide. Agents only propose, and nothing executes without an approval."),
                new ListItem("Every figure comes from your data or says \"No data\". Nothing is estimated silently."),
                new ListItem("Simulated and real are always labelled differently."),
                new ListItem("Roles and separation of duties: the person who creates a plan cannot be the one who approves it."),
                new ListItem("Every decision is written to an audit trail."),
                new ListItem("The agents are deterministic Java rules that run inside the application. No language model, external AI service or API key is involved, and no invented scores are shown."));
        ul.addClassName("sg-land-list");
        s.add(ul);
        return s;
    }

    private static Div footer(SystemStatusService status) {
        Div f = new Div();
        f.addClassName("sg-land-footer");
        Button in = new Button("Sign in", e -> UI.getCurrent().navigate(LoginView.class));
        in.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        f.add(new Span("SupplyGraph"), in, new SystemStatusPanel(status.current(), false));
        return f;
    }
}
