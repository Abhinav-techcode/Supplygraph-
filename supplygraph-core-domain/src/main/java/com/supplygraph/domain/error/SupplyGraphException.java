package com.supplygraph.domain.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Root of every deliberate failure in the platform. It carries a stable {@link ErrorCode} and optional structured details, so the UI, the REST
 * layer and the logs can react to the kind of failure without parsing messages. Unchecked on purpose: callers that cannot recover let it propagate
 * to the central handlers.
 */
public class SupplyGraphException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final Map<String, Object> details = new LinkedHashMap<>();

    public SupplyGraphException(ErrorCode code, String message) { this(code, message, null); }

    public SupplyGraphException(ErrorCode code, String message, Throwable cause) {
        super(message == null || message.isBlank() ? Objects.requireNonNull(code).defaultMessage() : message, cause);
        this.code = Objects.requireNonNull(code);
    }

    public ErrorCode code() { return code; }

    /** Adds a structured detail (for example the offending field or the id of the missing item); returns this for chaining. */
    public SupplyGraphException with(String key, Object value) { details.put(key, value); return this; }

    public Map<String, Object> details() { return Collections.unmodifiableMap(details); }

    /** Text safe to show to an end user. */
    public String userMessage() { return code.messageIsUserSafe() ? getMessage() : code.defaultMessage(); }
}
