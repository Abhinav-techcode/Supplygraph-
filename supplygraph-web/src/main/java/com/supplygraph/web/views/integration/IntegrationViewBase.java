package com.supplygraph.web.views.integration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Common frame: heading, description, active principal line. */
abstract class IntegrationViewBase extends VerticalLayout {
    protected IntegrationViewBase(String title, String description) {
        setSizeFull();
        setPadding(true);
        add(new H2(title), new Paragraph(description));
        Paragraph who = new Paragraph("Acting as: " + UiSupport.currentUser() + " · workspace " + UiSupport.workspace().value());
        who.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        add(who);
    }

    protected void body(Component... components) { add(components); }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent e) {
        super.onAttach(e);
        com.supplygraph.web.views.integration.GridUsability.actionsFirst(this);
    }

}
