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
    private int cursorStep,cursorWait;
    private double cursorX,cursorY;
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
            if(Boolean.getBoolean("goosetools.menuCursorRegression")){cursor(mc);return;}
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
    private net.minecraft.client.gui.screens.inventory.ContainerScreen chest(Minecraft mc,int rows){
        return new net.minecraft.client.gui.screens.inventory.ContainerScreen(
            rows==3?net.minecraft.world.inventory.ChestMenu.threeRows(81,mc.player.getInventory()):net.minecraft.world.inventory.ChestMenu.sixRows(82,mc.player.getInventory()),
            mc.player.getInventory(),net.minecraft.network.chat.Component.literal(rows==3?"Training maps (3 rows)":"Roles / Assassin menu (6 rows)"));
    }
    private void position(Minecraft mc){
        cursorX=mc.getWindow().getScreenWidth()*0.27;cursorY=mc.getWindow().getScreenHeight()*0.68;
        com.mojang.blaze3d.platform.InputConstants.releaseMouse(mc.getWindow(),cursorX,cursorY);mc.mouseHandler.resyncMousePosition();
    }
    private void preserved(Minecraft mc,String what){
        if(Math.abs(mc.mouseHandler.xpos()-cursorX)>2||Math.abs(mc.mouseHandler.ypos()-cursorY)>2)
            throw new AssertionError(what+": cursor "+mc.mouseHandler.xpos()+","+mc.mouseHandler.ypos()+" expected "+cursorX+","+cursorY);
    }
    private void cursor(Minecraft mc){
        if(++cursorWait<8)return;cursorWait=0;
        switch(cursorStep++){
            case 0->{mc.gui.setScreen(chest(mc,3));position(mc);}
            case 1->{mc.gui.setScreen(chest(mc,6));preserved(mc,"map to role screen");mc.gui.setScreen(null);}
            case 2->{mc.gui.setScreen(chest(mc,6));preserved(mc,"close packet then reopen next tick");}
            case 3->{mc.gui.setScreen(chest(mc,6));preserved(mc,"assassin faction/page replacement");
                var data=new net.minecraft.server.dialog.CommonDialogData(net.minecraft.network.chat.Component.literal("Settings"),java.util.Optional.empty(),true,false,net.minecraft.server.dialog.DialogAction.CLOSE,java.util.List.of(),java.util.List.of());
                var notice=new net.minecraft.server.dialog.NoticeDialog(data,net.minecraft.server.dialog.NoticeDialog.DEFAULT_ACTION);
                var access=(net.minecraft.client.gui.screens.dialog.DialogConnectionAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.client.gui.screens.dialog.DialogConnectionAccess.class},(proxy,method,args)->method.getName().equals("serverLinks")?net.minecraft.server.ServerLinks.EMPTY:null);
                mc.gui.setScreen(net.minecraft.client.gui.screens.dialog.DialogScreens.createFromData(notice,null,access));preserved(mc,"container to native dialog");}
            case 4->{mc.gui.setScreen(null);}
            case 5->{mc.gui.setScreen(chest(mc,6));preserved(mc,"native dialog back to phone");}
            case 6->{preserved(mc,"position remains stable after client ticks");finish(mc,"PASS real client cursor retention: 3/6 row menus, delayed close/open, Assassin-style page replacement and native dialog return");}
        }
    }
    private void finish(Minecraft mc,String result) {
        done=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(result);mc.stop();
    }
}
