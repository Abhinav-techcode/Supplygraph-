package com.supplygraph.web;

import com.supplygraph.web.layout.MainLayout;
import com.supplygraph.web.views.*;
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
    @DisplayName("Should verify DisruptionsView is annotated with 'disruptions' route and MainLayout")
    void shouldVerifyDisruptionsViewRoute() {
        Route route = DisruptionsView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("disruptions");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = DisruptionsView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Disruption");
    }

    @Test
    @DisplayName("Should verify ImpactAssessmentView is annotated with 'impact' route and MainLayout")
    void shouldVerifyImpactAssessmentViewRoute() {
        Route route = ImpactAssessmentView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("impact");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = ImpactAssessmentView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Impact");
    }

    @Test
    @DisplayName("Should verify SimulationWorkspaceView is annotated with 'simulation' route and MainLayout")
    void shouldVerifySimulationWorkspaceViewRoute() {
        Route route = SimulationWorkspaceView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("simulation");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = SimulationWorkspaceView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Simulation");
    }

    @Test
    @DisplayName("Should verify MonteCarloWorkspaceView is annotated with 'monte-carlo' route and MainLayout")
    void shouldVerifyMonteCarloWorkspaceViewRoute() {
        Route route = MonteCarloWorkspaceView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("monte-carlo");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = MonteCarloWorkspaceView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Monte Carlo");
    }

    @Test
    @DisplayName("Should verify OptimizationWorkspaceView is annotated with 'optimization' route and MainLayout")
    void shouldVerifyOptimizationWorkspaceViewRoute() {
        Route route = OptimizationWorkspaceView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("optimization");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = OptimizationWorkspaceView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Optimization");
    }

    @Test
    @DisplayName("Should verify RecoveryDecisionCentreView is annotated with 'decision-centre' route and MainLayout")
    void shouldVerifyRecoveryDecisionCentreViewRoute() {
        Route route = RecoveryDecisionCentreView.class.getAnnotation(Route.class);
        assertThat(route).isNotNull();
        assertThat(route.value()).isEqualTo("decision-centre");
        assertThat(route.layout()).isEqualTo(MainLayout.class);

        PageTitle pageTitle = RecoveryDecisionCentreView.class.getAnnotation(PageTitle.class);
        assertThat(pageTitle).isNotNull();
        assertThat(pageTitle.value()).contains("Recovery Decision");
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
    @DisplayName("Should instantiate all 7 Phase 4 views and verify component hierarchy")
    void shouldInstantiateAllPhase4Views() {
        HomeView homeView = new HomeView();
        assertThat(homeView.getComponentCount()).isGreaterThanOrEqualTo(3);

        DisruptionsView disruptionsView = new DisruptionsView();
        assertThat(disruptionsView.getComponentCount()).isGreaterThanOrEqualTo(3);

        ImpactAssessmentView impactView = new ImpactAssessmentView();
        assertThat(impactView.getComponentCount()).isGreaterThanOrEqualTo(2);

        SimulationWorkspaceView simView = new SimulationWorkspaceView();
        assertThat(simView.getComponentCount()).isGreaterThanOrEqualTo(2);

        MonteCarloWorkspaceView mcView = new MonteCarloWorkspaceView();
        assertThat(mcView.getComponentCount()).isGreaterThanOrEqualTo(2);

        OptimizationWorkspaceView optView = new OptimizationWorkspaceView();
        assertThat(optView.getComponentCount()).isGreaterThanOrEqualTo(2);

        RecoveryDecisionCentreView decisionView = new RecoveryDecisionCentreView();
        assertThat(decisionView.getComponentCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should instantiate MainLayout and verify navigation shell")
    void shouldInstantiateMainLayout() {
        MainLayout layout = new MainLayout();
        assertThat(layout).isNotNull();
    }
}
