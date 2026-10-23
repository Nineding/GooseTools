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
import java.util.List;

/** Compare real GPU frames with and without artwork; rectangle mistakes cannot pass asset-only tests. */
final class RoleIntroArtworkRegression {
    private final String[] roles = {"Raven", "Assassin", "Goose", "DuelingDodos"};
    private boolean world, finished, pending;
    private int role, phase, sourceWidth, baselineWidth;
    private int[] baseline;
    private long since;
    private RoleIntroScreen screen;
    private RoleIntroArtwork.Bounds box;

    void tick(Minecraft mc) {
        if (finished || pending || !mc.isGameLoadFinished()) return;
        try {
            if (!world) {
                world = true; mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(3);
                mc.createWorldOpenFlows().createFreshLevel("artwork-"+System.currentTimeMillis(),
                        new LevelSettings("Role artwork GPU check", GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42,false,false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
                return;
            }
            if (mc.player == null || mc.level == null || !mc.player.connection.hasClientLoaded()) return;
            if (role == roles.length) { finish(mc,"PASS real GPU artwork pixels: Raven, Assassin, Goose, DuelingDodos; text-safe rectangles; GUI scales 2/3"); return; }
            if (phase == 0) {
                mc.options.guiScale().set(role % 2 == 0 ? 2 : 3); mc.resizeGui();
                var definition = RoleIntroCatalog.byTag(roles[role]);
                RoleIntroScreen.apply(new RoleIntroPayload(true,roles[role],definition.faction(),0,
                        List.of(new RoleIntroPayload.Appearance(mc.player.getUUID(),mc.player.getGameProfile().name(),false))));
                screen = (RoleIntroScreen)mc.gui.screen(); phase = 1; since = System.currentTimeMillis(); return;
            }
            if (phase == 1) {
                if (System.currentTimeMillis()-since < 1800) return;
                sourceWidth = (int)field("artworkWidth").get(screen);
                box = (RoleIntroArtwork.Bounds)field("artworkBounds").get(screen);
                if (sourceWidth <= 0 || box == null || box.right() <= box.left() || box.bottom() <= box.top())
                    throw new IllegalStateException("missing drawable artwork: "+roles[role]);
                field("artworkWidth").set(screen,0); phase = 2; since = System.currentTimeMillis(); return;
            }
            if (System.currentTimeMillis()-since < 250) return;
            pending = true;
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image -> {
                try (image) {
                    if (phase == 2) {
                        baseline = image.getPixels(); baselineWidth = image.getWidth();
                        mc.execute(() -> {try {field("artworkWidth").set(screen,sourceWidth);phase=3;since=System.currentTimeMillis();pending=false;}catch(Exception e){finish(mc,"FAIL "+e);}});
                    } else {
                        int changed = 0, w=image.getWidth(),h=image.getHeight();
                        if(w!=baselineWidth)throw new IllegalStateException("frame dimensions changed");
                        for(int y=Math.max(0,box.top()*h/screen.height);y<Math.min(h,box.bottom()*h/screen.height);y++)
                            for(int x=Math.max(0,box.left()*w/screen.width);x<Math.min(w,box.right()*w/screen.width);x++) {
                                if(Math.abs(x-w*.5)<w*.06)continue; // Exclude moving player/name pixels.
                                int a=baseline[y*w+x],b=image.getPixel(x,y),delta=0;
                                for(int shift:new int[]{16,8,0})delta+=Math.abs((a>>shift&255)-(b>>shift&255));
                                if(delta>12)changed++;
                            }
                        image.writeToFile(mc.gameDirectory.toPath().resolve("artwork-"+roles[role]+".png"));
                        if(changed<40)throw new IllegalStateException("artwork invisible in real GPU frame: "+roles[role]+" changed="+changed);
                        com.goosethings.tools.GooseTools.LOGGER.info("Artwork GPU check {} changed {} pixels",roles[role],changed);
                        mc.execute(()->{RoleIntroScreen.apply(RoleIntroPayload.CLOSE);role++;phase=0;pending=false;});
                    }
                } catch(Throwable e) {mc.execute(()->finish(mc,"FAIL "+e));}
            });
        } catch(Throwable e) {finish(mc,"FAIL "+e);}
    }
    private static java.lang.reflect.Field field(String name) throws Exception {
        var result=RoleIntroScreen.class.getDeclaredField(name);result.setAccessible(true);return result;
    }
    private void finish(Minecraft mc,String text) {
        if(finished)return;finished=true;
        try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(text);mc.stop();
    }
}
