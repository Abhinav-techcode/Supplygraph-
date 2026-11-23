package com.supplygraph.web.monitor;

import com.supplygraph.application.error.ErrorClassifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory feed of the latest classified errors (UI and REST), newest first. Feeds the monitoring page; the log files remain the permanent record. */
public final class RecentErrors {
    public record Entry(Instant at, String source, String code, String message, String reference) {}

    private static final int MAX = 100;
    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());

    private RecentErrors() {}

    public static void record(String source, ErrorClassifier.Classified c) {
        synchronized (ENTRIES) {
            ENTRIES.add(0, new Entry(Instant.now(), source, c.code().name(), c.message(), c.reference()));
            while (ENTRIES.size() > MAX) ENTRIES.remove(ENTRIES.size() - 1);
        }
    }

    public static List<Entry> latest(int n) {
        synchronized (ENTRIES) { return new ArrayList<>(ENTRIES.subList(0, Math.min(n, ENTRIES.size()))); }
    }

    public static long countSince(Instant since) {
        synchronized (ENTRIES) { return ENTRIES.stream().filter(e -> e.at().isAfter(since)).count(); }
    }

    public static void clear() { ENTRIES.clear(); }
}
