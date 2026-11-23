package com.supplygraph.web.error;

import com.supplygraph.application.error.ErrorClassifier;
import org.slf4j.Logger;

/** Turns any failure into the text a user sees, logging it at the right level. Shared by the global handler and the per-action helpers. */
public final class UiErrors {
    private UiErrors() {}

    public static String describe(Throwable t, Logger log) {
        ErrorClassifier.Classified c = ErrorClassifier.classify(t);
        com.supplygraph.web.monitor.RecentErrors.record("UI", c);
        if (c.unexpected()) {
            log.error("[{}] {} {}", c.reference(), c.code(), c.message(), c.cause());
            return c.message() + " Reference " + c.reference() + ".";
        }
        log.warn("{}: {}", c.code(), c.message());
        return c.message();
    }
}
