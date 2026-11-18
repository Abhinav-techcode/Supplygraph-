package com.supplygraph.domain.model.connector;

import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ConnectorDomainTest {
    private static ConnectorConfiguration sandboxCfg() { return new ConnectorConfiguration(null, null, List.of(), 1000, 1024, 3); }
    private static Connector reg(ConnectorMode m) { return Connector.register(WorkspaceId.of("ws"), "ERP", ConnectorCategory.ERP, m, "sandbox"); }

    @Test void credentialReferenceRejectsRawSecrets() {
        assertThatThrownBy(() -> new CredentialReference("sk-live-abc123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CredentialReference("password=hunter2")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new CredentialReference("env:ERP_TOKEN").value()).isEqualTo("env:ERP_TOKEN");
    }

    @Test void credentialToStringDoesNotLeakAnythingBeyondReference() {
        assertThat(new CredentialReference("vault:erp/token").toString()).doesNotContain("secret");
    }

    @Test void lifecycleRegisterConfigureActivateDeactivate() {
        Connector c = reg(ConnectorMode.SANDBOX_SIMULATED);
        assertThat(c.isSimulated()).isTrue();
        c = c.configure(sandboxCfg()).activate();
        assertThat(c.status()).isEqualTo(ConnectorStatus.ACTIVE);
        assertThat(c.deactivate().status()).isEqualTo(ConnectorStatus.INACTIVE);
        assertThat(c.version()).isEqualTo(2);
    }

    @Test void cannotActivateBeforeConfiguration() {
        assertThatThrownBy(() -> reg(ConnectorMode.SANDBOX_SIMULATED).activate()).isInstanceOf(IllegalStateException.class);
    }

    @Test void cannotDeactivateRegisteredConnector() {
        assertThatThrownBy(() -> reg(ConnectorMode.SANDBOX_SIMULATED).deactivate()).isInstanceOf(IllegalStateException.class);
    }

    @Test void liveConnectorRequiresUriAndCredential() {
        assertThatThrownBy(() -> reg(ConnectorMode.LIVE).configure(sandboxCfg())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void configurationEnforcesAllowedHostsAndBounds() {
        CredentialReference ref = new CredentialReference("env:X");
        assertThatThrownBy(() -> new ConnectorConfiguration(URI.create("https://evil.example"), ref, List.of("erp.example"), 1000, 1024, 1))
                .hasMessageContaining("allowed host");
        assertThatThrownBy(() -> new ConnectorConfiguration(URI.create("https://u:p@erp.example"), ref, List.of("erp.example"), 1000, 1024, 1))
                .hasMessageContaining("credentials");
        assertThatThrownBy(() -> new ConnectorConfiguration(URI.create("ftp://erp.example"), ref, List.of("erp.example"), 1000, 1024, 1))
                .hasMessageContaining("scheme");
        assertThatThrownBy(() -> new ConnectorConfiguration(null, null, List.of(), 5, 1024, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ConnectorConfiguration(URI.create("https://erp.example/api"), ref, List.of("erp.example"), 1000, 1024, 1)).isNotNull();
    }
}
