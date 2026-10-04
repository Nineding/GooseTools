package com.goosethings.tools.client.web;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/** Exact allowlist for official websites opened from server-provided Web UI pages. */
public final class OfficialWebLinks {
    public static final String ROLE_GUIDE = "https://goose.worldrealms.top/guide";
    public static final String GAMEPLAY_GUIDE = "https://goose.worldrealms.top/guide/gameplay";
    public static final String MODES_GUIDE = "https://goose.worldrealms.top/guide/modes";
    public static final String OFFICIAL_SITE = "https://goose.worldrealms.top/";

    private static final Map<String, URI> ALLOWED = Map.of(
            ROLE_GUIDE, URI.create(ROLE_GUIDE),
            GAMEPLAY_GUIDE, URI.create(GAMEPLAY_GUIDE),
            MODES_GUIDE, URI.create(MODES_GUIDE),
            OFFICIAL_SITE, URI.create(OFFICIAL_SITE));

    private OfficialWebLinks() {
    }

    public static Optional<URI> resolve(String href) {
        if (href == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALLOWED.get(href));
    }
}
