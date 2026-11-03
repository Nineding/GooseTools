package com.goosethings.tools.client.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;
import java.util.UUID;

/** Loads the actual client mixin on a vanilla remote player; never shipped. */
final class TrainingClientRegression {
    private boolean world,done;
    void tick(Minecraft mc) {
        if(done||!mc.isGameLoadFinished())return;
        try {
            if(!world) {
                world=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("training-client-"+System.currentTimeMillis(),
                    new LevelSettings("Training client smoke",GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),provider->provider.lookupOrThrow(Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.level==null||mc.player==null||!mc.player.connection.hasClientLoaded())return;
            var target=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"GT_NPC_Proof"));
            var expected=new PlayerSkin(new ClientAsset.ResourceTexture(
                Identifier.parse("minecraft:entity/player/slim/unknow"),
                Identifier.parse("minecraft:textures/entity/player/slim/unknow.png")),
                null,null,PlayerModelType.SLIM,false);
            if(!expected.equals(target.getSkin()))throw new AssertionError("training NPC skin mixin did not apply");
            if(mc.getResourceManager().getResource(Identifier.parse("minecraft:textures/entity/player/slim/unknow.png")).isEmpty())
                throw new AssertionError("supplied unknow texture was not loaded");
            finish(mc,"PASS actual remote-player skin mixin; local packaged unknow texture; slim model; dedicated-server side already checked");
        }catch(Throwable error){finish(mc,"FAIL "+error);}
    }
    private void finish(Minecraft mc,String result) {
        done=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(result);mc.stop();
    }
}
