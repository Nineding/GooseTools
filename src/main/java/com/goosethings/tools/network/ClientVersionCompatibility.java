package com.goosethings.tools.network;

/** Explicit, directional release compatibility; the protocol is checked separately. */
public final class ClientVersionCompatibility {
    private ClientVersionCompatibility() { }

    public static boolean allows(String serverVersion, String clientVersion) {
        String server = normalize(serverVersion);
        String client = normalize(clientVersion);
        if (server.isEmpty() || client.isEmpty()) return false;
        return server.equals(client)
                || (server.equals("1.15.1") && client.equals("1.15.0"));
    }

    private static String normalize(String version) {
        return version == null ? "" : version.trim();
    }
}
