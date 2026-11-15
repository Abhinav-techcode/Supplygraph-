package com.supplygraph.domain.model.capacity;

import com.supplygraph.domain.model.graph.NodeId;

import java.util.Objects;

/**
 * Processing resource (machine, production line, cleanroom, inspection bay) within a facility.
 */
public class Resource {
    private final ResourceId id;
    private final String name;
    private final NodeId facilityId;
    private final CapacityCalendar calendar;

    public Resource(ResourceId id, String name, NodeId facilityId, CapacityCalendar calendar) {
        this.id = Objects.requireNonNull(id, "ResourceId must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Resource name must not be blank");
        }
        this.facilityId = Objects.requireNonNull(facilityId, "FacilityId must not be null");
        this.calendar = Objects.requireNonNull(calendar, "Calendar must not be null");
    }

    public ResourceId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeId getFacilityId() {
        return facilityId;
    }

    public CapacityCalendar getCalendar() {
        return calendar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource resource)) return false;
        return Objects.equals(id, resource.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", facilityId=" + facilityId +
                '}';
    }
}

