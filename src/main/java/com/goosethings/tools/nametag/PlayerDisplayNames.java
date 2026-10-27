package com.goosethings.tools.nametag;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.function.Function;

/** Optional server display provider. UUIDs and profile names remain the identity used by gameplay. */
public final class PlayerDisplayNames {
    private PlayerDisplayNames() {}
    public static String name(ServerPlayer player){return name(player.getUUID(),player.getGameProfile().name());}
    public static String name(UUID id,String original){
        Object shared=FabricLoader.getInstance().getObjectShare().get("goose:display_name_resolver");
        if(shared instanceof Function<?,?> function){
            @SuppressWarnings("unchecked") var resolver=(Function<UUID,String>)function;
            String display=resolver.apply(id);if(display!=null&&!display.isBlank())return display;
        }
        return original;
    }
}
