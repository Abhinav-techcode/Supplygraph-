package com.supplygraph.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Workspace Aggregate Domain Invariant Tests")
class WorkspaceTest {

    @Test
    @DisplayName("Should create valid Workspace record when all invariants are satisfied")
    void shouldCreateValidWorkspace() {
        WorkspaceId id = WorkspaceId.of("ws-001");
        Workspace workspace = Workspace.create(id, "EV Gigafactory Berlin", "EV_BATTERY");

        assertThat(workspace.id()).isEqualTo(id);
        assertThat(workspace.name()).isEqualTo("EV Gigafactory Berlin");
        assertThat(workspace.industry()).isEqualTo("EV_BATTERY");
        assertThat(workspace.createdAt()).isNotNull();
        assertThat(workspace.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw NullPointerException when required fields are null")
    void shouldThrowWhenFieldsAreNull() {
        WorkspaceId id = WorkspaceId.of("ws-001");
        Instant now = Instant.now();

        assertThatThrownBy(() -> new Workspace(null, "Name", "EV", now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Workspace(id, null, "EV", now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Workspace(id, "Name", null, now, now))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when fields are blank")
    void shouldThrowWhenFieldsAreBlank() {
        WorkspaceId id = WorkspaceId.of("ws-001");
        Instant now = Instant.now();

        assertThatThrownBy(() -> new Workspace(id, "   ", "EV", now, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Workspace(id, "Name", "   ", now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

