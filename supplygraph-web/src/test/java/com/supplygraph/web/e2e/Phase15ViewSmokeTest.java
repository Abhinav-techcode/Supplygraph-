package com.supplygraph.web.e2e;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.supplygraph.web.views.admin.*;
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
@DisplayName("Phase 15 Enterprise Platform Administration Smoke Test")
class Phase15ViewSmokeTest {

    @Autowired private ApplicationContext ctx;

    @Test
    @DisplayName("All 13 Phase 15 admin views instantiate with Spring beans and have unique routes")
    void allPhase15ViewsInstantiateWithRealServicesAndHaveDistinctRoutes() throws Exception {
        List<Class<?>> views = List.of(
                EnterpriseAdministrationCentreView.class,
                WorkspaceConfigurationCentreView.class,
                IndustryPolicyConfigurationView.class,
                ConfigurationVersionHistoryView.class,
                ConfigurationComparisonView.class,
                UserRoleAdministrationView.class,
                ApiDeveloperCentreView.class,
                ExtensionRegistryView.class,
                PlatformHealthDashboardView.class,
                PerformanceMonitoringDashboardView.class,
                BackupRecoveryCentreView.class,
                ReleaseVersionInfoView.class,
                AuditGovernanceCentreView.class
        );

        assertThat(views.stream().map(v -> v.getAnnotation(Route.class).value()).distinct())
                .as("All 13 Phase 15 views must have distinct route values")
                .hasSize(13);

        SecurityContext.setCurrentUser(UiPrincipalFilter.principalFor(UserRole.SYSTEM_ADMIN));
        try {
            for (Class<?> v : views) {
                Constructor<?> c = v.getConstructors()[0];
                Object[] args = java.util.Arrays.stream(c.getParameterTypes())
                        .map(paramType -> {
                            if (paramType.equals(List.class)) {
                                return List.of();
                            }
                            return ctx.getBean(paramType);
                        })
                        .toArray();
                Object instance = c.newInstance(args);
                assertThat(instance).as(v.getSimpleName() + " instantiation").isNotNull();
            }
        } finally {
            SecurityContext.clear();
        }
    }
}
