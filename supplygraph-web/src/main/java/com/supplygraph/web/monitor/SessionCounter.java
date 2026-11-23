package com.supplygraph.web.monitor;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

/** Counts live browser sessions for the monitoring page. */
@Component
public class SessionCounter implements HttpSessionListener {
    private final MonitoringService monitoring;

    public SessionCounter(MonitoringService monitoring) { this.monitoring = monitoring; }

    @Override public void sessionCreated(HttpSessionEvent se) { monitoring.sessionCreated(); }
    @Override public void sessionDestroyed(HttpSessionEvent se) { monitoring.sessionDestroyed(); }
}
