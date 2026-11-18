package com.supplygraph.domain.model.planning;

/**
 * Supported quantitative and collaborative demand forecasting strategies.
 */
public enum ForecastMethod {
    SIMPLE_MOVING_AVERAGE("Simple Moving Average (SMA)"),
    WEIGHTED_MOVING_AVERAGE("Weighted Moving Average (WMA)"),
    EXPONENTIAL_SMOOTHING("Single Exponential Smoothing (SES)"),
    TREND_ADJUSTED_HOLT_WINTERS("Trend-Adjusted Exponential Smoothing (Holt-Winters Additive)"),
    HISTORICAL_BASELINE("Direct Historical Period Baseline");

    private final String displayName;

    ForecastMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

