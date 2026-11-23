package com.supplygraph.application.error;

import com.supplygraph.domain.error.ErrorCode;
import com.supplygraph.domain.error.SupplyGraphException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * One place that decides what any {@link Throwable} means to a user: the error code, a message that is safe to show, structured details, and
 * whether it deserves an error-level log with a support reference. Our own {@link SupplyGraphException}s are used as they are; the JDK exceptions
 * that the domain model uses for invariants are mapped to the matching code, so UI and REST behave identically.
 */
public final class ErrorClassifier {
    /** @param reference short id to quote to support; present only for failures that were logged as errors */
    public record Classified(ErrorCode code, String message, Map<String, Object> details, String reference, Throwable cause) {
        public boolean unexpected() { return reference != null; }
    }

    private ErrorClassifier() {}

    public static Classified classify(Throwable raw) {
        Throwable t = unwrap(raw);
        if (t instanceof SupplyGraphException e) {
            boolean serious = !e.code().messageIsUserSafe() || e.code() == ErrorCode.SECURITY_VIOLATION;
            return new Classified(e.code(), e.userMessage(), new LinkedHashMap<>(e.details()), serious ? reference() : null, e);
        }
        if (t instanceof NoSuchElementException e) return plain(ErrorCode.NOT_FOUND, e);
        if (t instanceof IllegalArgumentException e) return plain(ErrorCode.VALIDATION_FAILED, e);
        if (t instanceof IllegalStateException e) return plain(ErrorCode.INVALID_STATE, e);
        if (t instanceof SecurityException e) return new Classified(ErrorCode.SECURITY_VIOLATION, ErrorCode.SECURITY_VIOLATION.defaultMessage(), Map.of(), reference(), e);
        return new Classified(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), Map.of(), reference(), t);
    }

    private static Classified plain(ErrorCode code, RuntimeException e) {
        String m = e.getMessage();
        return new Classified(code, m == null || m.isBlank() ? code.defaultMessage() : m, Map.of(), null, e);
    }

    private static Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && (cur instanceof CompletionException || cur instanceof java.lang.reflect.InvocationTargetException
                || cur instanceof java.lang.reflect.UndeclaredThrowableException || cur.getClass().getName().startsWith("org.springframework.transaction")
                || cur.getClass().getName().equals("java.lang.RuntimeException") && cur.getMessage() != null && cur.getMessage().equals(String.valueOf(cur.getCause())))) cur = cur.getCause();
        return cur;
    }

    private static String reference() { return UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
}
