package checks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;

public final class IrisOutlineSmoke implements ClientModInitializer {
 boolean opened, setup, capturing, done; int ticks, stage;
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 void tick(Minecraft mc){
  if(done||capturing||!mc.isGameLoadFinished())return;
  try{
   if(!opened){opened=true;mc.options.pauseOnLostFocus=false;
    mc.createWorldOpenFlows().createFreshLevel("iris-"+System.currentTimeMillis(),new LevelSettings("Iris outline",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;}
   if(mc.level==null||mc.player==null||!mc.player.connection.hasClientLoaded())return;
   if(!setup){setup=true;
    var server=mc.getSingleplayerServer();server.execute(()->{
     var src=server.createCommandSourceStack();
     for(String cmd:new String[]{"time set 6000","weather clear","tp @a 0 -60 0 0 0","fill -5 -61 2 5 -61 9 smooth_stone","summon minecraft:block_display -2 -60 6 {Tags:[\"fixture\"],Glowing:1b,glow_color_override:16776960,block_state:{id:\"minecraft:gold_block\"},transformation:{translation:[0f,0f,0f],scale:[1f,2f,0.3f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}","summon minecraft:item 0 -59 6 {Tags:[\"fixture\"],Glowing:1b,NoGravity:1b,Item:{id:\"minecraft:player_head\",count:1},Age:-32768,PickupDelay:32767}","team add red","team modify red color red","team join red @e[type=item,tag=fixture]","summon minecraft:mannequin 2 -60 6 {Tags:[\"fixture\"],Glowing:1b,NoGravity:1b}","team add aqua","team modify aqua color aqua","team join aqua @e[type=mannequin,tag=fixture]"})server.getCommands().performPrefixedCommand(src,cmd);
    });return;}
   if(++ticks<100)return;ticks=0;capturing=true;
   Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve("stage-"+stage+".png"));}catch(Exception e){e.printStackTrace();}mc.execute(()->{
    try{
     if(stage++==0){com.goosethings.tools.client.shader.IrisCompat.enable(System.getProperty("goosetools.irisTestPack"));capturing=false;}
     else{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"PASS screenshots captured");done=true;mc.stop();}
    }catch(Throwable e){fail(mc,e);}
   });});
  }catch(Throwable e){fail(mc,e);}
 }
 void fail(Minecraft mc,Throwable e){e.printStackTrace();done=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"FAIL "+e);}catch(Exception ignored){}mc.stop();}
}
