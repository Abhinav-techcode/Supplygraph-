package com.supplygraph.web.error;

import com.supplygraph.domain.error.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("REST error responses have one shape")
class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("Not found: 404 with code, message and details, no support reference")
    void notFound() {
        var r = handler.handle(new ResourceNotFoundException("Disruption", "D1"));
        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getBody()).containsEntry("code", "NOT_FOUND").containsEntry("retryable", false).doesNotContainKey("reference");
        assertThat(r.getBody().get("message")).isEqualTo("Disruption not found: D1");
        assertThat(r.getBody().get("details")).isEqualTo(java.util.Map.of("type", "Disruption", "id", "D1"));
    }

    @Test
    @DisplayName("Business conflict and permission problems map to 409 and 403")
    void conflictAndForbidden() {
        assertThat(handler.handle(new ConflictException("stale")).getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handle(new InvalidStateException("Workflow", "BLOCKED")).getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handle(new AccessDeniedException("no")).getStatusCode().value()).isEqualTo(403);
        assertThat(handler.handle(new BusinessRuleException("rule")).getStatusCode().value()).isEqualTo(422);
    }

    @Test
    @DisplayName("Unexpected failures give 500, a generic message and a reference, never the internal text")
    void unexpected() {
        var r = handler.handle(new IllegalArgumentException("fine")); // domain invariant: validation
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        var boom = handler.handle(new NullPointerException("secret table name"));
        assertThat(boom.getStatusCode().value()).isEqualTo(500);
        assertThat(boom.getBody().get("message").toString()).doesNotContain("secret");
        assertThat(boom.getBody()).containsKey("reference");
        var storage = handler.handle(new StorageFailureException("bad json in column x", new RuntimeException()));
        assertThat(storage.getBody().get("message").toString()).doesNotContain("column x");
        assertThat(storage.getBody()).containsKey("reference");
    }
}
