package com.supplygraph.web.views.workspace;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class WorkspaceViewBase extends VerticalLayout {

    protected WorkspaceViewBase(String personaTitle, String subtitle, String roleBadge) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2(personaTitle);
        header.getStyle().set("margin-bottom", "0");

        Paragraph sub = new Paragraph(subtitle);
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "var(--lumo-space-xs)");

        HorizontalLayout topBar = new HorizontalLayout(header);
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.BASELINE);

        Span badge = new Span(roleBadge);
        badge.getElement().getThemeList().add("badge small primary");
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

    protected VerticalLayout createKpiCard(String title, String value, String description, String theme) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("sg-kpi-card");
        card.setPadding(true);
        card.setSpacing(false);

        Span titleSpan = new Span(title);
        titleSpan.addClassName("sg-kpi-title");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("sg-kpi-value");

        Span descSpan = new Span(description);
        descSpan.addClassName("sg-kpi-subtitle");

        if (theme != null && !theme.isBlank()) {
            valueSpan.getElement().getThemeList().add("badge " + theme);
        }

        card.add(titleSpan, valueSpan, descSpan);
        return card;
    }
}

