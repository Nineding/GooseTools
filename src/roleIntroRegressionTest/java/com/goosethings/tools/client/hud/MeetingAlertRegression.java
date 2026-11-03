package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.presence.GamePresenceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/** Real packet delivery, F1 rendering, late meeting/chunks, and report/bell GPU checks. */
final class MeetingAlertRegression {
    private int stage, warmup, captures;
    private long entered=System.nanoTime();
    private boolean finished;
    private volatile CompletableFuture<Void> pending;
    private static long now() { return System.nanoTime(); }

    void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            require(now()-entered < 75_000_000_000L, "timed out at stage " + stage);
            if (stage==0) {
                stage=1; mc.options.pauseOnLostFocus=false; mc.options.renderDistance().set(2);
                mc.options.guiScale().set(2); mc.resizeGui();
                mc.createWorldOpenFlows().createFreshLevel("blackout-"+System.currentTimeMillis(),
                        new LevelSettings("Meeting blackout regression",GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42,false,false), p -> p.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null
                    ||!mc.player.connection.hasClientLoaded()||++warmup<40)return;
            if(pending!=null) { if(!pending.isDone())return;pending.join();pending=null; }
            switch(stage) {
                case 1 -> { commands(mc,"scoreboard objectives add ggdSession dummy","scoreboard players set #MeetingPhase ggdSession 0",
                        "tag @a add gamingGGD","gamerule minecraft:advance_time false","time set day");next(); }
                case 2 -> { if(!GamePresenceClient.phaseId().equals("playing"))return;
                    commands(mc,"execute as @p at @s run goosetools meeting-alert bell");next(); }
                case 3 -> { if(elapsed()<200_000_000L)return;
                    require(MeetingAlertHud.blocksMovement(),"entrance did not own movement");
                    capture(mc,"blackout-bell-entry.png",true);next(); }
                case 4 -> { if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();next(); }
                case 5 -> { if(elapsed()<900_000_000L)return;capture(mc,"blackout-f1.png",true);next(); }
                case 6 -> { if(mc.gui.hud.isHidden())mc.gui.hud.toggle();if(elapsed()<3_300_000_000L)return;
                    require(MeetingAlertHud.blocksMovement(),"late meeting exposed map after banner");
                    capture(mc,"blackout-wait.png",true);next(); }
                case 7 -> { commands(mc,"fill 144 -61 -16 176 -61 16 minecraft:gold_block",
                        "tp @p 160 -60 0 0 0","scoreboard players set #MeetingPhase ggdSession 1");next(); }
                case 8 -> { long reveal=revealStarted();if(reveal<0||elapsed()-reveal<160_000_000L)return;
                    capture(mc,"blackout-reveal.png",false);next(); }
                case 9 -> { if(MeetingAlertHud.blocksMovement())return;
                    require(mc.player.getX()>150,"did not arrive at meeting destination");
                    capture(mc,"blackout-meeting.png",false);next(); }
                case 10 -> { commands(mc,"scoreboard players set #MeetingPhase ggdSession 0");next(); }
                case 11 -> { if(!GamePresenceClient.phaseId().equals("playing"))return;
                    commands(mc,"execute as @p at @s run goosetools meeting-alert report @p");next(); }
                case 12 -> { if(elapsed()<500_000_000L)return;capture(mc,"blackout-report.png",true);next(); }
                case 13 -> { if(elapsed()<3_150_000_000L)return;
                    require(MeetingAlertHud.blocksMovement(),"report backdrop ended with banner");
                    commands(mc,"tp @p 162 -60 0 0 0","scoreboard players set #MeetingPhase ggdSession 1");next(); }
                case 14 -> { if(MeetingAlertHud.blocksMovement())return;
                    require(captures==6,"missing GPU screenshots");finish(mc,"PASS report/bell 200 ms fade-in; F1; loading delay; real meeting presence and teleport; 1000 ms reveal; six GPU captures"); }
                default -> throw new IllegalStateException("stage "+stage);
            }
        } catch(Throwable e) { e.printStackTrace();finish(mc,"FAIL stage "+stage+": "+e); }
    }
    private void next(){stage++;entered=now();}
    private Object alert() throws Exception {
        var f=MeetingAlertHud.class.getDeclaredField("active");f.setAccessible(true);return f.get(null);
    }
    private long elapsed() throws Exception {
        var a=alert();if(a==null)return 0;
        var f=a.getClass().getDeclaredField("startedNanos");f.setAccessible(true);return now()-f.getLong(a);
    }
    private long revealStarted() throws Exception {
        var a=alert();if(a==null)return -1;
        var f=a.getClass().getDeclaredField("transition");f.setAccessible(true);
        var r=MeetingAlertTransition.class.getDeclaredField("revealStarted");r.setAccessible(true);return r.getLong(f.get(a));
    }
    private void commands(Minecraft mc,String... commands) {
        pending=mc.getSingleplayerServer().submit(() -> {
            for(String command:commands)mc.getSingleplayerServer().getCommands().performPrefixedCommand(
                    mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(),command);
        });
    }
    private void capture(Minecraft mc,String name,boolean black) {
        var result=new CompletableFuture<Void>();pending=result;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image -> {
            try(image) {
                image.writeToFile(mc.gameDirectory.toPath().resolve(name));
                int samples=0;
                for(double x:new double[]{.35,.5,.75,.9})for(double y:new double[]{.12,.18,.8}) {
                    int rgb=image.getPixel((int)(image.getWidth()*x),(int)(image.getHeight()*y))&0xffffff;
                    if(rgb==0)samples++;
                }
                require(black?samples==12:samples<12,"unexpected map visibility in "+name+": "+samples+" black samples");
                captures++;result.complete(null);
            } catch(Throwable e) { result.completeExceptionally(e); }
        });
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){
        if(finished)return;finished=true;if(mc.gui.hud.isHidden())mc.gui.hud.toggle();
        try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(message);mc.stop();
    }
}
