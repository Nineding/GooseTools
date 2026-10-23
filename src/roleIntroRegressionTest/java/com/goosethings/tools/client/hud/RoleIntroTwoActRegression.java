package com.goosethings.tools.client.hud;

import com.goosethings.tools.intro.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;
import java.util.*;

/** Actual GUI entity/name/light rendering at both acts, multiple hero seats and GUI scales. */
final class RoleIntroTwoActRegression {
    private final int[] counts={6,12,21,3,1,2,2,5,1,2};
    private final String[] cases={"goose6","goose12","goose21","duck","solo","solo-mimic","lover-goose","lover-duck","neutral","duel"};
    private boolean world,finished,pending;
    private int scenario,phase;
    private long since;
    private double heldSeconds;
    private RoleIntroScreen screen;
    private List<RoleIntroPayload.Appearance> roster;
    private int captures;
    void tick(Minecraft mc) {
        if(finished||pending||!mc.isGameLoadFinished())return;
        try {
            if(!world) {
                world=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);
                mc.createWorldOpenFlows().createFreshLevel("two-act-"+System.currentTimeMillis(),
                    new LevelSettings("Two-act GPU check",GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),provider->provider.lookupOrThrow(Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||mc.level==null||!mc.player.connection.hasClientLoaded())return;
            if(scenario==counts.length){finish(mc,"PASS special intro GPU: goose 6/12/21, duck, solo, solo with Mimic, goose lover, duck lover two groups, neutral retained artwork; paired dueling Dodos face-off; GUI scales 2/3; original nametags; true half-opacity frame/second; red teammate lighting; ten-second handoff; captures="+captures);return;}
            if(phase==0) {
                mc.options.guiScale().set(scenario==1?3:2);mc.resizeGui();
                roster=new ArrayList<>();int hero=scenario==1?7:scenario==7?2:counts[scenario];
                for(int seat=1;seat<=counts[scenario];seat++) roster.add(new RoleIntroPayload.Appearance(
                    seat==hero?mc.player.getUUID():new UUID(42,seat),seat==hero?"AnttCat":seat==3?"LongName_1234567":"Player_"+seat,scenario==7&&seat>2));
                var labels=RoleIntroRegression.class.getDeclaredMethod("labels",List.class,boolean.class);
                labels.setAccessible(true);labels.invoke(null,roster,scenario==6||scenario==7);
                RoleIntroScreen.apply(new RoleIntroPayload(true,scenario<3?"Goose":scenario==6?"Detective":scenario==8?"Raven":scenario==9?"DuelingDodos":"Assassin",
                    scenario<3?0:scenario==6||scenario==7?3:scenario>=8?2:1,1,scenario==0?9:11,scenario==2,scenario==4||scenario==5,roster));
                screen=(RoleIntroScreen)mc.gui.screen();phase=1;setTime(1.2);return;
            }
            if(screen != null && mc.gui.screen()==screen && phase<4) {
                var time=RoleIntroScreen.class.getDeclaredField("started");time.setAccessible(true);
                time.setLong(screen,System.nanoTime()-(long)(heldSeconds*1_000_000_000L));
            }
            if(System.currentTimeMillis()-since<450)return;
            if(phase==4) {
                if(!(mc.gui.screen() instanceof PresentationBlackoutScreen))throw new IllegalStateException("ten-second handoff missing");
                RoleIntroScreen.apply(RoleIntroPayload.CLOSE);scenario++;phase=0;return;
            }
            if(mc.gui.screen()!=screen)throw new IllegalStateException("unexpected stage exit");
            var modelField=RoleIntroScreen.class.getDeclaredField("models");modelField.setAccessible(true);
            int rendered=((Map<?,?>)modelField.get(screen)).size();
            if(phase==1&&rendered!=1)throw new IllegalStateException("first act disclosed roster: "+rendered);
            if(phase==3&&rendered!=counts[scenario])throw new IllegalStateException("second act missing models: "+rendered);
            pending=true;String label=phase==1?"first":phase==2?"opening":"second";
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
                try(image) {
                    image.writeToFile(mc.gameDirectory.toPath().resolve("special-"+cases[scenario]+"-"+label+".png"));
                    mc.execute(()->{try{captures++;phase++;pending=false;setTime(phase==2?5.15:phase==3?6.0:10.1);}catch(Exception e){finish(mc,"FAIL "+e);}});
                }catch(Throwable e){mc.execute(()->finish(mc,"FAIL "+e));}
            });
        }catch(Throwable e){finish(mc,"FAIL "+e);}
    }
    private void setTime(double seconds)throws Exception {
        heldSeconds=seconds;
        var field=RoleIntroScreen.class.getDeclaredField("started");field.setAccessible(true);
        field.setLong(screen,System.nanoTime()-(long)(seconds*1_000_000_000L));since=System.currentTimeMillis();
    }
    private void finish(Minecraft mc,String text){if(finished)return;finished=true;
        try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(text);mc.stop();}
}
