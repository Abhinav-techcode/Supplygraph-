package com.supplygraph.application.connector;

import com.supplygraph.domain.model.connector.Connector;

/** Port implemented by vendor/protocol adapters. Implementations must never throw for remote failures; they return a failure result. */
public interface ConnectorAdapter {
    /** Matches {@link Connector#adapterType()}. */
    String adapterType();
    ConnectorCallResult testConnectivity(Connector connector);
    ConnectorCallResult synchronize(Connector connector);
}
