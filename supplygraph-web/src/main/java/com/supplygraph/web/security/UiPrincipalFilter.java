package com.supplygraph.web.security;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Establishes the thread-local {@link SecurityContext} for Vaadin requests.
 * The principal is the signed-in user (see LoginView / AuthenticationService) held in the HTTP session. With supplygraph.auth.required=false
 * (development only) a configured role is used instead. The optional demo role switcher (supplygraph.ui.demo-role-switcher=true) changes the role
 * of the current session for demos and must never be enabled on an untrusted network.
 * All services still enforce permissions and workspace isolation server-side against this principal.
 */
@Component
public class UiPrincipalFilter extends OncePerRequestFilter {
    public static final String SESSION_ATTRIBUTE = "supplygraph.ui.principal";
    public static final String WORKSPACE = "WS-AUTO-01";

    private final UserRole defaultRole;
    private final boolean authRequired;
    private final com.supplygraph.application.port.out.UserProfileRepositoryPort users;

    public UiPrincipalFilter(@Value("${supplygraph.ui.default-role:SUPPLY_CHAIN_MANAGER}") String defaultRole,
                             @Value("${supplygraph.auth.required:true}") boolean authRequired,
                             com.supplygraph.application.port.out.UserProfileRepositoryPort users) {
        this.defaultRole = UserRole.valueOf(defaultRole);
        this.authRequired = authRequired;
        this.users = users;
    }

    /** Persists the UI principal so approvals recorded under its id stay traceable to a known user profile. */
    public static void ensureRegistered(com.supplygraph.application.port.out.UserProfileRepositoryPort users, UserProfile p) {
        try {
            if (users.findById(p.id()).isEmpty()) users.save(p);
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(UiPrincipalFilter.class).warn("Could not register UI principal {}: {}", p.id(), e.getClass().getSimpleName());
        }
    }

    public static UserProfile principalFor(UserRole role) {
        return new UserProfile(com.supplygraph.domain.model.security.UserId.of("ui-" + role.name().toLowerCase()), "ui-" + role.name().toLowerCase(),
                role.name().toLowerCase() + "@local.invalid", role.getDisplayName(), role, Set.of(WorkspaceId.of(WORKSPACE)), true, java.time.Instant.now());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/webhooks/");   // webhooks authenticate by HMAC, never by a UI principal
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        UserProfile principal = (UserProfile) session.getAttribute(SESSION_ATTRIBUTE);
        if (principal == null && !authRequired) {   // development mode only: act as the configured role without signing in
            principal = principalFor(defaultRole);
            session.setAttribute(SESSION_ATTRIBUTE, principal);
        }
        if (principal == null) {   // not signed in: no principal at all, the route guard sends the user to the welcome / sign-in page
            chain.doFilter(request, response);
            return;
        }
        String flag = "supplygraph.ui.registered." + principal.id().value();
        if (session.getAttribute(flag) == null) {
            ensureRegistered(users, principal);
            session.setAttribute(flag, Boolean.TRUE);
        }
        SecurityContext.setCurrentUser(principal);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }
}
