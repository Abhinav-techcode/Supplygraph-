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
 * IMPORTANT: SupplyGraph has no login system yet. The principal is a configured operator (role + workspace) held in the HTTP session;
 * an optional demo role switcher (supplygraph.ui.demo-role-switcher=true) lets one browser session act as different roles to
 * exercise separation of duties. This is NOT authentication and must not be exposed on an untrusted network.
 * All services still enforce permissions and workspace isolation server-side against this principal.
 */
@Component
public class UiPrincipalFilter extends OncePerRequestFilter {
    public static final String SESSION_ATTRIBUTE = "supplygraph.ui.principal";
    public static final String WORKSPACE = "default-workspace";

    private final UserRole defaultRole;
    private final com.supplygraph.application.port.out.UserProfileRepositoryPort users;

    public UiPrincipalFilter(@Value("${supplygraph.ui.default-role:SUPPLY_CHAIN_MANAGER}") String defaultRole,
                             com.supplygraph.application.port.out.UserProfileRepositoryPort users) {
        this.defaultRole = UserRole.valueOf(defaultRole);
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
        if (principal == null) {
            principal = principalFor(defaultRole);
            session.setAttribute(SESSION_ATTRIBUTE, principal);
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
