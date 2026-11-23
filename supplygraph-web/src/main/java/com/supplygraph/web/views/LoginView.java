package com.supplygraph.web.views;

import com.supplygraph.application.security.AuthenticationService;
import com.supplygraph.web.auth.AuthSession;
import com.supplygraph.web.status.SystemStatusPanel;
import com.supplygraph.web.status.SystemStatusService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

/** Sign-in page (public). Failures are explained in plain words; lockout and password checks live in {@link AuthenticationService}. */
@Route("login")
@PageTitle("Sign in | SupplyGraph")
public class LoginView extends Div {
    public LoginView(AuthenticationService auth, SystemStatusService status,
                     @Value("${supplygraph.auth.show-demo-accounts:false}") boolean showDemo,
                     @Value("${supplygraph.auth.demo-password:Demo!2026}") String demoPassword) {
        addClassName("sg-login-page");
        Div card = new Div();
        card.addClassName("sg-login-card");
        Anchor brand = new Anchor("welcome", "SupplyGraph");
        brand.addClassName("sg-brand");
        H2 title = new H2("Sign in");
        title.getStyle().set("margin", "8px 0 4px");
        Paragraph sub = new Paragraph("Use your SupplyGraph account to open the control tower.");
        sub.getStyle().set("margin", "0 0 12px").set("color", "var(--lumo-secondary-text-color)");

        LoginForm form = new LoginForm();
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getForm().setTitle("");
        i18n.getForm().setUsername("Username");
        i18n.getForm().setSubmit("Sign in");
        i18n.getForm().setForgotPassword("");
        i18n.getErrorMessage().setTitle("Could not sign in");
        form.setI18n(i18n);
        form.setForgotPasswordButtonVisible(false);
        form.addLoginListener(e -> {
            try {
                var r = auth.authenticate(e.getUsername(), e.getPassword());
                if (r.success()) {
                    AuthSession.signIn(r.user());
                    UI.getCurrent().getPage().setLocation("/");
                } else {
                    i18n.getErrorMessage().setMessage(r.message());
                    form.setI18n(i18n);
                    form.setError(true);
                }
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(LoginView.class).error("Sign-in failed unexpectedly", ex);
                i18n.getErrorMessage().setMessage("Sign-in is temporarily unavailable. Please try again in a moment.");
                form.setI18n(i18n);
                form.setError(true);
            }
        });
        card.add(brand, title, sub, form);
        if (showDemo) {
            Div demo = new Div();
            demo.addClassName("sg-demo-accounts");
            demo.add(new Span("Local demo accounts (password: " + demoPassword + ")"));
            demo.add(new Paragraph("admin · manager · procurement · production · logistics · quality · analyst · viewer"));
            demo.add(new Paragraph("Tip: approving your own plan is blocked by separation of duties, so use two different accounts to see the full approval flow."));
            card.add(demo);
        }
        Button back = new Button("Back to the welcome page", e -> UI.getCurrent().navigate(LandingView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        card.add(back, new SystemStatusPanel(status.current(), false));
        add(card);
    }
}
