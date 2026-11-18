package com.supplygraph.web.e2e;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.supplygraph.web.views.planning.*;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 11 Planning Views Smoke & Instantiation Test")
class Phase11ViewSmokeTest {

    @Autowired private ApplicationContext ctx;

    @Test
    @DisplayName("All 15 Phase 11 planning views instantiate with Spring beans and have unique routes")
    void allPhase11ViewsInstantiateWithRealServicesAndHaveDistinctRoutes() throws Exception {
        List<Class<?>> views = List.of(
                PlanningCommandCentreView.class,
                DemandPlanningWorkspaceView.class,
                DemandForecastDashboardView.class,
                ForecastDetailExplorerView.class,
                InventoryOptimizationCentreView.class,
                SafetyStockPlannerView.class,
                ReorderPlanningWorkspaceView.class,
                ProcurementPlanningCentreView.class,
                SupplierAllocationComparisonView.class,
                ProductionPlanningWorkspaceView.class,
                CapacityUtilizationExplorerView.class,
                MultiEchelonNetworkPlannerView.class,
                PlanningScenarioDesignerView.class,
                PlanningResultsExplorerView.class,
                PlanningApprovalHandoffView.class
        );

        assertThat(views.stream().map(v -> v.getAnnotation(Route.class).value()).distinct())
                .as("All 15 planning views must have distinct route values")
                .hasSize(15);

        SecurityContext.setCurrentUser(UiPrincipalFilter.principalFor(UserRole.SUPPLY_CHAIN_MANAGER));
        try {
            for (Class<?> v : views) {
                Constructor<?> c = v.getConstructors()[0];
                Object[] args = java.util.Arrays.stream(c.getParameterTypes())
                        .map(ctx::getBean)
                        .toArray();
                Object instance = c.newInstance(args);
                assertThat(instance).as(v.getSimpleName() + " instantiation").isNotNull();
            }
        } finally {
            SecurityContext.clear();
        }
    }
}

