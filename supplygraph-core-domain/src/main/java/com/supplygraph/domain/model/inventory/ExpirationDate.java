package com.supplygraph.domain.model.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

public record ExpirationDate(LocalDate date) implements Comparable<ExpirationDate> {
    public ExpirationDate {
        Objects.requireNonNull(date, "ExpirationDate must not be null");
    }

    public static ExpirationDate of(LocalDate date) {
        return new ExpirationDate(date);
    }

    public static ExpirationDate of(String isoDate) {
        return new ExpirationDate(LocalDate.parse(isoDate));
    }

    public boolean isExpiredAsOf(LocalDate referenceDate) {
        return date.isBefore(referenceDate);
    }

    public boolean isExpiredAsOf(Instant referenceInstant) {
        return date.isBefore(referenceInstant.atZone(ZoneOffset.UTC).toLocalDate());
    }

    @Override
    public int compareTo(ExpirationDate o) {
        return this.date.compareTo(o.date);
    }
}

