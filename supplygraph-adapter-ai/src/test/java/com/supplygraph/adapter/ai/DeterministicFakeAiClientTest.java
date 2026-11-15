package com.supplygraph.adapter.ai;

import com.supplygraph.adapter.ai.exception.AiMalformedResponseException;
import com.supplygraph.adapter.ai.exception.AiProviderException;
import com.supplygraph.adapter.ai.exception.AiTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deterministic Fake AI Client Tests")
class DeterministicFakeAiClientTest {

    private DeterministicFakeAiClient client;

    @BeforeEach
    void setUp() {
        client = new DeterministicFakeAiClient();
    }

    @Test
    @DisplayName("Should generate deterministic fake proposal fixture in normal mode")
    void shouldGenerateFakeProposal() {
        assertThat(client.getDefaultModel()).isEqualTo("deterministic-fake-ai-v1");

        String json = client.generateFakeProposal("EV-SC-01");
        assertThat(json)
                .contains("\"scenarioCode\": \"EV-SC-01\"")
                .contains("\"status\": \"PROPOSED\"")
                .contains("\"confidence\": 0.95");
    }

    @Test
    @DisplayName("Failure Simulation: Should throw AiTimeoutException when configured for timeout")
    void shouldThrowAiTimeoutException() {
        client.setMode(DeterministicFakeAiClient.ExecutionMode.TIMEOUT);

        assertThatThrownBy(() -> client.generateFakeProposal("EV-SC-01"))
                .isInstanceOf(AiTimeoutException.class)
                .hasMessageContaining("socket read timeout");
    }

    @Test
    @DisplayName("Failure Simulation: Should throw AiProviderException on provider outage")
    void shouldThrowAiProviderException() {
        client.setMode(DeterministicFakeAiClient.ExecutionMode.PROVIDER_OUTAGE);

        assertThatThrownBy(() -> client.generateFakeProposal("PHARMA-SC-02"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("503 Service Unavailable");
    }

    @Test
    @DisplayName("Failure Simulation: Should detect and throw AiMalformedResponseException on malformed output")
    void shouldDetectMalformedResponse() {
        client.setMode(DeterministicFakeAiClient.ExecutionMode.MALFORMED_OUTPUT);

        assertThatThrownBy(() -> client.executeWithRetry("SEMI-SC-03", 2))
                .isInstanceOf(AiMalformedResponseException.class)
                .hasMessageContaining("Malformed JSON");
    }

    @Test
    @DisplayName("Resilience: Should successfully retry and recover after transient provider failures")
    void shouldRetryAndSucceedAfterTransientFailures() {
        client.setMode(DeterministicFakeAiClient.ExecutionMode.RETRY_WITH_FAILURES);
        client.setFailuresBeforeSuccess(2); // Fails twice, succeeds on attempt 3

        String json = client.executeWithRetry("AUTO-SC-04", 4);
        assertThat(json)
                .contains("\"scenarioCode\": \"AUTO-SC-04\"")
                .contains("\"status\": \"PROPOSED\"");
    }

    @Test
    @DisplayName("Resilience: Should exhaust retries when failures exceed max attempts")
    void shouldExhaustRetriesWhenOutagePersists() {
        client.setMode(DeterministicFakeAiClient.ExecutionMode.RETRY_WITH_FAILURES);
        client.setFailuresBeforeSuccess(5); // Fails 5 times, but max attempts is 3

        assertThatThrownBy(() -> client.executeWithRetry("AUTO-SC-04", 3))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Exhausted 3 retry attempts");
    }
}
