package com.supplygraph.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitOfMeasureAndQuantityTest {

    @Test
    @DisplayName("Unit of measure converts correctly within the same dimension")
    void shouldConvertWithinSameDimension() {
        // 2.5 KG to GRAM -> 2500.0000 GRAM
        BigDecimal kgValue = new BigDecimal("2.5");
        BigDecimal gramValue = UnitOfMeasure.KILOGRAM.convertTo(kgValue, UnitOfMeasure.GRAM, 4, RoundingMode.HALF_UP);
        assertThat(gramValue).isEqualByComparingTo("2500.0000");

        // 500 GRAM to KG -> 0.5000 KG
        BigDecimal backToKg = UnitOfMeasure.GRAM.convertTo(new BigDecimal("500"), UnitOfMeasure.KILOGRAM, 4, RoundingMode.HALF_UP);
        assertThat(backToKg).isEqualByComparingTo("0.5000");

        // 1 METRIC_TON to KG -> 1000.0000 KG
        BigDecimal tonToKg = UnitOfMeasure.METRIC_TON.convertTo(BigDecimal.ONE, UnitOfMeasure.KILOGRAM, 4, RoundingMode.HALF_UP);
        assertThat(tonToKg).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("Incompatible dimension conversions throw IllegalArgumentException")
    void shouldRejectCrossDimensionConversion() {
        assertThatThrownBy(() -> UnitOfMeasure.KILOGRAM.convertTo(BigDecimal.TEN, UnitOfMeasure.LITER, 4, RoundingMode.HALF_UP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert between incompatible dimensions");
    }

    @Test
    @DisplayName("Quantity preserves 4 decimal places and performs exact arithmetic")
    void shouldPerformExactArithmetic() {
        Quantity q1 = Quantity.of("10.5", UnitOfMeasure.KILOGRAM);
        Quantity q2 = Quantity.of("500", UnitOfMeasure.GRAM); // 0.5000 KG

        Quantity sum = q1.add(q2);
        assertThat(sum.amount()).isEqualByComparingTo("11.0000");
        assertThat(sum.unit()).isEqualTo(UnitOfMeasure.KILOGRAM);

        Quantity diff = q1.subtract(q2);
        assertThat(diff.amount()).isEqualByComparingTo("10.0000");

        Quantity multiplied = q1.multiply(new BigDecimal("2.5"));
        assertThat(multiplied.amount()).isEqualByComparingTo("26.2500");

        Quantity divided = q1.divide(new BigDecimal("2"));
        assertThat(divided.amount()).isEqualByComparingTo("5.2500");
    }

    @Test
    @DisplayName("Quantity comparison compares normalized amounts across units")
    void shouldCompareQuantitiesAccurately() {
        Quantity oneKg = Quantity.of("1.0", UnitOfMeasure.KILOGRAM);
        Quantity thousandGrams = Quantity.of("1000.0", UnitOfMeasure.GRAM);
        Quantity fiveHundredGrams = Quantity.of("500.0", UnitOfMeasure.GRAM);

        assertThat(oneKg).isEqualByComparingTo(thousandGrams);
        assertThat(oneKg).isGreaterThan(fiveHundredGrams);
        assertThat(fiveHundredGrams).isLessThan(oneKg);
    }
}

