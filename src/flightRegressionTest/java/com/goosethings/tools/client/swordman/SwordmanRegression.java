package com.goosethings.tools.client.swordman;

import com.goosethings.tools.GooseTools;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Exercises the real input packets, host authority, datapack and corpse renderer in a new isolated save. */
public final class SwordmanRegression implements ClientModInitializer {
    public static volatile boolean countCorrections;
    public static final java.util.concurrent.atomic.AtomicInteger corrections=new java.util.concurrent.atomic.AtomicInteger();
    private int interpolatedSamples;
    private boolean opened,finished,installed,wantUse;
    private int stage,ticks,checks,celebrityNotices;
    private UUID victimId,upperId,shieldId,behindId;
    private CompletableFuture<Void> pending;
    @Override public void onInitializeClient() {
        if(!Boolean.getBoolean("goosetools.swordmanRegressionTest"))return;
        ClientTickEvents.START_CLIENT_TICK.register(mc->{if(installed&&!finished)mc.options.keyUse.setDown(wantUse);});
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(Minecraft mc) {
        if(finished||!mc.isGameLoadFinished())return;
        try {
            if(!opened) {
                opened=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("swordman-"+System.currentTimeMillis(),
                        new LevelSettings("Swordman regression",GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42L,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;
            }
            if(mc.level==null||mc.player==null||!mc.player.connection.hasClientLoaded()||mc.player.tickCount<20)return;
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(!installed){installed=true;install(mc);return;}
            if(mc.gui.screen()!=null)mc.setScreenAndShow(null);
            mc.player.getInventory().setSelectedSlot(8);
            if(stage==0){server(mc,p->reset(p,0));stage++;ticks=0;return;}
            if(stage==1){if(++ticks==5)wantUse=true;if(ticks==15)wantUse=false;
                if(ticks<22)return;server(mc,p->{require(score(p,"ggdSwordCD")==0,"short release spent cooldown");require(p.getZ()<1,"short release dashed");});stage++;ticks=0;return;}
            if(stage==2){wantUse=true;++ticks;
                if(ticks<65)return;wantUse=false;corrections.set(0);countCorrections=true;stage++;ticks=0;return;}
            if(stage==3 && SwordmanClient.isDashing()) {
                var p=mc.player;
                var previous=new net.minecraft.world.phys.Vec3(p.xo,p.yo,p.zo);
                if(p.position().distanceToSqr(previous)>1.0E-8) {
                    interpolatedSamples++;
                    require(p.getPosition(0.5F).distanceTo(previous.lerp(p.position(),0.5))<1.0E-6,"camera interpolation did not retain approved endpoints");
                    require(p.getZ()>=p.zo,"dash interpolated backwards");
                }
            }
            if(stage==3){++ticks;if(ticks==5){server(mc,p->{require(p.entityTags().contains("swordmanDashing"),"dash ended before its floating phase");require(Math.abs(p.getY()-101)<0.05,"dash dropped while active");});return;}if(ticks<22)return;
                countCorrections=false;
                require(corrections.get()<=2,"dash still sends repeated position corrections: "+corrections.get());
                require(interpolatedSamples>=4,"dash lacked interpolated movement samples: "+interpolatedSamples);
                server(mc,p->{
                require(Math.abs(p.getZ()-14.5)<0.12,"full charge did not travel 14 blocks: "+p.position());
                require(target(p,victimId).entityTags().contains("beingKill"),"path target survived");
                require(target(p,upperId).entityTags().contains("beingKill"),"vertical distance incorrectly excluded target");
                require(score(p,"ggdSwordCD")==30,"dash did not enter cooldown");
                require(score(p,"achDuckGooseKills")==2,"Duck-Goose achievement count was not exactly two");
                for(ServerPlayer v:List.of(target(p,victimId),target(p,upperId))){command(v,"function ggd:deadveent/beingkilled");v.removeTag("beingKill");}
                require(celebrityNotices==0,"Celebrity announced before collapse");
                require(!p.entityTags().contains("KilledCanadian"),"Canadian armed at slash time");
                target(p,upperId).addTag("endGame");
            });stage++;ticks=0;return;}
            if(stage==4){if(++ticks<15)return;server(mc,p->{
                List<Entity> bodies=corpses(p);require(bodies.size()==2,"two corpses not created: "+bodies.size());
                require(bodies.stream().allMatch(b->b.entityTags().contains("swordmanFrozen")),"corpse collapsed before approach");
                require(bodies.stream().anyMatch(b->b.getPose()==Pose.CROUCHING),"crouching pose was lost");
                p.connection.teleport(.5,101,6.5,180,0);
            });stage++;ticks=0;return;}
            if(stage==5){if(++ticks<10)return;server(mc,p->{
                List<Entity> bodies=corpses(p);require(bodies.stream().filter(b->b.entityTags().contains("swordmanFrozen")).count()==1,"approach did not collapse only the nearby corpse");
                require(bodies.stream().anyMatch(b->b.getPose()==Pose.SLEEPING),"corpse did not become sleeping");
                var recorded=p.level().getServer().getCommandStorage().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("ggd","swordman_test")).get("reporter");
                long msb=p.getUUID().getMostSignificantBits(),lsb=p.getUUID().getLeastSignificantBits();
                require(new net.minecraft.nbt.IntArrayTag(new int[]{(int)(msb>>32),(int)msb,(int)(lsb>>32),(int)lsb}).equals(recorded),"Canadian reporter was not the Swordman");
                require(celebrityNotices==0,"distant frozen Celebrity announced");
                p.connection.teleport(.5,107,6.5,180,0);
            });stage=50;ticks=0;return;}
            if(stage==50){if(++ticks<10)return;server(mc,p->{require(celebrityNotices>0,"Celebrity did not announce on collapse");reset(p,1);});stage=6;ticks=0;return;}
            if(stage==6){wantUse=true;if(++ticks<65)return;wantUse=false;stage++;ticks=0;return;}
            if(stage==7){if(++ticks<22)return;server(mc,p->{
                require(!target(p,shieldId).entityTags().contains("guardShielded"),"shield was not consumed");
                require(!target(p,shieldId).entityTags().contains("beingKill"),"shielded player died");
                require(!target(p,behindId).entityTags().contains("beingKill"),"dash killed player behind shield");
                require(score(p,"achDuckGooseKills")==2,"shield counted a false achievement kill");
                require(p.getZ()<6,"shield did not stop dash");
                require(score(p,"ggdSwordCD")==30,"blocked dash did not cool down");reset(p,2);
            });stage++;ticks=0;return;}
            if(stage==8){wantUse=true;if(++ticks<65)return;wantUse=false;stage++;ticks=0;return;}
            if(stage==9){if(++ticks<22)return;server(mc,p->{
                require(p.getZ()<7.8,"dash crossed wall");require(!target(p,behindId).entityTags().contains("beingKill"),"victim behind wall died");
                require(score(p,"ggdSwordCD")==30,"wall-ended dash did not cool down");
            });stage++;ticks=0;return;}
            finish(mc,"PASS "+checks+" checks: camera interpolation, single endpoint correction, real hold/release, short cancellation, linear capped distance, floating dash, vertical sweep, death/corpse pose, delayed Canadian reporter and Celebrity notice, post-dash approach, shield consumption and interception, wall collision, cooldown.");
        }catch(Throwable error){GooseTools.LOGGER.error("Swordman regression failed",error);finish(mc,"FAIL "+error);}
    }
    private void install(Minecraft mc) throws Exception {
        var srv=mc.getSingleplayerServer();Path source=Path.of(System.getProperty("goosetools.swordmanDatapack"));
        Path target=srv.getWorldPath(LevelResource.DATAPACK_DIR).resolve("Whoiskiller");
        pending=CompletableFuture.runAsync(()->{try(var files=Files.walk(source)){
            for(Path file:files.toList()){
                Path relative=source.relativize(file);String name=relative.toString().replace('\\','/');
                if(name.equals("data/minecraft/tags/function/tick.json")||name.equals("data/minecraft/tags/function/load.json"))continue;
                Path out=target.resolve(relative);if(Files.isDirectory(file))Files.createDirectories(out);else {
                    Files.copy(file,out);
                    // Observe the actual report entry while its existing end-game guard keeps this minimal fixture out of a meeting.
                    if(name.equals("data/ggd/function/report/trigger.mcfunction"))Files.writeString(out,
                            "execute if entity @s[tag=swordmanDeferredReporter] run data modify storage ggd:swordman_test reporter set from entity @s UUID\n"+Files.readString(out,java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }catch(Exception e){throw new RuntimeException(e);}}).thenRunAsync(()->srv.getPackRepository().reload(),srv)
                .thenCompose(v->{var ids=new ArrayList<>(srv.getPackRepository().getSelectedIds());ids.add("file/Whoiskiller");return srv.reloadResources(ids);})
                .thenRunAsync(()->{
                    ServerPlayer p=srv.getPlayerList().getPlayer(mc.player.getUUID());var objectives=new HashSet<String>();
                    Pattern pattern=Pattern.compile("scoreboard objectives add ([A-Za-z0-9_]+)");
                    try(var files=Files.walk(source)){for(Path f:files.filter(f->f.toString().endsWith(".mcfunction")).toList()){
                        var matcher=pattern.matcher(new String(Files.readAllBytes(f),java.nio.charset.StandardCharsets.UTF_8));while(matcher.find())objectives.add(matcher.group(1));
                    }}catch(Exception e){throw new RuntimeException(e);}
                    for(String objective:objectives)if(srv.getScoreboard().getObjective(objective)==null)command(p,"scoreboard objectives add "+objective+" dummy");
                    victimId=fake(p,"SwordVictim",2);upperId=fake(p,"UpperVictim",3);shieldId=fake(p,"ShieldVictim",4);behindId=fake(p,"BehindShield",5);
                },srv);
    }
    private UUID fake(ServerPlayer p,String name,int seat){
        var srv=p.level().getServer();var profile=new GameProfile(UUID.randomUUID(),name);
        var fake=new ServerPlayer(srv,p.level(),profile,ClientInformation.createDefault());
        fake.connection=new ServerGamePacketListenerImpl(srv,new Connection(PacketFlow.SERVERBOUND),fake,CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(Packet<?> packet){if(packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat&&chat.content().getString().contains("\uE07A"))celebrityNotices++;}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
        };
        fake.addTag("p"+seat);fake.setNoGravity(true);fake.absSnapTo(.5,101,40,0,0);
        srv.getPlayerList().getPlayers().add(fake);srv.getPlayerList().getPlayersByUUID().put(fake.getUUID(),fake);
        p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fake)));p.level().addNewPlayer(fake);return fake.getUUID();
    }
    private void reset(ServerPlayer p,int scenario){
        command(p,"gt swordman clear");command(p,"kill @e[tag=deadbody]");
        for(String value:List.of("FullBloodDLC 1","SwordMinCharge 2","SwordMaxCharge 3","SwordDistance 14","SwordUseCD 30","SwordRadius 1","HowRole 0")){
            String[] pair=value.split(" ");command(p,"scoreboard players set "+pair[0]+" ggdadv "+pair[1]);
        }
        for(ServerPlayer q:p.level().getServer().getPlayerList().getPlayers()){
            String seat=q==p?"p1":q==target(p,victimId)?"p2":q==target(p,upperId)?"p3":q==target(p,shieldId)?"p4":"p5";
            for(String tag:List.copyOf(q.entityTags()))q.removeTag(tag);
            for(String tag:List.of("players","gamingGGD",seat,q==p?"evil":"good"))q.addTag(tag);
            q.setGameMode(GameType.ADVENTURE);q.setNoGravity(q!=p);q.setShiftKeyDown(false);q.setPose(Pose.STANDING);
            q.absSnapTo(.5,101,40,0,0);
        }
        p.addTag("Swordman");p.connection.teleport(.5,101,.5,0,0);p.getInventory().setSelectedSlot(8);
        command(p,"scoreboard players set @s ggdSwordCD 0");
        // Isolate the skill from unrelated lobby/inventory callbacks in this minimal, map-free fixture.
        p.addTag("ggdSkipInvChanged");command(p,"function ggd:skill/swordman/render");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=36;z++){
            p.level().setBlockAndUpdate(new BlockPos(x,100,z),z<=3?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            for(int y=101;y<=103;y++)p.level().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        target(p,victimId).addTag("Canadian");target(p,upperId).addTag("Celebrity");
        target(p,victimId).absSnapTo(.5,101,6.5,180,0);target(p,victimId).setShiftKeyDown(true);target(p,victimId).setPose(Pose.CROUCHING);
        target(p,upperId).absSnapTo(.5,107,6.5,180,0);
        if(scenario==1){target(p,victimId).absSnapTo(.5,101,40,0,0);target(p,upperId).absSnapTo(.5,107,40,0,0);
            target(p,shieldId).absSnapTo(.5,101,6.5,0,0);target(p,shieldId).addTag("guardShielded");target(p,behindId).absSnapTo(.5,101,10.5,0,0);}
        if(scenario==2){for(UUID id:List.of(victimId,upperId,shieldId))target(p,id).absSnapTo(.5,107,40,0,0);
            target(p,behindId).absSnapTo(.5,101,10.5,0,0);for(int x=-2;x<=2;x++)for(int y=101;y<=103;y++)p.level().setBlockAndUpdate(new BlockPos(x,y,8),Blocks.STONE.defaultBlockState());}
    }
    private List<Entity> corpses(ServerPlayer p){var result=new ArrayList<Entity>();for(Entity e:p.level().getAllEntities())if(e.entityTags().contains("deadbody"))result.add(e);return result;}
    private ServerPlayer target(ServerPlayer p,UUID id){return p.level().getServer().getPlayerList().getPlayer(id);}
    private int score(ServerPlayer p,String objective){var value=p.level().getServer().getScoreboard().getPlayerScoreInfo(p,p.level().getServer().getScoreboard().getObjective(objective));return value==null?-999:value.value();}
    private void server(Minecraft mc,Consumer<ServerPlayer> action){pending=CompletableFuture.runAsync(()->action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());}
    private void command(ServerPlayer p,String command){var srv=p.level().getServer();srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withEntity(p).withLevel(p.level()).withPosition(p.position()).withSuppressedOutput(),command);}
    private void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private void finish(Minecraft mc,String result){finished=true;wantUse=false;countCorrections=false;try{Files.writeString(mc.gameDirectory.toPath().resolve("swordman-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}GooseTools.LOGGER.info(result);mc.stop();}
}
