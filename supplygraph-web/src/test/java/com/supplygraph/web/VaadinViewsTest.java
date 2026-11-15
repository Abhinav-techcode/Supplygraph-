package com.supplygraph.web;

import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.HealthView;
import com.supplygraph.web.views.HomeView;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Vaadin Flow Views & Shell Structure Tests")
class VaadinViewsTest {

    @Test
    @DisplayName("Should verify HomeView is annotated with root route and MainLayout")
    void shouldVerifyHomeViewRoute() {
        Route route = HomeView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEmpty();
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = HomeView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("SupplyGraph");
    }

    @Test
    @DisplayName("Should verify HealthView is annotated with 'health' route and MainLayout")
    void shouldVerifyHealthViewRoute() {
        Route route = HealthView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("health");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = HealthView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Health");
    }

    @Test
    @DisplayName("Should instantiate HomeView and verify component hierarchy")
    void shouldInstantiateHomeView() {
        HomeView homeView = new HomeView();
        assertThat(homeView.getComponentCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should instantiate HealthView and verify diagnostics card")
    void shouldInstantiateHealthView() {
        HealthView healthView = new HealthView();
        assertThat(healthView.getComponentCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should instantiate MainLayout and verify navigation shell")
    void shouldInstantiateMainLayout() {
        MainLayout layout = new MainLayout();
        assertThat(layout).isNotNull();
    }
}

