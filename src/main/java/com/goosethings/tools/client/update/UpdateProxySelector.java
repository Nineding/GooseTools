package com.goosethings.tools.client.update;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Honors existing Java proxy settings first, then standard HTTP proxy environment variables. */
final class UpdateProxySelector extends ProxySelector {
    private final Proxy proxy;
    private final String bypass;

    private UpdateProxySelector(Proxy proxy, String bypass) {
        this.proxy = proxy;
        this.bypass = bypass;
    }

    static ProxySelector configured(Map<String, String> environment, ProxySelector fallback) {
        // Java's explicit JVM proxy arguments take precedence over inherited environment settings.
        if (System.getProperty("https.proxyHost") != null || System.getProperty("http.proxyHost") != null) return fallback;
        String serialized = first(environment, "HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy");
        if (serialized == null || serialized.isBlank()) return fallback;
        try {
            URI uri = URI.create(serialized.contains("://") ? serialized : "http://" + serialized);
            // java.net.http supports HTTP CONNECT proxies. Do not silently send credentials in a URL.
            if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() <= 0)) return fallback;
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort() == -1 ? 80 : uri.getPort()));
            return new UpdateProxySelector(proxy, first(environment, "NO_PROXY", "no_proxy"));
        } catch (IllegalArgumentException invalid) { return fallback; }
    }

    private static String first(Map<String, String> environment, String... names) {
        for (String name : names) {
            String value = environment.get(name);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (bypass != null) {
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            for (String token : bypass.split(",")) {
                String domain = token.strip().toLowerCase(java.util.Locale.ROOT).replaceFirst("^\\.", "");
                if (domain.equals("*") || host.equals(domain) || host.endsWith("." + domain)) {
                    return List.of(Proxy.NO_PROXY);
                }
            }
        }
        return List.of(proxy);
    }

    @Override public void connectFailed(URI uri, SocketAddress address, IOException failure) { }
}
