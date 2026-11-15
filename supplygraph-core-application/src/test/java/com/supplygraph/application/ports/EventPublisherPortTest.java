package com.supplygraph.application.ports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application Ports Contracts Test")
class EventPublisherPortTest {

    @Test
    @DisplayName("Should publish event through port interface")
    void shouldPublishEvent() {
        AtomicBoolean called = new AtomicBoolean(false);
        EventPublisherPort port = event -> called.set(true);

        port.publish("TestEvent");
        assertThat(called).isTrue();
    }
}

