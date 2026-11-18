package com.supplygraph.web.e2e;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.supplygraph.web.views.UnifiedCommandCentreView;
import com.supplygraph.web.views.workspace.*;
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
@DisplayName("Phase 14 Premium Enterprise UI/UX & Workspaces Smoke Test")
class Phase14ViewSmokeTest {

    @Autowired private ApplicationContext ctx;

    @Test
    @DisplayName("All 10 Phase 14 workspaces & command center instantiate with Spring beans and have unique routes")
    void allPhase14ViewsInstantiateWithRealServicesAndHaveDistinctRoutes() throws Exception {
        List<Class<?>> views = List.of(
                UnifiedCommandCentreView.class,
                ExecutiveWorkspaceView.class,
                SupplyChainManagerWorkspaceView.class,
                ProcurementManagerWorkspaceView.class,
                ProductionManagerWorkspaceView.class,
                LogisticsManagerWorkspaceView.class,
                QualityManagerWorkspaceView.class,
                AnalystWorkspaceView.class,
                SupplierPartnerWorkspaceView.class,
                PlatformAdminWorkspaceView.class
        );

        assertThat(views.stream().map(v -> v.getAnnotation(Route.class).value()).distinct())
                .as("All 10 Phase 14 views must have distinct route values")
                .hasSize(10);

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

