package com.supplygraph.web.error;

import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.PageTitle;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/** Shown when a page fails while it is being built (for example because a service it needs is unavailable or the data is inconsistent). */
@ParentLayout(MainLayout.class)
@PageTitle("Something went wrong | ORBIX")
public class AppErrorView extends VerticalLayout implements HasErrorParameter<Exception> {
    private static final Logger log = LoggerFactory.getLogger(AppErrorView.class);

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        var c = com.supplygraph.application.error.ErrorClassifier.classify(parameter.getException());
        String id = c.reference() != null ? c.reference() : UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Page failed to load [{}] at /{} ({})", id, event.getLocation().getPath(), c.code(), c.cause());
        removeAll();
        setPadding(true);
        setMaxWidth("720px");
        getStyle().set("margin", "48px auto");
        Button home = new Button("Back to home", e -> UI.getCurrent().navigate(""));
        home.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button retry = new Button("Try again", e -> UI.getCurrent().getPage().reload());
        add(new H2("This page could not be shown"),
                new Paragraph(c.code() == com.supplygraph.domain.error.ErrorCode.INTERNAL_ERROR || c.code() == com.supplygraph.domain.error.ErrorCode.STORAGE_FAILURE
                        ? "Something unexpected happened while loading it. Nothing was changed. If it keeps happening, quote reference " + id + " to your administrator."
                        : c.message() + " (reference " + id + ")"),
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(home, retry));
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
