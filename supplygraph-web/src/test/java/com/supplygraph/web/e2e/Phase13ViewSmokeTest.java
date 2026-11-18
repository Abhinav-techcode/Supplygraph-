package com.supplygraph.web.e2e;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.supplygraph.web.views.collaboration.*;
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
@DisplayName("Phase 13 Supplier Collaboration Views Smoke & Instantiation Test")
class Phase13ViewSmokeTest {

    @Autowired private ApplicationContext ctx;

    @Test
    @DisplayName("All 14 Phase 13 supplier collaboration views instantiate with Spring beans and have unique routes")
    void allPhase13ViewsInstantiateWithRealServicesAndHaveDistinctRoutes() throws Exception {
        List<Class<?>> views = List.of(
                SupplierPortalView.class,
                SupplierOrganizationDashboardView.class,
                SupplierProfileCentreView.class,
                SupplierEvidenceCentreView.class,
                PurchaseOrderCollaborationView.class,
                RfqManagementWorkspaceView.class,
                SupplierQuotationSubmissionView.class,
                QuotationComparisonDashboardView.class,
                DeliveryConfirmationCentreView.class,
                CapacityConfirmationWorkspaceView.class,
                QualityCollaborationCentreView.class,
                PartnerDocumentCentreView.class,
                CollaborationActivityTimelineView.class,
                InternalSupplierRelationshipDashboardView.class
        );

        assertThat(views.stream().map(v -> v.getAnnotation(Route.class).value()).distinct())
                .as("All 14 collaboration views must have distinct route values")
                .hasSize(14);

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

