package com.supplygraph.domain.model.connector;

/**
 * SANDBOX_SIMULATED connectors never talk to a real enterprise system; every record they produce is simulated.
 * LIVE connectors call an external endpoint through a real adapter.
 */
public enum ConnectorMode { SANDBOX_SIMULATED, LIVE }
