package com.goosethings.tools.client.hud;

import com.goosethings.tools.death.*;
import com.goosethings.tools.intro.RoleIntroPayload.Appearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;
import java.util.UUID;

/** Isolated GPU checks; no user save or live world is touched. */
final class DeathSceneRegression {
    private boolean world, done, captured;
    private int warmup, scenario;
    private long since;
    private static void apply(DeathScenePayload p) throws Exception {
        var m=DeathSceneHud.class.getDeclaredMethod("apply",DeathScenePayload.class);m.setAccessible(true);m.invoke(null,p);
    }
    private static Object active() throws Exception {
        var f=DeathSceneHud.class.getDeclaredField("active");f.setAccessible(true);return f.get(null);
    }
    void tick(Minecraft mc) {
        if(done||!mc.isGameLoadFinished())return;
        try {
            if(!world) {
                world=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);
                mc.createWorldOpenFlows().createFreshLevel("death-scene-"+System.currentTimeMillis(),
                    new LevelSettings("Death scene regression",GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.player==null||mc.level==null||!mc.player.connection.hasClientLoaded()||++warmup<40)return;
            if(scenario==DeathSceneKind.values().length+1) {
                apply(new DeathScenePayload(1000,DeathSceneKind.KNIFE,new Appearance(UUID.randomUUID(),"Other",false)));
                if(active()!=null)throw new AssertionError("other player received a scene");
                apply(new DeathScenePayload(1001,DeathSceneKind.KNIFE,new Appearance(mc.player.getUUID(),"Self",false)));
                var before=active();
                apply(new DeathScenePayload(999,DeathSceneKind.UNKNOWN,null));
                if(active()!=before)throw new AssertionError("stale packet replaced a scene");
                apply(new DeathScenePayload(1002,DeathSceneKind.UNKNOWN,null));
                if(active()!=null)throw new AssertionError("clear did not stop scene");
                finish(mc,"PASS all 17 causes rendered; reduced motion; automatic expiry; victim isolation; stale-event guard; clear.");return;
            }
            if(since==0) {
                mc.gui.setScreen(null);
                boolean reduced=scenario==DeathSceneKind.values().length;
                mc.options.screenEffectScale().set(reduced?0.0:1.0);
                mc.options.guiScale().set(scenario%2==0?2:3);mc.resizeGui();
                var kind=reduced?DeathSceneKind.SHERIFF:DeathSceneKind.values()[scenario];
                apply(new DeathScenePayload(scenario+1,kind,new Appearance(mc.player.getUUID(),mc.player.getGameProfile().name(),false)));
                since=System.nanoTime();captured=false;return;
            }
            double t=(System.nanoTime()-since)/1e9;
            if(!captured&&t>1.15) {
                captured=true;String file="death-scene-"+scenario+".png";
                Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),img->{try(img){img.writeToFile(mc.gameDirectory.toPath().resolve(file));}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot: "+e));}});
            }
            if(t>2.9){if(active()!=null)throw new AssertionError("scene did not expire");scenario++;since=0;}
        }catch(Throwable e){finish(mc,"FAIL: "+e);}
    }
    private void finish(Minecraft mc,String result){if(done)return;done=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("death-scene-result.txt"),result);Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception ignored){}com.goosethings.tools.GooseTools.LOGGER.info(result);mc.stop();}
}
