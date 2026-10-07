package com.goosethings.tools.client.update;

import org.junit.jupiter.api.Test;
import java.net.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UpdateProxySelectorTest {
    @Test void usesConfiguredHttpConnectProxyWithoutChangingGlobalSettings() {
        ProxySelector original = ProxySelector.getDefault();
        var selector = UpdateProxySelector.configured(Map.of("HTTPS_PROXY", "http://127.0.0.1:10808"), original);
        Proxy proxy = selector.select(URI.create("https://github.com/file")).getFirst();
        assertEquals(Proxy.Type.HTTP, proxy.type());
        assertEquals(10808, ((InetSocketAddress) proxy.address()).getPort());
        assertSame(original, ProxySelector.getDefault());
    }

    @Test void respectsNoProxyAndRejectsUnsupportedOrAuthenticatedProxyUrls() {
        var selector = UpdateProxySelector.configured(Map.of("https_proxy", "http://127.0.0.1:10808",
                "no_proxy", ".github.com,localhost"), null);
        assertEquals(Proxy.NO_PROXY, selector.select(URI.create("https://api.github.com/releases")).getFirst());
        assertEquals(Proxy.Type.HTTP, selector.select(URI.create("https://release-assets.githubusercontent.com/file")).getFirst().type());
        assertNull(UpdateProxySelector.configured(Map.of("HTTPS_PROXY", "socks5://127.0.0.1:10808"), null));
        assertNull(UpdateProxySelector.configured(Map.of("HTTPS_PROXY", "http://user:secret@127.0.0.1:10808"), null));
    }
}
