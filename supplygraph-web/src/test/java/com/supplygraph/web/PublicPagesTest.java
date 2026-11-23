package com.supplygraph.web;

import com.supplygraph.application.security.AuthenticationService;
import com.supplygraph.web.status.SystemStatusPanel;
import com.supplygraph.web.status.SystemStatusService;
import com.supplygraph.web.views.LandingView;
import com.supplygraph.web.views.LoginView;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Public pages: welcome and sign-in")
class PublicPagesTest {
    private static SystemStatusService status(boolean up, boolean persistent) {
        var s = Mockito.mock(SystemStatusService.class);
        Mockito.when(s.current()).thenReturn(new SystemStatusService.Status(up, "H2 2.3", "jdbc:h2:mem:x", persistent, "18", 18, 18, 2, 8, up ? null : "down"));
        return s;
    }

    private static String text(Component c) {
        return Stream.concat(Stream.of(c), c.getChildren().flatMap(ch -> Stream.of(ch))).flatMap(x -> Stream.concat(Stream.of(x.getElement().getText()), x.getChildren().map(PublicPagesTest::text)))
                .collect(Collectors.joining(" "));
    }

    @Test
    @DisplayName("The welcome page explains the product and how to use it")
    void landingExplains() {
        String t = text(new LandingView(status(true, false)));
        assertThat(t).contains("See the disruption coming", "How to use SupplyGraph", "Add your data", "Execute and learn", "Built to be trusted", "No language model, external AI service or API key is involved");
    }

    @Test
    @DisplayName("The status line says honestly when data is only in memory, and when the database is down")
    void statusIsHonest() {
        assertThat(text(new SystemStatusPanel(status(true, false).current(), false))).contains("in memory", "lost");
        assertThat(text(new SystemStatusPanel(status(true, true).current(), false))).doesNotContain("lost");
        assertThat(text(new SystemStatusPanel(status(false, false).current(), false))).contains("unavailable");
    }

    @Test
    @DisplayName("The sign-in page builds without any signed-in user and hides demo accounts unless enabled")
    void loginPage() {
        AuthenticationService auth = Mockito.mock(AuthenticationService.class);
        assertThat(text(new LoginView(auth, status(true, true), false, "x"))).doesNotContain("demo accounts");
        assertThat(text(new LoginView(auth, status(true, true), true, "Demo!2026"))).contains("demo accounts", "Demo!2026");
    }
}
