package com.goosethings.tools.client.csl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ArrayListMultimap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinOverrideBridgeTest {
    @Test
    void onlyMarkedProfilesBypassTheCslCache() {
        GameProfile unmarked = new GameProfile(UUID.randomUUID(), "OrdinaryPlayer");
        assertFalse(SkinOverrideBridge.hasOverrideMarker(unmarked));

        var propertyEntries = ArrayListMultimap.<String, Property>create();
        propertyEntries.put(
                SkinOverrideBridge.OVERRIDE_MARKER,
                new Property(SkinOverrideBridge.OVERRIDE_MARKER, "1"));
        PropertyMap properties = new PropertyMap(propertyEntries);
        GameProfile marked = new GameProfile(
                UUID.randomUUID(), "DisguisedPlayer", properties);

        assertTrue(SkinOverrideBridge.hasOverrideMarker(marked));
    }
}
