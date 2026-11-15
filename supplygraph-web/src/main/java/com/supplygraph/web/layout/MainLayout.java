package com.supplygraph.web.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Standard enterprise responsive application layout for SupplyGraph.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        H1 title = new H1("SupplyGraph");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span badge = new Span("Platform Foundation :: Phase 1");
        badge.getElement().getThemeList().add("badge success small");
        badge.getStyle().set("margin-left", "var(--lumo-space-s)");

        HorizontalLayout header = new HorizontalLayout(toggle, title, badge);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Overview", "", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("System Health", "health", VaadinIcon.HEART.create()));

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("p-s");

        addToDrawer(scroller);
    }
}
