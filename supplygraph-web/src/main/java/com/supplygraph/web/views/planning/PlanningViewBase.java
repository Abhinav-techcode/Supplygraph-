package com.supplygraph.web.views.planning;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserProfile;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class PlanningViewBase extends VerticalLayout {

    protected PlanningViewBase(String title, String subtitle) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2(title);
        header.getStyle().set("margin-bottom", "0");

        Paragraph sub = new Paragraph(subtitle);
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "var(--lumo-space-xs)");

        HorizontalLayout topBar = new HorizontalLayout(header);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.BASELINE);

        Span badge = new Span("Supply chain planning");
        badge.getElement().getThemeList().add("badge small contrast");
        topBar.add(badge);

        add(topBar, sub);
    }

    protected WorkspaceId currentWorkspaceId() {
        UserProfile user = SecurityContext.getCurrentUser();
        if (user != null && !user.assignedWorkspaces().isEmpty()) {
            return user.assignedWorkspaces().iterator().next();
        }
        return new WorkspaceId("ws-default");
    }
}

