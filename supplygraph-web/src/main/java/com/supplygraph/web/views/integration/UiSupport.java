package com.supplygraph.web.views.integration;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorMode;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/** Shared UI helpers: uniform error handling (no stack traces to the user), timestamps, and SIMULATED labelling. */
public final class UiSupport {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private UiSupport() {}

    public static WorkspaceId workspace() { return WorkspaceId.of(UiPrincipalFilter.WORKSPACE); }

    public static String ts(Instant i) { return i == null ? "—" : TS.format(i); }

    public static String dur(Duration d) { return d == null ? "unknown" : d.toMillis() + " ms"; }

    public static String pct(Double d) { return d == null ? "unknown" : String.format("%.1f%%", d * 100); }

    public static Span simulatedBadge(boolean simulated) {
        Span s = new Span(simulated ? "SIMULATED" : "LIVE");
        s.getElement().getThemeList().add(simulated ? "badge contrast" : "badge success");
        return s;
    }

    public static String modeLabel(ConnectorMode m) { return m == ConnectorMode.SANDBOX_SIMULATED ? "SIMULATED (sandbox)" : "LIVE"; }

    public static String currentUser() {
        var u = SecurityContext.getCurrentUser();
        return u == null ? "anonymous" : u.displayName() + " (" + u.role() + ")";
    }

    /** Runs an action; reports success or a concise error. Returns true on success. */
    public static boolean run(String successMessage, Runnable action) {
        try {
            action.run();
            Notification n = Notification.show(successMessage, 3000, Notification.Position.BOTTOM_START);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (RuntimeException e) {
            error(e);
            return false;
        }
    }

    public static <T> T call(Supplier<T> action, T fallback) {
        try { return action.get(); } catch (RuntimeException e) { error(e); return fallback; }
    }

    public static void error(RuntimeException e) {
        String msg = e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
        Notification n = Notification.show(msg, 6000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
