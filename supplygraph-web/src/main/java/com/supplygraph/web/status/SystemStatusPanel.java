package com.supplygraph.web.status;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/** Compact, honest status line: database up or down, which one, whether data survives a restart. */
public class SystemStatusPanel extends Div {
    public SystemStatusPanel(SystemStatusService.Status s, boolean detailed) {
        addClassName("sg-status");
        Span dot = new Span();
        dot.addClassNames("sg-status-dot", s.connected() ? (s.persistent() ? "sg-ok" : "sg-warn") : "sg-bad");
        String text;
        if (!s.connected()) text = "Database unavailable. " + s.problem();
        else if (s.persistent()) text = "Database connected: " + s.product();
        else text = "Database connected: " + s.product() + " (in memory: demo data is lost when the application stops)";
        add(dot, new Span(text));
        if (detailed && s.connected()) {
            Div d = new Div(new Span("Location " + s.where() + " · schema version " + s.migration() + " · " + s.facilities() + " facilities, " + s.lanes() + " lanes, "
                    + s.disruptions() + " incidents, " + s.users() + " user accounts"));
            d.addClassName("sg-status-detail");
            add(d);
        }
    }
}
