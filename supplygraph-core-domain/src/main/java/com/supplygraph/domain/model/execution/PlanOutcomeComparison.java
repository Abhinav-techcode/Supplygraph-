package com.supplygraph.domain.model.execution;

import java.util.*;

/** Expected vs actual per metric. Missing actuals are UNKNOWN; simulated actuals are SIMULATED and excluded from deviation decisions. */
public record PlanOutcomeComparison(List<Row> rows) {
    public enum Status { UNKNOWN, MEASURED, SIMULATED }

    public record Row(OutcomeMetric metric, double expected, Double actual, Double delta, Double deltaPercent, Status status, String actualSource) {}

    public PlanOutcomeComparison { rows = List.copyOf(rows); }

    public static PlanOutcomeComparison compare(Map<OutcomeMetric, Double> expected, Collection<ActualMeasurement> actuals) {
        Map<OutcomeMetric, ActualMeasurement> latest = new EnumMap<>(OutcomeMetric.class);
        for (ActualMeasurement a : actuals) {
            ActualMeasurement cur = latest.get(a.metric());
            if (cur == null || a.recordedAt().isAfter(cur.recordedAt())) latest.put(a.metric(), a);
        }
        List<Row> rows = new ArrayList<>();
        for (OutcomeMetric m : OutcomeMetric.values()) {
            Double exp = expected.get(m);
            if (exp == null) continue;
            ActualMeasurement a = latest.get(m);
            if (a == null) { rows.add(new Row(m, exp, null, null, null, Status.UNKNOWN, null)); continue; }
            double delta = a.value() - exp;
            Double pct = exp == 0 ? null : delta / Math.abs(exp) * 100.0;
            rows.add(new Row(m, exp, a.value(), delta, pct, a.simulated() ? Status.SIMULATED : Status.MEASURED, a.source()));
        }
        return new PlanOutcomeComparison(rows);
    }

    /** Largest absolute percentage deviation among real (MEASURED) rows; empty when nothing real was measured. */
    public OptionalDouble maxMeasuredDeviationPercent() {
        return rows.stream().filter(r -> r.status() == Status.MEASURED && r.deltaPercent() != null).mapToDouble(r -> Math.abs(r.deltaPercent())).max();
    }

    public boolean isMaterial(double thresholdPercent) { return maxMeasuredDeviationPercent().orElse(0) >= thresholdPercent; }
}
