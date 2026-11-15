package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "digital_twin_states")
public class DigitalTwinStateJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id; // WorkspaceId value

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DigitalTwinStateJpaEntity() {
    }

    public DigitalTwinStateJpaEntity(String id, Long version, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Id must not be null");
        this.version = Objects.requireNonNull(version, "Version must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");
    }

    public String getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

