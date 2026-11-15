package com.supplygraph.domain.model.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable, high-precision quantity value object.
 * Enforces explicit scale (default 4) and prevents floating-point inaccuracy.
 */
public record Quantity(BigDecimal amount, UnitOfMeasure unit) implements Comparable<Quantity> {

    public static final int DEFAULT_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    public Quantity {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(unit, "Unit must not be null");
        amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static Quantity of(BigDecimal amount, UnitOfMeasure unit) {
        return new Quantity(amount, unit);
    }

    public static Quantity of(long amount, UnitOfMeasure unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public static Quantity of(String amount, UnitOfMeasure unit) {
        return new Quantity(new BigDecimal(amount), unit);
    }

    public static Quantity zero(UnitOfMeasure unit) {
        return new Quantity(BigDecimal.ZERO, unit);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public Quantity add(Quantity other) {
        Objects.requireNonNull(other, "Other quantity must not be null");
        Quantity normalizedOther = other.convertTo(this.unit);
        return new Quantity(this.amount.add(normalizedOther.amount), this.unit);
    }

    public Quantity subtract(Quantity other) {
        Objects.requireNonNull(other, "Other quantity must not be null");
        Quantity normalizedOther = other.convertTo(this.unit);
        return new Quantity(this.amount.subtract(normalizedOther.amount), this.unit);
    }

    public Quantity multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor must not be null");
        return new Quantity(this.amount.multiply(factor).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING), this.unit);
    }

    public Quantity divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor must not be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new Quantity(this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING), this.unit);
    }

    public Quantity convertTo(UnitOfMeasure targetUnit) {
        if (this.unit == targetUnit) {
            return this;
        }
        BigDecimal converted = this.unit.convertTo(this.amount, targetUnit, DEFAULT_SCALE, DEFAULT_ROUNDING);
        return new Quantity(converted, targetUnit);
    }

    @Override
    public int compareTo(Quantity other) {
        Objects.requireNonNull(other, "Cannot compare to null quantity");
        Quantity normalized = other.convertTo(this.unit);
        return this.amount.compareTo(normalized.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + unit.name();
    }
}

