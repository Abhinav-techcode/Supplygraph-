package com.supplygraph.web.auth;

import com.supplygraph.application.security.AuthenticationService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * First-run accounts. Demo accounts exist only when {@code supplygraph.auth.seed-demo-users=true} (the local profile). Otherwise an
 * {@code admin} account is created from SUPPLYGRAPH_ADMIN_PASSWORD if nobody can sign in yet. Passwords are never logged.
 */
@Component
@Order(100)
public class UserAccountSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(UserAccountSeeder.class);
    private static final WorkspaceId WS = WorkspaceId.of("WS-AUTO-01");

    private final AuthenticationService auth;
    private final boolean seedDemo;
    private final String demoPassword;
    private final String adminPassword;

    public UserAccountSeeder(AuthenticationService auth, @Value("${supplygraph.auth.seed-demo-users:false}") boolean seedDemo,
                             @Value("${supplygraph.auth.demo-password:Demo!2026}") String demoPassword,
                             @Value("${SUPPLYGRAPH_ADMIN_PASSWORD:}") String adminPassword) {
        this.auth = auth;
        this.seedDemo = seedDemo;
        this.demoPassword = demoPassword;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedDemo) {
            auth.register("admin", "Alex Admin", "admin@example.com", UserRole.SYSTEM_ADMIN, demoPassword, WS);
            auth.register("manager", "Maya Manager", "manager@example.com", UserRole.SUPPLY_CHAIN_MANAGER, demoPassword, WS);
            auth.register("procurement", "Priya Procurement", "procurement@example.com", UserRole.PROCUREMENT_MANAGER, demoPassword, WS);
            auth.register("production", "Paul Production", "production@example.com", UserRole.PRODUCTION_MANAGER, demoPassword, WS);
            auth.register("logistics", "Lena Logistics", "logistics@example.com", UserRole.LOGISTICS_MANAGER, demoPassword, WS);
            auth.register("quality", "Quinn Quality", "quality@example.com", UserRole.QUALITY_MANAGER, demoPassword, WS);
            auth.register("analyst", "Ana Analyst", "analyst@example.com", UserRole.ANALYST, demoPassword, WS);
            auth.register("viewer", "Victor Viewer", "viewer@example.com", UserRole.VIEWER, demoPassword, WS);
            log.warn("Demo accounts created (admin, manager, procurement, production, logistics, quality, analyst, viewer). Never enable this outside local use.");
        } else if (!auth.hasAnyCredential()) {
            if (adminPassword.isBlank()) log.error("No one can sign in: set SUPPLYGRAPH_ADMIN_PASSWORD (8+ characters) to create the first administrator.");
            else {
                auth.register("admin", "Administrator", "admin@localhost", UserRole.SYSTEM_ADMIN, adminPassword, WS);
                log.info("Created the first administrator account 'admin'.");
            }
        }
    }
}
