package com.supplygraph.domain.model.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Standard Units of Measure with explicit, deterministic conversion logic.
 * Authoritative measurements in SupplyGraph never use floating-point types.
 */
public enum UnitOfMeasure {
    // Mass units (base: GRAM)
    MILLIGRAM(Dimension.MASS, new BigDecimal("0.001")),
    GRAM(Dimension.MASS, BigDecimal.ONE),
    KILOGRAM(Dimension.MASS, new BigDecimal("1000")),
    METRIC_TON(Dimension.MASS, new BigDecimal("1000000")),

    // Volume units (base: MILLILITER)
    MILLILITER(Dimension.VOLUME, BigDecimal.ONE),
    LITER(Dimension.VOLUME, new BigDecimal("1000")),

    // Discrete counts (base: PIECE)
    PIECE(Dimension.COUNT, BigDecimal.ONE),
    DOZEN(Dimension.COUNT, new BigDecimal("12")),
    BOX(Dimension.COUNT, new BigDecimal("100")),
    WAFER(Dimension.COUNT, BigDecimal.ONE),

    // Length units (base: MILLIMETER)
    MILLIMETER(Dimension.LENGTH, BigDecimal.ONE),
    METER(Dimension.LENGTH, new BigDecimal("1000")),

    // Time units (base: SECOND)
    SECOND(Dimension.TIME, BigDecimal.ONE),
    MINUTE(Dimension.TIME, new BigDecimal("60")),
    HOUR(Dimension.TIME, new BigDecimal("3600")),
    DAY(Dimension.TIME, new BigDecimal("86400"));

    public enum Dimension {
        MASS, VOLUME, COUNT, LENGTH, TIME
    }

    private final Dimension dimension;
    private final BigDecimal factorToBase;

    UnitOfMeasure(Dimension dimension, BigDecimal factorToBase) {
        this.dimension = Objects.requireNonNull(dimension, "Dimension must not be null");
        this.factorToBase = Objects.requireNonNull(factorToBase, "Factor must not be null");
    }

    public Dimension getDimension() {
        return dimension;
    }

    public BigDecimal getFactorToBase() {
        return factorToBase;
    }

    public boolean isCompatibleWith(UnitOfMeasure target) {
        return target != null && this.dimension == target.dimension;
    }

    /**
     * Converts a quantity value from this unit to the target unit.
     *
     * @param value the numeric value in this unit
     * @param target the target unit of measure
     * @param scale calculation scale
     * @param roundingMode rounding mode
     * @return converted value
     */
    public BigDecimal convertTo(BigDecimal value, UnitOfMeasure target, int scale, RoundingMode roundingMode) {
        Objects.requireNonNull(value, "Value must not be null");
        Objects.requireNonNull(target, "Target unit must not be null");
        if (!isCompatibleWith(target)) {
            throw new IllegalArgumentException(
                    "Cannot convert between incompatible dimensions: " + this.dimension + " and " + target.dimension);
        }
        if (this == target) {
            return value.setScale(scale, roundingMode);
        }
        // value_in_base = value * factorToBase
        BigDecimal valueInBase = value.multiply(this.factorToBase);
        // value_in_target = value_in_base / target.factorToBase
        return valueInBase.divide(target.factorToBase, scale, roundingMode);
    }
}

