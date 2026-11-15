package com.supplygraph.adapter.ai;

import com.supplygraph.adapter.ai.exception.AiMalformedResponseException;
import com.supplygraph.adapter.ai.exception.AiProviderException;
import com.supplygraph.adapter.ai.exception.AiTimeoutException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic Fake AI Client for offline, reproducible integration testing.
 * Implements bounded proposal fixture generation without external network calls,
 * with full simulation support for timeouts, malformed responses, provider failures,
 * and retries.
 */
public class DeterministicFakeAiClient {

    public enum ExecutionMode {
        NORMAL,
        TIMEOUT,
        MALFORMED_OUTPUT,
        PROVIDER_OUTAGE,
        RETRY_WITH_FAILURES
    }

    private final String defaultModel;
    private volatile ExecutionMode mode = ExecutionMode.NORMAL;
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    private int failuresBeforeSuccess = 2;

    public DeterministicFakeAiClient(String defaultModel) {
        this.defaultModel = Objects.requireNonNull(defaultModel, "defaultModel must not be null");
    }

    public DeterministicFakeAiClient() {
        this("deterministic-fake-ai-v1");
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.failureCounter.set(0);
    }

    public void setFailuresBeforeSuccess(int count) {
        this.failuresBeforeSuccess = count;
    }

    /**
     * Generates a deterministic JSON payload or throws simulated network/provider exceptions.
     */
    public String generateFakeProposal(String scenarioCode) {
        switch (mode) {
            case TIMEOUT -> throw new AiTimeoutException("Simulated AI provider socket read timeout (60000ms exceeded)");
            case PROVIDER_OUTAGE -> throw new AiProviderException("Simulated 503 Service Unavailable: AI Provider upstream outage");
            case MALFORMED_OUTPUT -> {
                return "{ unclosed_json: true, invalid_tokens: [";
            }
            case RETRY_WITH_FAILURES -> {
                int attempts = failureCounter.incrementAndGet();
                if (attempts <= failuresBeforeSuccess) {
                    throw new AiProviderException("Transient failure attempt " + attempts + " of " + failuresBeforeSuccess);
                }
                return buildSuccessJson(scenarioCode);
            }
            case NORMAL -> {
                return buildSuccessJson(scenarioCode);
            }
            default -> throw new IllegalStateException("Unhandled mode: " + mode);
        }
    }

    /**
     * Executes prompt invocation wrapped in retry logic.
     */
    public String executeWithRetry(String scenarioCode, int maxAttempts) {
        int attempts = 0;
        Exception lastException = null;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                String response = generateFakeProposal(scenarioCode);
                // Validate JSON sanity
                if (!response.trim().startsWith("{") || !response.trim().endsWith("}")) {
                    throw new AiMalformedResponseException("Malformed JSON received from AI provider");
                }
                return response;
            } catch (AiProviderException | AiTimeoutException ex) {
                lastException = ex;
            }
        }
        throw new AiProviderException("Exhausted " + maxAttempts + " retry attempts. Last error: "
                + (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private String buildSuccessJson(String scenarioCode) {
        return """
                {
                  "scenarioCode": "%s",
                  "status": "PROPOSED",
                  "confidence": 0.95,
                  "rationale": "Deterministic test fixture response"
                }
                """.formatted(scenarioCode);
    }
}
