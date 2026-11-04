package com.goosethings.tools.xaero;
import com.goosethings.tools.GooseTools;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;
import java.util.*;

/** Exercises a real Xaero terrain map in a private training dimension and captures the GPU output. */
public final class TrainingMapRegression implements ClientModInitializer {
    boolean opened,saved,captured;int ticks,phase;
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    void tick(Minecraft mc){
        if(!mc.isGameLoadFinished())return;
        try{
            if(!opened){
                opened=true;mc.options.pauseOnLostFocus=false;
                org.lwjgl.sdl.SDLVideo.SDL_HideWindow(mc.getWindow().handle());
                mc.createWorldOpenFlows().createFreshLevel("training-map-"+System.currentTimeMillis(),
                    new LevelSettings("Training map",GameType.ADVENTURE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L,false,false),
                    p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||!mc.player.connection.hasClientLoaded()||!GgdServerContext.isGooseServer())return;
            ticks++;
            if(phase==0&&ticks>40){
                var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                server.execute(()->{
                    var key=ResourceKey.create(Registries.DIMENSION,Identifier.parse("ggd_training:poolcore"));var level=server.getLevel(key);
                    if(level==null)throw new AssertionError("test training dimension not registered");
                    for(int x=527>>4;x<=640>>4;x++)for(int z=575>>4;z<=703>>4;z++)level.getChunk(x,z);
                    var source=server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
                    server.getCommands().performPrefixedCommand(source,"fill 527 133 575 580 133 635 minecraft:yellow_concrete");
                    server.getCommands().performPrefixedCommand(source,"fill 581 133 575 640 133 635 minecraft:light_blue_concrete");
                    server.getCommands().performPrefixedCommand(source,"fill 527 133 636 580 133 703 minecraft:blue_concrete");
                    server.getCommands().performPrefixedCommand(source,"fill 581 133 636 640 133 703 minecraft:lime_concrete");
                    var player=server.getPlayerList().getPlayer(id);player.teleportTo(level,580,134,635,Set.of(),0,50,true);
                });phase=1;ticks=0;
            }else if(phase==1&&mc.level.dimension().identifier().getNamespace().equals("ggd_training")&&ticks>220){
                require(GgdMapState.isGameActive(mc),"training map was hidden by the match-only gate");
                require(GgdMapState.currentBounds(mc)==GameMapBounds.POOLCORE,"training map bounds missing");
                require(GgdMapState.isPoolcoreAdvanced(mc),"training PoolCore expansion missing");
                require(!GgdMapState.isMeetingView(mc),"training mistaken for a meeting view");
                if(!captured){captured=true;Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve("training-minimap.png"));saved=true;}catch(Exception e){throw new RuntimeException(e);}});}
                if(saved){
                    var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                    server.execute(()->server.getPlayerList().getPlayer(id).teleportTo(server.overworld(),0,64,0,Set.of(),0,0,true));
                    phase=2;ticks=0;
                }
            }else if(phase==2&&mc.level.dimension().identifier().toString().equals("minecraft:overworld")&&ticks>20){
                require(!GgdMapState.isGameActive(mc),"leaving training leaked an active match flag into the lobby");
                finish(mc,"PASS real training dimension: minimap terrain renders, PoolCore expansion enabled, no meeting context, lobby flag cleared; training-minimap.png");
            }
            if(ticks>1100)throw new AssertionError("training map client timeout phase="+phase);
        }catch(Throwable e){e.printStackTrace();finish(mc,"FAIL "+e);}
    }
    void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    void finish(Minecraft mc,String result){try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}mc.stop();}
}
