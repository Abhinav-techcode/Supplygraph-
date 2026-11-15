package com.supplygraph.domain.model.bom;

import com.supplygraph.domain.model.common.Quantity;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Single BOM relationship linking a parent material to a required component material.
 */
public record BomEntry(
        MaterialId parentMaterialId,
        MaterialId childMaterialId,
        Quantity quantityPerParent,
        BigDecimal scrapRate,
        int position
) {
    public BomEntry {
        Objects.requireNonNull(parentMaterialId, "Parent material ID must not be null");
        Objects.requireNonNull(childMaterialId, "Child material ID must not be null");
        if (parentMaterialId.equals(childMaterialId)) {
            throw new IllegalArgumentException("A material cannot be a component of itself (self-cycle)");
        }
        Objects.requireNonNull(quantityPerParent, "Quantity per parent must not be null");
        if (!quantityPerParent.isPositive()) {
            throw new IllegalArgumentException("Quantity per parent must be strictly positive");
        }
        if (scrapRate == null) {
            scrapRate = BigDecimal.ZERO;
        }
        if (scrapRate.compareTo(BigDecimal.ZERO) < 0 || scrapRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Scrap rate must be in range [0.0, 1.0)");
        }
        if (position < 1) {
            throw new IllegalArgumentException("Position must be >= 1");
        }
    }

    public static BomEntry of(MaterialId parent, MaterialId child, Quantity qty, int position) {
        return new BomEntry(parent, child, qty, BigDecimal.ZERO, position);
    }

    public static BomEntry of(MaterialId parent, MaterialId child, Quantity qty, BigDecimal scrapRate, int position) {
        return new BomEntry(parent, child, qty, scrapRate, position);
    }

    /**
     * Calculates the gross quantity required for a given parent net quantity, factoring in scrap rate.
     * Gross = (ParentQuantity * quantityPerParent) * (1 + scrapRate)
     */
    public Quantity calculateGrossRequired(Quantity parentQuantity) {
        Objects.requireNonNull(parentQuantity, "Parent quantity must not be null");
        BigDecimal netAmount = parentQuantity.amount().multiply(quantityPerParent.amount());
        BigDecimal scrapMultiplier = BigDecimal.ONE.add(scrapRate);
        BigDecimal grossAmount = netAmount.multiply(scrapMultiplier);
        return Quantity.of(grossAmount, quantityPerParent.unit());
    }
}

