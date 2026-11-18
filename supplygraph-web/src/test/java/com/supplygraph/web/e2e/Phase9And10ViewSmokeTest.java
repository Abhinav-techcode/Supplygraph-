package com.supplygraph.web.e2e;

import com.supplygraph.web.views.integration.*;
import com.supplygraph.web.views.execution.*;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.supplygraph.domain.model.security.UserRole;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Every Phase 9 view must construct against real Spring services (H2 context) and declare a unique route. */
@SpringBootTest
class Phase9And10ViewSmokeTest {
    @Autowired ApplicationContext ctx;

    @Test void allPhase9And10ViewsInstantiateWithRealServicesAndHaveDistinctRoutes() throws Exception {
        List<Class<?>> views = List.of(EnterpriseIntegrationCentreView.class, ConnectorRegistryView.class, ConnectorConfigurationView.class,
                ConnectorHealthMonitorView.class, SynchronizationCentreView.class, SynchronizationHistoryView.class, EventDeliveryMonitorView.class,
                DeadLetterManagementView.class, EventReplayCentreView.class, DataReconciliationCentreView.class, IntegrationSecurityAuditView.class,
                RuntimeOperationsDashboardView.class,
                RecoveryExecutionCentreView.class, ExecutionWorkflowDesignerView.class, ExecutionApprovalCentreView.class, TaskDependencyExplorerView.class,
                LiveExecutionMonitorView.class, ExecutionFailureManagementView.class, ExternalAcknowledgementMonitorView.class, ReconciliationWorkspaceView.class,
                CompensationRecoveryCentreView.class, ExecutionAuditTimelineView.class, PlanVersusActualAnalyticsView.class, ClosedLoopIntelligenceDashboardView.class);
        assertThat(views.stream().map(v -> v.getAnnotation(Route.class).value()).distinct()).hasSize(24);
        SecurityContext.setCurrentUser(UiPrincipalFilter.principalFor(UserRole.SUPPLY_CHAIN_MANAGER));
        try {
            for (Class<?> v : views) {
                Constructor<?> c = v.getConstructors()[0];
                Object[] args = java.util.Arrays.stream(c.getParameterTypes()).map(t -> t == org.springframework.beans.factory.ObjectProvider.class
                        ? ctx.getBeanProvider(com.supplygraph.adapter.integration.OutboxDispatcherWorker.class) : ctx.getBean(t)).toArray();
                assertThat(c.newInstance(args)).as(v.getSimpleName()).isNotNull();
            }
        } finally {
            SecurityContext.clear();
        }
    }
}
