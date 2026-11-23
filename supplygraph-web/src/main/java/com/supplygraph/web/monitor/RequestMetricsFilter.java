package com.supplygraph.web.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Times every application request (not static assets) and hands the result to {@link MonitoringService}. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestMetricsFilter extends OncePerRequestFilter {
    private final MonitoringService monitoring;

    public RequestMetricsFilter(MonitoringService monitoring) { this.monitoring = monitoring; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/VAADIN/") || p.startsWith("/frontend/") || p.startsWith("/icons/") || p.startsWith("/sw.js") || p.endsWith(".js") || p.endsWith(".css")
                || p.endsWith(".png") || p.endsWith(".ico") || p.endsWith(".svg") || p.endsWith(".woff2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        long t0 = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(req, res);
            status = res.getStatus();
        } finally {
            String path = req.getRequestURI();
            String area = req.getParameter("v-r") != null ? "UI events" : path.startsWith("/api/") ? "REST API" : path.equals("/") || path.isEmpty() ? "Page loads" : "Page loads";
            monitoring.recordRequest(area, (System.nanoTime() - t0) / 1_000_000, status);
        }
    }
}
