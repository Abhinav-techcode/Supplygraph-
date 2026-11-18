package com.supplygraph.application.connector;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * SSRF defence for outbound connector calls: scheme, allow-listed host, and every resolved address must be public.
 * Known limitation: validation and connection resolve DNS separately (DNS rebinding window); mitigated by short timeouts,
 * no redirects, and the host allow-list. {@code allowLoopback} exists only for local integration-test servers.
 */
public class OutboundUrlGuard {
    private final boolean allowLoopback;

    public OutboundUrlGuard(boolean allowLoopback) { this.allowLoopback = allowLoopback; }

    public void check(URI uri, List<String> allowedHosts) {
        if (uri == null || uri.getHost() == null) throw new SecurityException("Outbound URL has no host");
        String scheme = uri.getScheme();
        if (!"https".equals(scheme) && !("http".equals(scheme) && allowLoopback)) {
            throw new SecurityException("Outbound scheme not permitted: " + scheme);
        }
        if (uri.getUserInfo() != null) throw new SecurityException("Credentials in URL are not permitted");
        if (!allowedHosts.contains(uri.getHost())) throw new SecurityException("Host not in allow-list: " + uri.getHost());
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new SecurityException("Host cannot be resolved: " + uri.getHost());
        }
        for (InetAddress a : addresses) {
            if (isForbidden(a)) throw new SecurityException("Resolved address is not publicly routable: " + a.getHostAddress());
        }
    }

    boolean isForbidden(InetAddress a) {
        if (a.isLoopbackAddress()) return !allowLoopback;
        if (a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress() || a.isMulticastAddress()) return true;
        byte[] b = a.getAddress();
        if (b.length == 16 && (b[0] & 0xFE) == 0xFC) return true;                       // IPv6 unique local fc00::/7
        if (b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64) return true;  // CGNAT 100.64.0.0/10
        return false;
    }
}
