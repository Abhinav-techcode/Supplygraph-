package com.supplygraph.application.ports;

/**
 * Port interface for publishing application and domain events.
 * Encapsulates in-process Spring ApplicationEventPublisher or future message brokers.
 */
public interface EventPublisherPort {
    void publish(Object event);
}

