package com.supplygraph.domain.model.graph;

import java.util.Objects;

/**
 * Base abstract node in the SupplyGraph digital twin network.
 */
public abstract class SupplyChainNode {
    private final NodeId id;
    private final String name;
    private final NodeType type;
    private final String location;
    private boolean active;

    protected SupplyChainNode(NodeId id, String name, NodeType type, String location, boolean active) {
        this.id = Objects.requireNonNull(id, "NodeId must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Node name must not be blank");
        }
        this.type = Objects.requireNonNull(type, "NodeType must not be null");
        this.location = Objects.requireNonNull(location, "Location must not be null");
        this.active = active;
    }

    public NodeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplyChainNode that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", active=" + active +
                '}';
    }
}

