package com.supplygraph.application.connector;

import com.supplygraph.domain.error.SecurityViolationException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OutboundUrlGuardTest {
    private final OutboundUrlGuard guard = new OutboundUrlGuard(false);

    @Test void blocksPrivateLoopbackLinkLocalAndMetadataAddresses() throws Exception {
        for (String ip : List.of("127.0.0.1", "10.0.0.5", "192.168.1.1", "172.16.0.9", "169.254.169.254", "0.0.0.0", "100.64.1.1", "::1", "fd00::1", "fe80::1")) {
            assertThat(guard.isForbidden(InetAddress.getByName(ip))).as(ip).isTrue();
        }
        assertThat(guard.isForbidden(InetAddress.getByName("93.184.216.34"))).isFalse();
    }

    @Test void loopbackOnlyAllowedWhenExplicitlyEnabled() throws Exception {
        assertThat(new OutboundUrlGuard(true).isForbidden(InetAddress.getByName("127.0.0.1"))).isFalse();
        assertThat(new OutboundUrlGuard(true).isForbidden(InetAddress.getByName("10.0.0.5"))).isTrue();
    }

    @Test void rejectsHostsOutsideAllowListSchemesAndEmbeddedCredentials() {
        assertThatThrownBy(() -> guard.check(URI.create("https://93.184.216.34/x"), List.of("example.com"))).hasMessageContaining("allow-list");
        assertThatThrownBy(() -> guard.check(URI.create("http://example.com/x"), List.of("example.com"))).hasMessageContaining("scheme");
        assertThatThrownBy(() -> guard.check(URI.create("https://u:p@example.com/x"), List.of("example.com"))).hasMessageContaining("Credentials");
        assertThatThrownBy(() -> guard.check(URI.create("https://127.0.0.1/x"), List.of("127.0.0.1"))).hasMessageContaining("publicly routable");
        assertThatThrownBy(() -> guard.check(URI.create("https://169.254.169.254/latest"), List.of("169.254.169.254"))).isInstanceOf(SecurityViolationException.class);
    }
}
