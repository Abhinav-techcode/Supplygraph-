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
    @DisplayName("Should verify Phase 5 continuous monitoring views routes and annotations")
    void shouldVerifyPhase5ViewRoutes() {
        Route r1 = OperationalEventMonitorView.class.getAnnotation(Route.class);
        assertThat(r1).isNotNull();
        assertThat(r1.value()).isEqualTo("events");
        assertThat(r1.layout()).isEqualTo(MainLayout.class);

        Route r2 = EventIngestionHistoryView.class.getAnnotation(Route.class);
        assertThat(r2).isNotNull();
        assertThat(r2.value()).isEqualTo("event-ingestion");
        assertThat(r2.layout()).isEqualTo(MainLayout.class);

        Route r3 = MonitoringRulesView.class.getAnnotation(Route.class);
        assertThat(r3).isNotNull();
        assertThat(r3.value()).isEqualTo("monitoring-rules");
        assertThat(r3.layout()).isEqualTo(MainLayout.class);

        Route r4 = RiskAlertCentreView.class.getAnnotation(Route.class);
        assertThat(r4).isNotNull();
        assertThat(r4.value()).isEqualTo("risk-alerts");
        assertThat(r4.layout()).isEqualTo(MainLayout.class);

        Route r5 = ImpactReassessmentHistoryView.class.getAnnotation(Route.class);
        assertThat(r5).isNotNull();
        assertThat(r5.value()).isEqualTo("reassessments");
        assertThat(r5.layout()).isEqualTo(MainLayout.class);

        Route r6 = AdaptiveReplanningMonitorView.class.getAnnotation(Route.class);
        assertThat(r6).isNotNull();
        assertThat(r6.value()).isEqualTo("adaptive-replanning");
        assertThat(r6.layout()).isEqualTo(MainLayout.class);

        Route r7 = AgentInvestigationHistoryView.class.getAnnotation(Route.class);
        assertThat(r7).isNotNull();
        assertThat(r7.value()).isEqualTo("agent-history");
        assertThat(r7.layout()).isEqualTo(MainLayout.class);
    }

    @Test
    @DisplayName("Should verify Phase 6 security, governance, provenance, and benchmark views routes")
    void shouldVerifyPhase6ViewRoutes() {
        Route r1 = SecurityAdminView.class.getAnnotation(Route.class);
        assertThat(r1).isNotNull();
        assertThat(r1.value()).isEqualTo("security-admin");
        assertThat(r1.layout()).isEqualTo(MainLayout.class);

        Route r2 = AuditGovernanceView.class.getAnnotation(Route.class);
        assertThat(r2).isNotNull();
        assertThat(r2.value()).isEqualTo("audit-log");
        assertThat(r2.layout()).isEqualTo(MainLayout.class);

        Route r3 = ProvenanceExplorerView.class.getAnnotation(Route.class);
        assertThat(r3).isNotNull();
        assertThat(r3.value()).isEqualTo("provenance");
        assertThat(r3.layout()).isEqualTo(MainLayout.class);

        Route r4 = BenchmarkStudioView.class.getAnnotation(Route.class);
        assertThat(r4).isNotNull();
        assertThat(r4.value()).isEqualTo("benchmark-studio");
        assertThat(r4.layout()).isEqualTo(MainLayout.class);

        Route r5 = HealthView.class.getAnnotation(Route.class);
        assertThat(r5).isNotNull();
        assertThat(r5.value()).isEqualTo("health");
        assertThat(r5.layout()).isEqualTo(MainLayout.class);
    }

    @Test
    @DisplayName("Should instantiate MainLayout and verify navigation shell")
    void shouldInstantiateMainLayout() {
        MainLayout layout = new MainLayout();
        assertThat(layout).isNotNull();
    }
}
