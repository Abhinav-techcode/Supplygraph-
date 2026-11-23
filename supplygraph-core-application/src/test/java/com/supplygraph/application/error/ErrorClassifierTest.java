package com.supplygraph.application.error;

import com.supplygraph.domain.error.*;
import com.supplygraph.domain.model.security.UnauthorizedOperationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Custom exception hierarchy and classification")
class ErrorClassifierTest {

    @Test
    @DisplayName("Every custom exception is a SupplyGraphException with the right code, status and retry hint")
    void codesAndStatuses() {
        assertThat(new ValidationException("x").code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(new ResourceNotFoundException("Plan", "P1").getMessage()).isEqualTo("Plan not found: P1");
        assertThat(new ResourceNotFoundException("Plan", "P1").details()).containsEntry("type", "Plan").containsEntry("id", "P1");
        assertThat(new InvalidStateException("Workflow", "BLOCKED").code().httpStatus()).isEqualTo(409);
        assertThat(new ConflictException("c").code().retryable()).isTrue();
        assertThat(new BusinessRuleException("b").code().httpStatus()).isEqualTo(422);
        assertThat(new AccessDeniedException("a").code().httpStatus()).isEqualTo(403);
        assertThat(new RateLimitExceededException("k", "op").code().httpStatus()).isEqualTo(429);
        assertThat(new ExternalServiceException("erp", "down", new RuntimeException()).code().retryable()).isTrue();
        assertThat(new UnauthorizedOperationException("no")).isInstanceOf(AccessDeniedException.class);
        assertThat(new com.supplygraph.domain.model.execution.SeparationOfDutiesException("sod")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Storage and internal failures never leak their message to users")
    void internalDetailsAreHidden() {
        var storage = new StorageFailureException("column details_json is invalid at row 7", new RuntimeException("jackson"));
        assertThat(storage.userMessage()).isEqualTo(ErrorCode.STORAGE_FAILURE.defaultMessage()).doesNotContain("details_json");
        var c = ErrorClassifier.classify(storage);
        assertThat(c.code()).isEqualTo(ErrorCode.STORAGE_FAILURE);
        assertThat(c.message()).doesNotContain("details_json");
        assertThat(c.unexpected()).isTrue();          // logged as an error with a support reference
        assertThat(c.reference()).hasSize(8);
    }

    @Test
    @DisplayName("Expected failures keep their own message and carry no support reference")
    void expectedFailuresAreQuiet() {
        var c = ErrorClassifier.classify(new ConflictException("The network changed."));
        assertThat(c.message()).isEqualTo("The network changed.");
        assertThat(c.unexpected()).isFalse();
    }

    @Test
    @DisplayName("JDK exceptions used by the domain model are mapped to the matching code")
    void jdkExceptionsAreMapped() {
        assertThat(ErrorClassifier.classify(new IllegalArgumentException("bad input")).code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(ErrorClassifier.classify(new IllegalStateException("not now")).code()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(ErrorClassifier.classify(new NoSuchElementException("gone")).code()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorClassifier.classify(new SecurityException("blocked")).code()).isEqualTo(ErrorCode.SECURITY_VIOLATION);
        var unknown = ErrorClassifier.classify(new NullPointerException("oops"));
        assertThat(unknown.code()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(unknown.message()).doesNotContain("oops");
        assertThat(unknown.unexpected()).isTrue();
    }

    @Test
    @DisplayName("Wrapper exceptions are unwrapped to the real cause")
    void unwrapsWrappers() {
        var c = ErrorClassifier.classify(new CompletionException(new ResourceNotFoundException("Plan", "P9")));
        assertThat(c.code()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(c.details()).containsEntry("id", "P9");
    }

    @Test
    @DisplayName("Details can be attached fluently and are read-only to callers")
    void details() {
        var e = new ValidationException("quantity", "Quantity must be positive");
        assertThat(e.details()).containsEntry("field", "quantity");
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> e.details().put("x", 1));
    }
}
