package com.goosethings.tools.client.projection;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import com.goosethings.tools.client.dream.DreamTestLatency;
import com.goosethings.tools.client.dream.DreamTcpFixture;
import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.goosethings.tools.projection.ProjectionBodyServer;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.commands.CommandSourceStack;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Actual datapack entry points, real remote-player tracking, and the local owner's native body render. */
public final class RetainedProjectionRegression implements ClientModInitializer {
    public static boolean suspendBodyTicks;
    private static final String[] ROLES = {"astral", "sniper", "esper", "mime"};
    private static final String[] TAGS = {"Astral", "Sniper", "Esper", "Mime"};
    private boolean opened, initialized, finished;
    private int ticks, scenario, stage, checks, ownerBodyFrames, extractionFrames, renderWaits, sourceId;
    private UUID fakeId;
    private CompletableFuture<?> pending;
    private Vec3 entry;
    private boolean ownerView;
    private String lastRender = "none";
    private Entity movementCamera;
    private Vec3 movementStart, previousCameraPosition;
    private int movementSamples;
    private UUID walkerId;
    private int walkerEntityId, walkingFrames, bodyWalkingFrames;
    private Entity trackedWalker;
    private String idleAnimationError;
    public static volatile boolean countCorrections;
    public static final java.util.concurrent.atomic.AtomicInteger corrections = new java.util.concurrent.atomic.AtomicInteger();

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("goosetools.retainedProjectionRegression")) return;
        DreamTestLatency.register();
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            suspendBodyTicks=ownerView && (stage==1 || stage==4);
            boolean moving=ownerView && ticks<20 && (stage==4 && scenario%4!=2
                    || stage==1 && (scenario%4==0 || scenario%4==1));
            mc.options.keyUp.setDown(moving);
            mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);mc.options.keySprint.setDown(false);
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerPlayer fake=fakeId==null?null:server.getPlayerList().getPlayer(fakeId);
            // This observer has no real network connection to drive ServerPlayer.doTick().
            // Run vanilla equipment detection so changing its role also updates tracked clients.
            if(fake!=null)fake.doTick();
            ServerPlayer walker=walkerId==null?null:server.getPlayerList().getPlayer(walkerId);
            if(walker!=null) {
                int phase=server.getTickCount()%160;
                walker.absSnapTo(12.5+.1*Math.min(phase,160-phase),81,4.5,90,0);
                walker.doTick();
            }
            if(!ownerView && scenario==7 && stage==4 && fake!=null) {
                Entity actor=DreamAvatarServer.actor(fake);
                if(actor!=null) {
                    DreamAvatarServer.move(fake,fake.level(),actor.position().add(.12,0,0),90,0);
                    com.goosethings.tools.dream.DreamStandInServer.avatarChanged();
                }
            }
            if(ownerView && stage==4 && scenario%4==2 && fake!=null)
                fake.teleportTo(fake.level(),fake.getX()+.1,fake.getY(),fake.getZ(),Set.of(),90,0,false);
        });
        // Run after the body's extraction callback even when Fabric initializes this fixture first.
        var renderPhase=net.minecraft.resources.Identifier.fromNamespaceAndPath("goosetools_flight_test","retained_body");
        LevelRenderEvents.END_EXTRACTION.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE,renderPhase);
        LevelRenderEvents.END_EXTRACTION.register(renderPhase,context -> {
            Minecraft mc = Minecraft.getInstance();
            extractionFrames++;
            if (!ownerView || !DreamAvatarClient.retainedProjection() || mc.player == null
                    || mc.getCameraEntity().distanceToSqr(mc.player)<100) return;
            lastRender=context.levelState().entityRenderStates.stream().map(state->state.getClass().getSimpleName()+":"+state.x+","+state.y+","+state.z
                    +":"+state.isInvisible).toList().toString();
            for (var state : context.levelState().entityRenderStates) {
                if (state instanceof AvatarRenderState avatar && !avatar.isInvisible && !avatar.isSpectator
                        && new Vec3(state.x,state.y,state.z).distanceToSqr(mc.player.position()) < .01) {
                    if (avatar.walkAnimationSpeed != 0)
                        idleAnimationError="stationary extracted body retained walk speed "+avatar.walkAnimationSpeed;
                    ownerBodyFrames++;break;
                }
            }
        });
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;mc.options.pauseOnLostFocus = false;
                if(DreamTcpFixture.open(mc))return;
                mc.createWorldOpenFlows().createFreshLevel("retained-" + System.currentTimeMillis(),
                        new LevelSettings("Retained projection", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42,false,false), provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if(DreamTcpFixture.awaitConnection(mc))return;
            if (mc.level == null || mc.player == null || !mc.player.connection.hasClientLoaded()) return;
            if (mc.gui.screen() != null) mc.setScreenAndShow(null);
            if (pending != null) { if (!pending.isDone()) return;pending.join();pending = null; }
            if(ownerView && stage==4) {
                require(mc.getCameraEntity()==movementCamera,"projection camera switched back to the physical body");
                Vec3 current=mc.getCameraEntity().position();
                require(current.distanceTo(previousCameraPosition)<1,"projection camera jumped during sustained input: "+previousCameraPosition+" -> "+current);
                previousCameraPosition=current;movementSamples++;
                require(mc.level.getEntity(walkerEntityId)==trackedWalker,"moving player was removed/recreated in detached view");
                if(((RemotePlayer)trackedWalker).walkAnimation.speed()>.01)walkingFrames++;
                if(mc.getCameraEntity() instanceof net.minecraft.world.entity.LivingEntity body
                        && body.walkAnimation.speed()>.01)bodyWalkingFrames++;
                // This fixture's END callback may precede the prediction callback.
                // The second observation is still before the simulated 6-tick round trip.
                if(scenario%4==3 && movementSamples==2)
                    require(current.distanceTo(movementStart)>.01,"Mime ground input waited for delayed server movement: "+movementStart+" -> "+current);
            }
            if(!ownerView && scenario==7 && stage==4) {
                require(com.goosethings.tools.client.mime.MimeControlClient.followsBody(),"victim did not enter body-follow camera");
                require(mc.getCameraEntity()==movementCamera && movementCamera!=mc.player,"victim camera fell back onto the teleported LocalPlayer");
                Vec3 current=movementCamera.position();
                require(current.distanceTo(previousCameraPosition)<.5,"victim camera jumped during controlled walking");
                previousCameraPosition=current;
            }
            if (++ticks < 25) return;
            ticks = 0;
            if (!initialized) { initialized = true;installPack(mc);return; }
            if (stage == 0) {
                ownerBodyFrames=0;renderWaits=0;idleAnimationError=null;ownerView=scenario<4 || scenario>=8;
                // Enter while the native walk cycle is active. Suspending aiStep must
                // not freeze this phase and replay its partial-tick limb motion forever.
                if(ownerView)mc.player.walkAnimation.update(.8F,1,1);
                server(mc,p->begin(p));stage++;return;
            }
            if (stage == 1) {
                verifyClient(mc);
                if(ownerView && (scenario%4==0 || scenario%4==1))
                    require(mc.getCameraEntity().position().distanceTo(entry)>.5,"native input could not leave the original body: role="+ROLES[scenario%4]
                            +", camera="+mc.getCameraEntity().position()+", body="+mc.player.position());
                if(ownerView && !DreamAvatarClient.followsTarget())DreamAvatarClient.turn(
                        (90-mc.getCameraEntity().getYRot())/.15, -mc.getCameraEntity().getXRot()/.15);
                server(mc,p->separate(p));stage++;return;
            }
            if (stage == 2) {
                verifyClient(mc);
                // Resource reload can advance many client ticks before drawing the first world frame.
                if(ownerView && ownerBodyFrames==0 && ++renderWaits<5)return;
                if (ownerView) require(ownerBodyFrames>0,"owner's actual body never entered entity render extraction: scenario="+scenario
                        +", frames="+extractionFrames+", body="+mc.player.position()+", camera="+mc.getCameraEntity().position()
                        +", chunk="+mc.level.hasChunkAt(mc.player.blockPosition())
                        +", culled="+com.goosethings.tools.client.vision.VisionFogState.shouldHideEntityVisual(mc.player)
                        +", range="+mc.player.shouldRenderAtSqrDistance(mc.getCameraEntity().distanceToSqr(mc.player))+", states="+lastRender);
                if(ownerView)require(idleAnimationError==null,idleAnimationError);
                if(scenario==1 || scenario==5)net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image->{
                    try(image){image.writeToFile(mc.gameDirectory.toPath().resolve(scenario==1?"retained-owner-sniper.png":"retained-observer-sniper.png"));}
                    catch(Exception error){throw new RuntimeException(error);}
                });
                server(mc,p->verifyServer(p));stage++;return;
            }
            if (stage == 3) {
                movementCamera=mc.getCameraEntity();movementStart=previousCameraPosition=movementCamera.position();movementSamples=0;
                walkingFrames=bodyWalkingFrames=0;
                if(ownerView) {
                    trackedWalker=mc.level.getEntity(walkerEntityId);
                    require(trackedWalker instanceof RemotePlayer,"detached view did not track the walking player");
                }
                corrections.set(0);countCorrections=!ownerView && scenario==7;
                server(mc,p->{
                    if(scenario%4==3)require(source(p).getX()>entry.x+.1,"Mime real body did not mirror plausible remote movement");
                    if(ownerView)ServerPlayNetworking.send(p,new GooseToolsPayloads.MimeControlS2C(true,fakeId));
                });
                DreamTestLatency.enabled=ownerView && scenario%4!=2;
                stage++;return;
            }
            if(stage==4) {
                if(ownerView) {
                    require(movementSamples>=20,"not enough continuous camera samples");
                    require(mc.getCameraEntity().position().distanceTo(movementStart)>.5,"native movement left the projection stuck at its origin: role="+ROLES[scenario%4]);
                    require(walkingFrames>10,"tracked remote player slid without walking animation");
                    if(scenario%4==3)require(bodyWalkingFrames>10,"predicted Mime model slid without walking animation");
                }
                if(!ownerView && scenario==7) {
                    countCorrections=false;
                    require(corrections.get()==0,"victim received continuous position teleports: "+corrections.get());
                    require(movementCamera.position().distanceTo(movementStart)>1,"victim's spectator-style body camera did not follow movement");
                }
                DreamTestLatency.enabled=false;mc.options.keyUp.setDown(false);
                server(mc,p->{
                    if(ownerView)ServerPlayNetworking.send(p,new GooseToolsPayloads.MimeControlS2C(false,fakeId));
                    end(p);
                });stage++;return;
            }
            require(!DreamAvatarClient.retainedProjection(),"owner retained remote camera after cleanup");
            if(!ownerView && scenario==7)require(!com.goosethings.tools.client.mime.MimeControlClient.isControlled()
                    && mc.getCameraEntity()==mc.player,"victim camera/input was not restored after disconnect cleanup");
            server(mc,p->{
                ServerPlayer source=source(p);
                require(!ProjectionBodyServer.isRetained(source) && DreamAvatarServer.actor(source)==null,"server session survived cleanup");
                require(!source.entityTags().contains("projectionBodyRetained"),"retained tag survived cleanup");
                require(source.gameMode()==GameType.ADVENTURE,"projection changed the source game mode");
            });
            GooseTools.LOGGER.info("Retained projection passed {} {}",ownerView?"owner":"observer",ROLES[scenario%4]);
            if (++scenario==11) {
                finish(mc,"PASS "+checks+" checks: actual four-role datapack entry points, owner and tracked remote observer, "
                        +"Seagull borrowing, predicted Mime input, continuous tracked player identity/walk cycles, victim follow camera with zero ongoing teleports, native body ID/render, 20-block separation, crosshair/voice origins, "
                        +"physical/remote coordinates, armour, return, meeting/swallowing/death/disconnect cleanup");
            } else stage=0;
        } catch (Throwable error) { GooseTools.LOGGER.error("Retained projection regression failed",error);finish(mc,"FAIL "+error); }
    }

    private void installPack(Minecraft mc) throws Exception {
        var srv=DreamTcpFixture.server(mc);
        Path source=Path.of(System.getProperty("goosetools.retainedDatapack"));
        Path target=srv.getWorldPath(LevelResource.DATAPACK_DIR).resolve("Whoiskiller");
        pending=CompletableFuture.runAsync(()->{try (var files=Files.walk(source)) {
            for (Path file:files.toList()) {
                Path relative=source.relativize(file);
                String name=relative.toString().replace('\\','/');
                if (name.equals("data/minecraft/tags/function/tick.json") || name.equals("data/minecraft/tags/function/load.json")) continue;
                Path destination=target.resolve(relative);
                if(Files.isDirectory(file))Files.createDirectories(destination);else Files.copy(file,destination);
            }
        } catch(Exception error){throw new java.util.concurrent.CompletionException(error);}}).thenRunAsync(()->{
            srv.getPackRepository().reload();
        },srv).thenCompose(ignored->{
            var ids=new ArrayList<>(srv.getPackRepository().getSelectedIds());ids.add("file/Whoiskiller");
            return srv.reloadResources(ids);
        }).thenRunAsync(()->{
            ServerPlayer player=srv.getPlayerList().getPlayer(mc.player.getUUID());
            var objectives=new HashSet<String>();
            Pattern objective=Pattern.compile("scoreboard objectives add ([A-Za-z0-9_]+)");
            try(var files=Files.walk(source)) {
                for(Path file:files.filter(p->p.toString().endsWith(".mcfunction")).toList()) {
                    var matcher=objective.matcher(new String(Files.readAllBytes(file),java.nio.charset.StandardCharsets.UTF_8));
                    while(matcher.find())objectives.add(matcher.group(1));
                }
            } catch(Exception error){throw new RuntimeException(error);}
            objectives.addAll(List.of("ggdadv","ggdRoleSetting","carrotrightclick","gamesetting"));
            for(String name:objectives)if(srv.getScoreboard().getObjective(name)==null)command(player,"scoreboard objectives add "+name+" dummy");
            for(String setting:List.of("FullBloodDLC 1","DLCVisionFog 0","AstralDuration 50","SniperDuration 50","MimeDuration 10","EsperDuration 10"))
                command(player,"scoreboard players set "+setting.split(" ")[0]+" ggdadv "+setting.split(" ")[1]);
            for(int x=-8;x<=44;x++)for(int z=-8;z<=12;z++)player.level().setBlockAndUpdate(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState());
            var profile=new GameProfile(UUID.randomUUID(),"RetainedObserver");
            var fake=new ServerPlayer(srv,player.level(),profile,ClientInformation.createDefault());
            fake.connection=new ServerGamePacketListenerImpl(srv,new Connection(PacketFlow.SERVERBOUND),fake,CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener) {}
            };
            srv.getPlayerList().getPlayers().add(fake);srv.getPlayerList().getPlayersByUUID().put(fake.getUUID(),fake);
            player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fake)));
            player.level().addNewPlayer(fake);fakeId=fake.getUUID();
            var walkerProfile=new GameProfile(UUID.randomUUID(),"ProjectionWalker");
            var walker=new ServerPlayer(srv,player.level(),walkerProfile,ClientInformation.createDefault());
            walker.connection=new ServerGamePacketListenerImpl(srv,new Connection(PacketFlow.SERVERBOUND),walker,
                    CommonListenerCookie.createInitial(walkerProfile,false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener) {}
            };
            walker.absSnapTo(14.5,81,4.5,90,0);
            srv.getPlayerList().getPlayers().add(walker);srv.getPlayerList().getPlayersByUUID().put(walker.getUUID(),walker);
            player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(walker)));
            player.level().addNewPlayer(walker);walkerId=walker.getUUID();walkerEntityId=walker.getId();
            try {
                Field verified=MandatoryHandshake.class.getDeclaredField("VERIFIED");verified.setAccessible(true);
                ((Set<UUID>)verified.get(null)).add(fakeId);
            }catch(Exception error){throw new RuntimeException(error);}
        },srv);
    }

    private void begin(ServerPlayer local) {
        ServerPlayer fake=local.level().getServer().getPlayerList().getPlayer(fakeId);
        for(ServerPlayer p:List.of(local,fake)) {
            command(p,"goosetools projection abandon @s");
            for(String tag:List.copyOf(p.entityTags()))p.removeTag(tag);
            for(String tag:List.of("players","gamingGGD","good"))p.addTag(tag);
            p.setGameMode(GameType.ADVENTURE);p.setHealth(p.getMaxHealth());p.noPhysics=false;
            p.getInventory().clearContent();
        }
        local.teleportTo(local.level(),.5,81,ownerView?.5:6.5,Set.of(),180,0,false);
        fake.teleportTo(fake.level(),ownerView?8.5:.5,81,ownerView?6.5:.5,Set.of(),0,0,false);
        local.addTag("p1");fake.addTag("p2");
        ServerPlayer source=source(local),target=source==local?fake:local;
        entry=source.position();sourceId=source.getId();
        source.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.GOLDEN_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        int role=scenario%4;source.addTag(TAGS[role]);
        if(scenario>=8){source.addTag("Seagull");source.addTag("seagullHasBorrow");}
        for(String objective:List.of("ggdAstralCD","ggdSniperCD","ggdMimeCD","ggdEsperCD"))command(source,"scoreboard players set @s "+objective+" 0");
        String action="open";
        if(role==2){source.addTag("esperWaiting");target.addTag(scenario>=8?"seagullEsperTarget":"esperTarget");action="begin_possession";}
        if(role==3){source.addTag("mimeArmed");target.addTag("mimeMarked");action="begin_control";}
        command(source,"function ggd:skill/"+ROLES[role]+"/"+action);
        require(ProjectionBodyServer.isRetained(source),"actual role entry did not retain its body: "+ROLES[role]);
        require(DreamAvatarServer.actor(source)!=null,"actual role entry did not create its remote actor");
    }

    private void separate(ServerPlayer local) {
        ServerPlayer source=source(local),target=source==local?local.level().getServer().getPlayerList().getPlayer(fakeId):local;
        if(scenario%4==2)target.teleportTo(target.level(),20.5,81,.5,Set.of(),90,0,false);
        else DreamAvatarServer.move(source,source.level(),new Vec3(20.5,81,.5),90,0);
    }

    private void verifyServer(ServerPlayer local) {
        ServerPlayer source=source(local);Entity actor=DreamAvatarServer.actor(source);
        require(actor!=null && actor.position().distanceTo(source.position())>10,"body followed the remote actor");
        require(source.position().distanceTo(entry)<.2,"stationary body left its entry point");
        require(source.getId()==sourceId && source.level().getEntity(sourceId)==source,"native player body was replaced");
        require(source.gameMode()==GameType.ADVENTURE && !source.isInvisible(),"real body became spectator/invisible");
        require(source.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE),"body inherited target armour");
        require(DreamSpatialContext.runFor(source,()->source.getX())==actor.getX(),"executor did not use remote origin");
        ServerPlayer observer=source==local?local.level().getServer().getPlayerList().getPlayer(fakeId):local;
        require(DreamSpatialContext.runFor(observer,()->source.getX())==source.getX(),"observer targeting used the soul rather than body");
        require(ProjectionBodyServer.visualAnchor(source).position().distanceToSqr(source.position())<.01,"hit marker did not match physical body");
        try {
            Object voice=Class.forName("com.goosethings.projection.ProjectionAvatarBridge").getMethod("voiceAnchor",UUID.class).invoke(null,source.getUUID());
            require(voice!=null && ((Vec3)voice.getClass().getMethod("position").invoke(voice)).distanceTo(actor.position())<1,
                    "voice snapshot remained at the physical body");
        } catch(Exception error){throw new RuntimeException(error);}
        if(scenario%4==1) {
            command(source,"function ggd:skill/"+(scenario>=8?"seagull/":"")+"sniper/update_crosshair");
            Entity marker=null;
            for(Entity candidate:source.level().getAllEntities())if(candidate.entityTags().contains("sniperCrosshair")){marker=candidate;break;}
            require(marker!=null && Math.abs(marker.getX()-actor.getX())<.01 && Math.abs(marker.getZ()-actor.getZ())<.01,
                    "crosshair followed the retained body instead of soul");
        }
        if(scenario%4==3)DreamAvatarServer.move(source,source.level(),actor.position().add(.3,0,0),actor.getYRot(),actor.getXRot());
    }

    private void verifyClient(Minecraft mc) {
        if(ownerView) {
            require(DreamAvatarClient.retainedProjection() && mc.getCameraEntity()!=mc.player,"owner camera never left physical body");
            for(float partial:new float[]{0,.5F,1}) {
                var body=(AvatarRenderState)mc.getEntityRenderDispatcher().extractEntity(mc.player,partial);
                require(body.walkAnimationSpeed==0 && mc.player.walkAnimation.speed(partial)==0,
                        "stationary own body kept walking at partial tick "+partial+": "+body.walkAnimationSpeed);
            }
            require(mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE),"owner body armour changed in the client");
        }
        else {
            Entity body=mc.level.getEntity(sourceId);
            require(body instanceof RemotePlayer,"observer lost the real remote-player entity");
            require(((RemotePlayer)body).getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE),"observer's tracked body armour was stale");
            require(!ProjectionBodyClient.shouldSuppressBody(body) && !ProjectionBodyClient.shouldSuppressSource(body),"observer body was suppressed");
            var state=(AvatarRenderState)mc.getEntityRenderDispatcher().extractEntity(body,1);
            require(!state.isInvisible && !state.isInvisibleToPlayer && !state.isSpectator,"observer real body did not have a visible full render state");
        }
    }

    private void end(ServerPlayer local) {
        ServerPlayer source=source(local);
        if(scenario==4)source.addTag("spectator");
        if(scenario==7) {
            try {Class.forName("com.goosethings.mime.MimeManager").getMethod("onDisconnect",ServerPlayer.class,
                    net.minecraft.server.MinecraftServer.class).invoke(null,source,source.level().getServer());}
            catch(Exception error){throw new RuntimeException(error);}
            ProjectionBodyServer.abandon(source.level().getServer(),source);return;
        }
        if(scenario==5){source.addTag("inPelican");source.teleportTo(source.level(),-4.5,81,.5,Set.of(),0,0,false);}
        if(scenario==6){source.addTag("inTalk");source.teleportTo(source.level(),-4.5,81,.5,Set.of(),0,0,false);}
        String action=switch(scenario%4){case 0->"close";case 1->"close";case 2->"abort";default->"finish";};
        command(source,"function ggd:skill/"+ROLES[scenario%4]+"/"+action);
        if(scenario==5 || scenario==6)require(Math.abs(source.getX()+4.5)<.01,"cleanup teleported a swallowed/seated real body backwards");
    }

    private ServerPlayer source(ServerPlayer local) {return ownerView?local:local.level().getServer().getPlayerList().getPlayer(fakeId);}
    private void server(Minecraft mc,Consumer<ServerPlayer> operation) {
        var srv=DreamTcpFixture.server(mc);UUID local=mc.player.getUUID();
        pending=CompletableFuture.runAsync(()->operation.accept(srv.getPlayerList().getPlayer(local)),srv);
    }
    private static void command(ServerPlayer player,String command) {
        var srv=player.level().getServer();CommandSourceStack source=srv.createCommandSourceStack()
                .withEntity(player).withLevel(player.level()).withPosition(player.position()).withSuppressedOutput();
        srv.getCommands().performPrefixedCommand(source,command);
    }
    private void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private void finish(Minecraft mc,String result) {
        finished=true;
        suspendBodyTicks=false;
        mc.options.keyUp.setDown(false);DreamTestLatency.enabled=false;
        try{Files.writeString(mc.gameDirectory.toPath().resolve("retained-result.txt"),result);}catch(Exception error){GooseTools.LOGGER.error("Cannot write retained result",error);}
        DreamTcpFixture.stop();mc.stop();
    }
}
