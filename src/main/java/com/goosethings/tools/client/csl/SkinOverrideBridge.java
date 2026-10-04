package com.goosethings.tools.client.csl;

import com.mojang.authlib.GameProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants and marker checks for the optional CustomSkinLoader bridge. */
public final class SkinOverrideBridge {
    public static final String OVERRIDE_MARKER = "goosethings_csl_override";
    public static final Logger LOGGER = LoggerFactory.getLogger("goosetools/csl-bridge");

    private SkinOverrideBridge() {
    }

    public static boolean hasOverrideMarker(GameProfile profile) {
        return profile.properties() != null
                && !profile.properties().get(OVERRIDE_MARKER).isEmpty();
    }
}
