package com.specpulse.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Component
public class OutboundUrlSecurity {

    private static final List<String> DEFAULT_BLOCKED_HOSTNAMES = List.of(
            "localhost",
            "ip6-localhost",
            "localtest.me",
            "example.invalid"
    );

    private final boolean blockPrivateAddresses;
    private final List<String> allowedHostPatterns;

    public OutboundUrlSecurity(
            @Value("${specpulse.outbound.block-private-addresses:true}") boolean blockPrivateAddresses,
            @Value("${specpulse.outbound.allowed-hosts:}") String allowedHostsCsv
    ) {
        this.blockPrivateAddresses = blockPrivateAddresses;
        this.allowedHostPatterns = Arrays.stream(allowedHostsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    public void validateOpenApiUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("OpenAPI URL is required");
        }

        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid OpenAPI URL format");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Invalid OpenAPI URL scheme. Only http/https are allowed");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid OpenAPI URL. Userinfo is not allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Invalid OpenAPI URL. Host is required");
        }

        host = host.toLowerCase();
        if (DEFAULT_BLOCKED_HOSTNAMES.contains(host) || host.endsWith(".localhost")) {
            throw new IllegalArgumentException("Invalid OpenAPI URL. Host is not allowed");
        }

        if (!allowedHostPatterns.isEmpty() && !matchesAllowedHost(host)) {
            throw new IllegalArgumentException("Invalid OpenAPI URL. Host is not in allowed list");
        }

        if (!blockPrivateAddresses) {
            return;
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OpenAPI URL host");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("Invalid OpenAPI URL. Host resolves to a blocked address");
            }
        }
    }

    private boolean matchesAllowedHost(String host) {
        for (String pattern : allowedHostPatterns) {
            String p = pattern.toLowerCase();
            if (p.equals(host)) {
                return true;
            }
            if (p.startsWith("*.") && host.endsWith(p.substring(1))) {
                // *.example.com => any subdomain of example.com
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedAddress(InetAddress address) {
        // Basic hard blocks
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        // IPv4: block RFC1918 + metadata range
        if (address instanceof Inet4Address ipv4) {
            int b0 = ipv4.getAddress()[0] & 0xff;
            int b1 = ipv4.getAddress()[1] & 0xff;

            // 10.0.0.0/8
            if (b0 == 10) {
                return true;
            }
            // 172.16.0.0/12
            if (b0 == 172 && b1 >= 16 && b1 <= 31) {
                return true;
            }
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) {
                return true;
            }
            // 169.254.0.0/16 (cloud metadata)
            if (b0 == 169 && b1 == 254) {
                return true;
            }
        }

        // IPv6: block unique-local and link-local
        if (address instanceof Inet6Address ipv6) {
            byte[] b = ipv6.getAddress();
            int firstByte = b[0] & 0xff;
            // fc00::/7 => unique-local address
            if ((firstByte & 0xfe) == 0xfc) {
                return true;
            }
        }

        // Generic: block site-local ranges (best-effort)
        return address.isSiteLocalAddress();
    }
}
