package com.supplygraph.web.error;

import com.supplygraph.web.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.servlet.http.HttpServletResponse;

/** Friendly 404 inside the normal application frame. */
@ParentLayout(MainLayout.class)
@PageTitle("Page not found | ORBIX")
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {
    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        removeAll();
        setPadding(true);
        setMaxWidth("720px");
        getStyle().set("margin", "48px auto");
        Button home = new Button("Back to home", e -> UI.getCurrent().navigate(""));
        home.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(new H2("There is no page at /" + event.getLocation().getPath()), new Paragraph("Use the menu on the left, or go back to the home page."), home);
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
