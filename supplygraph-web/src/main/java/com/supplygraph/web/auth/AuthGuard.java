package com.supplygraph.web.auth;

import com.supplygraph.web.views.LandingView;
import com.supplygraph.web.views.LoginView;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Every page except the public welcome and sign-in pages requires a signed-in user. */
@Component
public class AuthGuard implements VaadinServiceInitListener {
    private final boolean required;

    public AuthGuard(@Value("${supplygraph.auth.required:true}") boolean required) { this.required = required; }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(init -> init.getUI().addBeforeEnterListener(this::check));
    }

    private void check(BeforeEnterEvent e) {
        if (!required) return;
        Class<?> target = e.getNavigationTarget();
        boolean isPublic = LandingView.class.equals(target) || LoginView.class.equals(target);
        if (isPublic) {
            if (AuthSession.isSignedIn() && LoginView.class.equals(target)) e.forwardTo("");
            return;
        }
        if (!AuthSession.isSignedIn()) {
            if (e.getLocation().getPath().isEmpty()) e.forwardTo(LandingView.class);
            else e.forwardTo(LoginView.class);
        }
    }
}
