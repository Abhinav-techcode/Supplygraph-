package com.supplygraph.adapter.integration;

import com.supplygraph.application.connector.ConnectorAdapter;
import com.supplygraph.application.connector.ConnectorCallResult;
import com.supplygraph.application.connector.ExternalRecord;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ErrorCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SIMULATED adapter. Performs no network I/O and produces a fixed, deterministic result; it is evidence of nothing
 * about any real enterprise system. Refuses to serve LIVE connectors.
 */
@Component
public class SandboxConnectorAdapter implements ConnectorAdapter {
    public static final String TYPE = "sandbox";
    public static final int SIMULATED_RECORDS = 5;

    @Override public String adapterType() { return TYPE; }

    @Override public ConnectorCallResult testConnectivity(Connector c) {
        if (!c.isSimulated()) return ConnectorCallResult.failure(ErrorCategory.POLICY_BLOCKED, "Sandbox adapter cannot serve LIVE connectors", false);
        return ConnectorCallResult.ok(0, 0, 0, "[SIMULATED] sandbox connectivity OK");
    }

    @Override public ConnectorCallResult synchronize(Connector c) {
        if (!c.isSimulated()) return ConnectorCallResult.failure(ErrorCategory.POLICY_BLOCKED, "Sandbox adapter cannot serve LIVE connectors", false);
        List<ExternalRecord> records = new ArrayList<>();
        for (int i = 1; i <= SIMULATED_RECORDS; i++) {
            records.add(new ExternalRecord("SIM-SHP-" + i, "SHIPMENT", Map.of("milestone", "IN_TRANSIT", "delayHours", Integer.toString(i * 3))));
        }
        return ConnectorCallResult.ok(SIMULATED_RECORDS, SIMULATED_RECORDS, 0, "[SIMULATED] " + SIMULATED_RECORDS + " synthetic records", records);
    }
}
