package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.GuiMovementGuard;
import com.goosethings.tools.intro.RoleIntroPayload;
import com.goosethings.tools.result.MatchResultPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** One focused real client checks network snapshots, wardrobe, opaque handoff and result rendering. */
final class PresentationRegression {
    private boolean world, requested, captured, finished, blackout;
    private int warmup, scenario, shots;
    private long since;
    private CompletableFuture<Void> pending;
    private static long now() { return System.nanoTime()/1_000_000; }
    void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!world) {
                world=true; mc.options.pauseOnLostFocus=false; mc.options.renderDistance().set(3);
                mc.createWorldOpenFlows().createFreshLevel("presentation-"+System.currentTimeMillis(),
                        new LevelSettings("Presentation verification",GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42,false,false), provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++warmup<40)return;
            if(pending!=null) { if(!pending.isDone())return; pending.join();pending=null; }
            if(scenario==7) { require(shots==7,"screenshots incomplete");finish(mc,"PASS real network wardrobe/head snapshots; seven-second intro -> opaque black -> server reveal; 21-player Duck/Goose and neutral/Lover results; dead winners head+coat only; frozen color/serial/outfit; non-DLC gate and cleanup; seven GPU screenshots");return; }
            if(!requested) {
                requested=true;captured=false;since=now();
                if(scenario==0) pending=fixture(mc,"evil",true);
                else if(scenario<=4) pending=fixture(mc,switch(scenario){case 1->"evil";case 2->"good_objective";case 3->"dodo";default->"lover";},false);
                else if(scenario==5) pending=commands(mc,"goosetools match-result close @a","scoreboard players set FullBloodDLC ggdadv 0","goosetools match-result capture","goosetools match-result open @a");
                else {
                    var equipment=new ArrayList<ItemStack>(com.goosethings.tools.intro.PresentationAppearance.emptyEquipment());
                    equipment.set(EquipmentSlot.CHEST.ordinal(),new ItemStack(Items.DIAMOND_CHESTPLATE));
                    RoleIntroScreen.apply(new RoleIntroPayload(true,"Spy",1,0,List.of(new RoleIntroPayload.Appearance(mc.player.getUUID(),mc.player.getGameProfile().name(),false,equipment))));
                }
                return;
            }
            require(now()-since<24000,"scenario timeout "+scenario);
            if(scenario==0) {
                if(mc.gui.screen() instanceof RoleIntroScreen screen && now()-since>1300 && !captured) {
                    require(!screen.payload().roster().getFirst().equipment().get(EquipmentSlot.CHEST.ordinal()).isEmpty(),"opening wardrobe not transmitted");
                    var models=models(screen);require(models.size()==1,"opening preview missing");
                    var model=models.values().iterator().next();
                    require(model.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD),"opening own head missing");
                    require(model.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"opening armor missing");
                    capture(mc,"presentation-intro.png");captured=true;
                }
                if(now()-since>7800 && !blackout) {
                    require(mc.gui.screen() instanceof PresentationBlackoutScreen,"world leaked after intro");
                    require(GuiMovementGuard.blocks(mc.gui.screen()),"blackout input escaped");
                    capture(mc,"presentation-blackout.png");blackout=true;return;
                }
                if(blackout && now()-since>10000) {
                    require(mc.gui.screen() instanceof PresentationBlackoutScreen,"blackout released before server reveal");
                    pending=commands(mc,"goosetools role-intro close @a");next();
                }
                return;
            }
            if(scenario>=1&&scenario<=4) {
                if(!(mc.gui.screen() instanceof MatchResultScreen screen))return;
                if(now()-since<1600)return;
                require(screen.payload().roster().size()==(scenario<=2?21:3),"result participants lost");
                require(MatchResultPolicy.theme(screen.payload().outcome())==(scenario==1?1:scenario==2?0:scenario==3?2:3),"result theme wrong");
                require(screen.payload().roster().stream().filter(p->p.winner()).count()==(scenario==1?3:scenario==2?18:scenario==3?1:2),"winner split wrong");
                require(GuiMovementGuard.blocks(screen)&&!screen.isPauseScreen()&&!screen.shouldCloseOnEsc(),"result input guard missing");
                var models=models(screen);require(models.size()==screen.payload().roster().size(),"result model missing");
                for(var p:screen.payload().roster()) {
                    var model=models.get(p.id());
                    require(model.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD),"result head missing");
                    require(model.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"frozen wardrobe changed");
                    require(model.isInvisible()==p.dead(),"ghost body visibility wrong");
                    if(p.dead()) {
                        require(model.getItemBySlot(EquipmentSlot.LEGS).isEmpty()&&model.getItemBySlot(EquipmentSlot.FEET).isEmpty(),"ghost legs/shoes visible");
                    } else require(!model.getItemBySlot(EquipmentSlot.LEGS).isEmpty(),"living outfit missing");
                    require(p.serial()>0,"serial missing");
                }
                capture(mc,"presentation-result-"+scenario+".png");pending=commands(mc,"goosetools match-result close @a");next();return;
            }
            if(scenario==5) { if(now()-since<800)return;require(!(mc.gui.screen() instanceof MatchResultScreen),"non-DLC result changed");next();return; }
            if(scenario==6&&now()-since>1300) {
                require(mc.gui.screen() instanceof RoleIntroScreen,"local stage missing");
                capture(mc,"presentation-close.png");RoleIntroScreen.apply(RoleIntroPayload.CLOSE);
                require(!(mc.gui.screen() instanceof RoleIntroScreen)&&!(mc.gui.screen() instanceof PresentationBlackoutScreen),"cleanup stuck");next();
            }
        } catch(Throwable error) {finish(mc,"FAIL "+error);}
    }
    private CompletableFuture<Void> fixture(Minecraft mc,String outcome,boolean intro) {
        CompletableFuture<Void> done=new CompletableFuture<>();
        mc.getSingleplayerServer().execute(()->{
            try {
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
                for(String command:List.of("scoreboard objectives add ggdadv dummy","scoreboard objectives add ready dummy",
                        "scoreboard players set FullBloodDLC ggdadv 1","scoreboard players set @a ready 1",
                        "goosetools role-intro close @a","goosetools match-result close @a","kill @e[tag=presentationFixture]"))
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);
                for(String tag:List.copyOf(player.entityTags()))if(!tag.equals("players"))player.removeTag(tag);
                player.addTag("players");player.addTag("p1");player.addTag("Spy");player.addTag("evil");player.addTag("ggdRoleShowLocked");
                if(!intro)player.addTag("endGame");
                var roster=new ListTag(); int count=intro?1:outcome.equals("evil")||outcome.equals("good_objective")?21:3;
                for(int i=1;i<=count;i++) {
                    UUID id=i==1?player.getUUID():new UUID(12,i);String name=i==1?player.getGameProfile().name():"Player"+i;
                    boolean dead=i==2;boolean lover=outcome.equals("lover")&&i<=2;
                    var entry=new CompoundTag();entry.putString("set","p"+i);entry.putString("real_name",name);
                    entry.put("UUID",new IntArrayTag(new int[]{(int)(id.getMostSignificantBits()>>32),(int)id.getMostSignificantBits(),(int)(id.getLeastSignificantBits()>>32),(int)id.getLeastSignificantBits()}));
                    entry.putString("faction",i<=3?"evil":"good");entry.putString("role",outcome.equals("dodo")&&i==2?"dodo":i<=3?"spy":"goose");entry.putBoolean("lover",lover);roster.add(entry);
                    var clone=new ArmorStand(server.overworld(),i,4,0);clone.addTag("cloneArmorStand");clone.addTag("p"+i);clone.addTag("presentationFixture");
                    if(dead)clone.addTag("ggdSeatDead");
                    var head=new ItemStack(Items.PLAYER_HEAD);head.set(DataComponents.PROFILE,ResolvableProfile.createResolved(new com.mojang.authlib.GameProfile(id,name)));
                    clone.setItemSlot(EquipmentSlot.HEAD,head);clone.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
                    clone.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));clone.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));server.overworld().addFreshEntity(clone);
                }
                var stored=new CompoundTag();stored.put("data",roster);server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","role"),stored);
                var temp=new CompoundTag();temp.putString("end_reveal",outcome);server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","temp"),temp);
                if(intro)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"goosetools role-intro open @a");
                else {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"goosetools match-result capture");
                    // Later equipment changes cannot change the snapshot sent to the client.
                    for(var entity:server.overworld().getAllEntities())if(entity instanceof ArmorStand clone&&entity.entityTags().contains("presentationFixture"))clone.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"goosetools match-result open @a");
                }
                done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}
        });return done;
    }
    @SuppressWarnings("unchecked") private static java.util.Map<UUID,net.minecraft.client.player.RemotePlayer> models(Object screen)throws Exception {
        var field=screen.getClass().getDeclaredField("models");field.setAccessible(true);return (java.util.Map<UUID,net.minecraft.client.player.RemotePlayer>)field.get(screen);
    }
    private static CompletableFuture<Void> commands(Minecraft mc,String... commands) {
        var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{
            try{var server=mc.getSingleplayerServer();for(var command:commands)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);done.complete(null);}catch(Throwable error){done.completeExceptionally(error);}
        });return done;
    }
    private void next(){scenario++;requested=false;captured=false;}
    private void capture(Minecraft mc,String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve(name));shots++;}catch(Exception error){mc.execute(()->finish(mc,"FAIL screenshot "+error));}});
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message) {
        if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(message);mc.stop();
    }
}
