package com.supplygraph.web.views.collaboration;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserProfile;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class CollaborationViewBase extends VerticalLayout {

    protected CollaborationViewBase(String title, String subtitle) {
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

        Span badge = new Span("Supplier collaboration");
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
        card.addClassNames("p-m", "border", "rounded-m");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");
        card.setPadding(true);
        card.setSpacing(false);

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("margin", "var(--lumo-space-xs) 0");

        Span descSpan = new Span(description);
        descSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        if (theme != null && !theme.isBlank()) {
            valueSpan.getElement().getThemeList().add("badge " + theme);
        }

        card.add(titleSpan, valueSpan, descSpan);
        return card;
    }
}

