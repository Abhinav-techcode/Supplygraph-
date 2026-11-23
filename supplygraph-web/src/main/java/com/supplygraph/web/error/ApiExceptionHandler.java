package com.supplygraph.web.error;

import com.supplygraph.application.error.ErrorClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consistent JSON errors for the REST endpoints (webhooks and API). The body always has the same shape: status, a stable {@code code}, a safe
 * message, structured details, a retry hint and, for unexpected failures, a support reference. Standard Spring MVC errors (malformed body, missing
 * parameter) keep their usual 4xx handling from the parent class.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e) {
        ErrorClassifier.Classified c = ErrorClassifier.classify(e);
        com.supplygraph.web.monitor.RecentErrors.record("REST", c);
        if (c.unexpected()) log.error("[{}] {} {}", c.reference(), c.code(), c.message(), c.cause());
        else log.warn("{}: {}", c.code(), c.message());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", c.code().httpStatus());
        body.put("code", c.code().name());
        body.put("message", c.message());
        if (!c.details().isEmpty()) body.put("details", c.details());
        body.put("retryable", c.code().retryable());
        if (c.reference() != null) body.put("reference", c.reference());
        return ResponseEntity.status(HttpStatus.valueOf(c.code().httpStatus())).body(body);
    }
}
