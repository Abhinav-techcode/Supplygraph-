package com.supplygraph.web.auth;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

/** Signed-in state lives in the HTTP session (the same attribute the request filter turns into the thread's principal). */
public final class AuthSession {
    private AuthSession() {}

    public static boolean isSignedIn() { return SecurityContext.getCurrentUser() != null; }

    public static void signIn(UserProfile user) {
        VaadinRequest req = VaadinRequest.getCurrent();
        if (req instanceof VaadinServletRequest sr && sr.getHttpServletRequest().getSession(false) != null) {
            sr.getHttpServletRequest().changeSessionId();   // new session id at login: no session fixation
        }
        VaadinSession.getCurrent().getSession().setAttribute(UiPrincipalFilter.SESSION_ATTRIBUTE, user);
    }

    public static void signOut() {
        VaadinSession s = VaadinSession.getCurrent();
        if (s != null) {
            s.getSession().invalidate();
            s.close();
        }
    }
}
