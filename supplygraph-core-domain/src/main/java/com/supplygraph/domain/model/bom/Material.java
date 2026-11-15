package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.util.Objects;

/**
 * Material item in the SupplyGraph catalog.
 */
public class Material {
    private final MaterialId id;
    private final String sku;
    private final String name;
    private final MaterialType type;
    private final UnitOfMeasure baseUnit;

    public Material(MaterialId id, String sku, String name, MaterialType type, UnitOfMeasure baseUnit) {
        this.id = Objects.requireNonNull(id, "MaterialId must not be null");
        this.sku = Objects.requireNonNull(sku, "SKU must not be null");
        if (sku.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
        this.name = Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Material name must not be blank");
        }
        this.type = Objects.requireNonNull(type, "MaterialType must not be null");
        this.baseUnit = Objects.requireNonNull(baseUnit, "BaseUnit must not be null");
    }

    public MaterialId getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public MaterialType getType() {
        return type;
    }

    public UnitOfMeasure getBaseUnit() {
        return baseUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Material material)) return false;
        return Objects.equals(id, material.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Material{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", baseUnit=" + baseUnit +
                '}';
    }
}

