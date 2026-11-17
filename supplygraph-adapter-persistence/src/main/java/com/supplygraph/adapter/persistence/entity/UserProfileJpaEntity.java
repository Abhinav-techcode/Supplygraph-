package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfileJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "username", length = 64, unique = true, nullable = false)
    private String username;

    @Column(name = "email", length = 128, nullable = false)
    private String email;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(name = "role", length = 32, nullable = false)
    private String role;

    @Column(name = "assigned_workspaces", length = 1024, nullable = false)
    private String assignedWorkspaces;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserProfileJpaEntity() {}

    public UserProfileJpaEntity(
            String id,
            String username,
            String email,
            String displayName,
            String role,
            String assignedWorkspaces,
            boolean active,
            Instant createdAt
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.assignedWorkspaces = assignedWorkspaces;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getAssignedWorkspaces() { return assignedWorkspaces; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRole(String role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setAssignedWorkspaces(String assignedWorkspaces) { this.assignedWorkspaces = assignedWorkspaces; }
}

