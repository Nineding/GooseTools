package com.goosethings.tools.client.appearance;

import com.goosethings.tools.appearance.LocalAppearancePolicy;
import com.goosethings.tools.client.nametag.NameTagClientState;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Local-only stolen-identity view. The nametag snapshot already carries
 * {@code identityPlayerId} while Morphling, Identity Thief, Parasite, or a
 * Seagull-borrowed Morphling is transformed; this class just exposes that UUID.
 */
public final class DisguiseViewClient {
    private DisguiseViewClient() {
    }

    public static UUID targetId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        UUID localId = minecraft.player.getUUID();
        return LocalAppearancePolicy.disguiseSkinSource(
                localId, NameTagClientState.identityPlayerId(localId));
    }
}
