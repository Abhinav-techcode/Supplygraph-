package com.supplygraph.domain.model.execution;

public enum AttemptOutcome { STARTED, ACKNOWLEDGED, CONFIRMED, REJECTED, RETRYABLE_FAILURE, PERMANENT_FAILURE, UNKNOWN_OUTCOME }
