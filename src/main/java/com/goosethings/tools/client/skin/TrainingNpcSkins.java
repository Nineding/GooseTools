package com.goosethings.tools.client.skin;
import com.mojang.authlib.GameProfile;
import com.goosethings.tools.client.csl.SkinOverrideBridge;
/** A viewer-specific disguise or Astral profile takes priority over the offline NPC fallback. */
public final class TrainingNpcSkins {
    public static final String PLACEHOLDER="eyJ0ZXh0dXJlcyI6e30sImdvb3NldG9vbHNfdHJhaW5pbmdfbnBjIjp0cnVlfQ==";
    private TrainingNpcSkins(){}
    public static boolean packaged(GameProfile profile){
        var textures=profile.properties().get("textures");
        if(SkinOverrideBridge.hasOverrideMarker(profile)&&textures.stream().anyMatch(p->!p.value().equals(PLACEHOLDER)))return false;
        return profile.name().startsWith("GT_NPC_")||textures.stream().anyMatch(p->p.value().equals(PLACEHOLDER));
    }
}
