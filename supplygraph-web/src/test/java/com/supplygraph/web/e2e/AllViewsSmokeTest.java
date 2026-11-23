package com.supplygraph.web.e2e;

import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.domain.model.security.UserRole;
import com.supplygraph.web.security.UiPrincipalFilter;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Builds every routed page against the real services (H2), as a signed-in manager. A page that cannot be built would show the error page to a user. */
@SpringBootTest
@DisplayName("Every page can be built")
class AllViewsSmokeTest {
    @Autowired ApplicationContext ctx;

    @Test
    void everyRoutedViewConstructs() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));
        List<String> failures = new ArrayList<>();
        Map<String, String> routes = new TreeMap<>();
        int built = 0;
        SecurityContext.setCurrentUser(UiPrincipalFilter.principalFor(UserRole.SUPPLY_CHAIN_MANAGER));
        try {
            for (var bd : scanner.findCandidateComponents("com.supplygraph.web")) {
                Class<?> view = Class.forName(bd.getBeanClassName());
                Route r = view.getAnnotation(Route.class);
                String path = r.value().isEmpty() ? "(home)" : r.value();
                if (routes.put(path, view.getSimpleName()) != null && !path.contains(":")) failures.add("DUPLICATE ROUTE " + path);
                Constructor<?> c = Arrays.stream(view.getConstructors()).max(Comparator.comparingInt(Constructor::getParameterCount)).orElse(null);
                if (c == null) { failures.add(view.getSimpleName() + ": no public constructor"); continue; }
                try {
                    Object[] args = Arrays.stream(c.getParameters()).map(p -> arg(p.getType())).toArray();
                    assertThat(c.newInstance(args)).isNotNull();
                    built++;
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    failures.add(view.getSimpleName() + " (/" + path + "): " + t.getClass().getSimpleName() + ": " + t.getMessage());
                } catch (RuntimeException e) {
                    failures.add(view.getSimpleName() + ": " + e);
                }
            }
        } finally {
            SecurityContext.clear();
        }
        System.out.println("VIEWS BUILT: " + built + " of " + routes.size());
        assertThat(failures).as("pages that fail to build").isEmpty();
    }

    private Object arg(Class<?> t) {
        if (t == ObjectProvider.class) return ctx.getBeanProvider(com.supplygraph.adapter.integration.OutboxDispatcherWorker.class);
        if (t == boolean.class) return false;
        if (t == String.class) return "";
        if (t == int.class || t == long.class) return 0;
        try { return ctx.getBean(t); } catch (RuntimeException e) { return null; }
    }
}
