package com.goosethings.tools.dream;

import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.projection.ProjectionBodyServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;

/** Shared remote-avatar physics. The authenticated player's visible body stays in the world. */
public final class DreamAvatarServer {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final TicketType VIEW_TICKET = new TicketType(40L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
    private static final TicketType BODY_TICKET = new TicketType(40L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
    private static long nextSession;
    private DreamAvatarServer() {}
    public static void register() {
        DreamAvatarPackets.register();
        ServerPlayNetworking.registerGlobalReceiver(DreamAvatarPackets.Input.TYPE, (p,c) ->
                c.server().execute(() -> input(c.player(), p)));
        ServerPlayNetworking.registerGlobalReceiver(DreamAvatarPackets.Ready.TYPE, (p,c) ->
                c.server().execute(() -> {
                    Session s=SESSIONS.get(c.player().getUUID());
                    if(s!=null && s.id==p.session())s.ready=true;
                }));
        ServerTickEvents.END_SERVER_TICK.register(DreamAvatarServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> end(h.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> SESSIONS.clear());
    }
    public static boolean begin(ServerPlayer owner, ServerLevel level, Vec3 position,
                                float yaw, float pitch, boolean raven) {
        if(SESSIONS.containsKey(owner.getUUID())
                || !MandatoryHandshake.isVerified(owner) || owner.getVehicle()==null
                || !DreamStandInServer.isPrepared(owner, raven)) return false;
        return begin(owner, level, position, yaw, pitch, raven, -1);
    }

    public static boolean beginObserver(ServerPlayer owner, ServerLevel level, Vec3 position, float yaw, float pitch) {
        if (SESSIONS.containsKey(owner.getUUID()) || !MandatoryHandshake.isVerified(owner)
                || !DreamStandInServer.observerPrepared(owner)) return false;
        return begin(owner, level, position, yaw, pitch, true, ObserverDreamPolicy.VIEW_KIND);
    }

    public static boolean observing(ServerPlayer owner) {
        Session session = SESSIONS.get(owner.getUUID());
        return session != null && session.projectionKind == ObserverDreamPolicy.VIEW_KIND;
    }

    /** Round skills retain their real, interactable player instead of publishing a body clone. */
    public static boolean beginProjection(ServerPlayer owner, ProjectionBodyServer.Kind kind) {
        if (kind == null || kind.isDream() || SESSIONS.containsKey(owner.getUUID())
                || !MandatoryHandshake.isVerified(owner)) return false;
        return begin(owner, owner.level(), owner.position(), owner.getYRot(), owner.getXRot(),
                kind == ProjectionBodyServer.Kind.ASTRAL || kind == ProjectionBodyServer.Kind.SNIPER,
                kind.networkId());
    }

    private static boolean begin(ServerPlayer owner, ServerLevel level, Vec3 position,
                                 float yaw, float pitch, boolean raven, int projectionKind) {
        Session session=new Session(++nextSession, owner, new Body(level, owner, raven, projectionKind), raven, projectionKind);
        session.body.absSnapTo(position.x,position.y,position.z,yaw,pitch);
        session.body.setOldPosAndRot();
        session.body.setYHeadRot(yaw);session.body.setYBodyRot(yaw);
        session.lastSafe = position;
        DreamChunkTracking meetingTracker = (DreamChunkTracking)(Object)owner.level().getChunkSource().chunkMap;
        meetingTracker.dream$removeViewer(owner);
        SESSIONS.put(owner.getUUID(),session);
        if (level != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                owner.createCommonSpawnInfo(level), (byte)3));
        session.viewer.connection = owner.connection;
        session.viewer.absSnapTo(position.x, position.y, position.z, yaw, pitch);
        ((DreamChunkTracking)(Object)level.getChunkSource().chunkMap).dream$addViewer(session.viewer, true);
        track(session);
        if (level != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                net.minecraft.network.protocol.game.ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0));
        ServerPlayNetworking.send(owner,new DreamAvatarPackets.View(session.id,true,position.x,position.y,position.z,raven,projectionKind));
        DreamStandInServer.avatarChanged();
        return true;
    }
    public static Entity actor(Entity owner) {
        if(!(owner instanceof ServerPlayer))return null;
        Session s=SESSIONS.get(owner.getUUID());return s==null?null:s.body;
    }
    public static boolean active(ServerPlayer owner) { return SESSIONS.containsKey(owner.getUUID()); }
    public static boolean syncRetainedBody(ServerPlayer owner, Vec3 position, float yaw, float pitch,
                                          float bodyYaw, float headYaw, Pose pose, boolean onGround) {
        Session s=SESSIONS.get(owner.getUUID());
        if(s==null || s.projectionKind<0 || !ServerPlayNetworking.canSend(owner,DreamAvatarPackets.Body.TYPE))return false;
        ServerPlayNetworking.send(owner,new DreamAvatarPackets.Body(s.id,owner.level().getServer().getTickCount(),
                position,yaw,pitch,bodyYaw,headYaw,pose,onGround));
        return true;
    }
    public static boolean retainsProjectionBody(ServerPlayer owner) {
        Session session=SESSIONS.get(owner.getUUID());return session!=null && session.projectionKind>=0;
    }
    public static int projectionKind(ServerPlayer owner) {
        Session session=SESSIONS.get(owner.getUUID());return session==null?-1:session.projectionKind;
    }
    public static UUID projectionTarget(ServerPlayer owner) {
        Session session=SESSIONS.get(owner.getUUID());return session==null?null:session.targetId;
    }
    /** Changes only the remote actor; the real body remains tracked at its own position. */
    public static boolean followProjection(ServerPlayer owner, ServerPlayer target) {
        Session session=SESSIONS.get(owner.getUUID());
        if(session==null || session.projectionKind<2 || target==owner || target.level()!=owner.level())return false;
        session.targetId=target.getUUID();
        return move(owner,target.level(),target.position(),target.getYRot(),target.getXRot());
    }
    public static boolean isSeatedOwner(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.owner == player;
    }
    public static Pose meetingPose(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null || session.owner != player || session.projectionKind>=0 ? null
                : player.entityTags().contains("dreamRemote") ? session.body.getPose() : session.meetingPose;
    }
    /** The connection remains authenticated as the owner; only chunk source and priority use the viewer. */
    public static ServerPlayer chunkViewer(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.owner == player ? session.viewer : player;
    }
    public static Entity owner(Entity entity) {
        return entity instanceof Body body ? body.owner : entity;
    }
    /** A detached player supplies vanilla vehicle steering; it is never mounted or spawned. */
    public static ServerPlayer vehicleController(Entity passenger) {
        if (!(passenger instanceof Body body)) return null;
        Session session = SESSIONS.get(body.owner.getUUID());
        return session == null ? null : session.viewer;
    }
    public static boolean swing(ServerPlayer owner, net.minecraft.world.InteractionHand hand,
                             net.minecraft.world.item.component.SwingAnimation animation, boolean broadcast) {
        Session session = SESSIONS.get(owner.getUUID());
        return session != null && session.body.swing(hand, animation, broadcast);
    }
    public static boolean ride(ServerPlayer owner, Entity vehicle, boolean force) {
        Session session = SESSIONS.get(owner.getUUID());
        return session != null && DreamSpatialContext.run(false,
                () -> session.body.startRiding(vehicle, force, false));
    }
    public static void dismount(ServerPlayer owner) {
        Session session = SESSIONS.get(owner.getUUID());
        if (session != null) DreamSpatialContext.run(false, () -> {
            session.body.stopRiding();
            return null;
        });
    }
    public static Entity end(ServerPlayer owner) {
        owner.getAdvancements().setPlayer(owner);
        if(!retainsProjectionBody(owner) && !observing(owner))DreamStandInServer.retainAvatarSnapshot(owner);
        Session s=SESSIONS.remove(owner.getUUID());
        if(s==null)return null;
        if (s.projectionKind == ObserverDreamPolicy.VIEW_KIND) owner.removeTag("spectatorDream");
        if(s.projectionKind<0)owner.setPose(s.meetingPose);
        s.body.stopRiding();
        if(s.chunk!=null) s.body.level().getChunkSource().removeTicketWithRadius(VIEW_TICKET,s.chunk,s.radius);
        if(s.bodyChunk!=null)owner.level().getChunkSource().removeTicketWithRadius(BODY_TICKET,s.bodyChunk,2);
        ((DreamChunkTracking)(Object)s.body.level().getChunkSource().chunkMap).dream$removeViewer(s.viewer);
        if (s.body.level() != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                owner.createCommonSpawnInfo(owner.level()), (byte)3));
        ((DreamChunkTracking)(Object)owner.level().getChunkSource().chunkMap).dream$addViewer(owner, true);
        owner.level().getChunkSource().chunkMap.move(owner);
        if (s.body.level() != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                net.minecraft.network.protocol.game.ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0));
        if(owner.connection!=null && ServerPlayNetworking.canSend(owner,DreamAvatarPackets.View.TYPE))
            ServerPlayNetworking.send(owner,new DreamAvatarPackets.View(s.id,false,owner.getX(),owner.getY(),owner.getZ(),s.raven,s.projectionKind));
        DreamStandInServer.avatarChanged();
        return s.body;
    }
    public static boolean move(ServerPlayer owner, ServerLevel level, Vec3 position, float yaw, float pitch) {
        Session s=SESSIONS.get(owner.getUUID());
        if(s==null || level!=s.body.level() || !Double.isFinite(position.lengthSqr())
                || !level.getWorldBorder().isWithinBounds(position.x,position.z))return false;
        s.body.absSnapTo(position.x,position.y,position.z,yaw,pitch);
        s.body.setDeltaMovement(Vec3.ZERO);s.body.fallDistance=0;s.epoch++;track(s);return true;
    }
    private static void input(ServerPlayer owner,DreamAvatarPackets.Input input) {
        Session s=SESSIONS.get(owner.getUUID());
        if(s==null || !s.ready || input.session()!=s.id || input.sequence()<=s.receivedSequence
                || !MandatoryHandshake.isVerified(owner))return;
        s.receivedSequence=input.sequence();s.input=input;s.lastInput=owner.level().getServer().getTickCount();
        if(s.raven || s.predictsGround()) {
            if(s.pending.size()>=32)s.pending.removeFirst();
            s.pending.addLast(input);
        }
    }
    private static void tick(MinecraftServer server) {
        for(Session s:List.copyOf(SESSIONS.values())) {
            var tags=s.owner.entityTags();
            boolean observer = s.projectionKind == ObserverDreamPolicy.VIEW_KIND;
            boolean unavailable=s.owner.hasDisconnected() || (!observer && tags.contains("spectator")) || tags.contains("endGame");
            if (observer) unavailable |= !ObserverDreamPolicy.valid(tags, meetingPhase(server));
            else if(s.projectionKind<0)unavailable|=(!tags.contains("inDream") && server.getTickCount()-s.started>2)
                    || s.owner.getVehicle()==null || s.owner.getVehicle().isRemoved();
            else unavailable|=tags.contains("inTalk") || tags.contains("inPelican")
                    || !ProjectionBodyServer.isRetained(s.owner);
            if(unavailable) {if(observer)DreamStandInServer.returnObserver(s.owner);else end(s.owner);continue;}
            if(!s.ready && server.getTickCount()-s.started>200) {
                if (observer) {DreamStandInServer.returnObserver(s.owner);continue;}
                String function=s.projectionKind<0?"ggd:skill/dream/wake":"ggd:skill/"+
                        ProjectionBodyServer.kind(s.owner.getUUID()).commandName()+"/force_cleanup";
                server.getCommands().performPrefixedCommand(s.owner.createCommandSourceStack().withSuppressedOutput(),"function "+function);
                continue;
            }
            if(s.projectionKind==2) {
                ServerPlayer target=s.targetId==null?null:server.getPlayerList().getPlayer(s.targetId);
                if(target!=null && target.level()==s.body.level()) {
                    s.body.absSnapTo(target.getX(),target.getY(),target.getZ(),target.getYRot(),target.getXRot());
                    s.body.setPose(target.getPose());s.body.setDeltaMovement(Vec3.ZERO);
                    s.body.setYHeadRot(target.getYHeadRot());s.body.setYBodyRot(target.yBodyRot);
                }
                track(s);DreamStandInServer.avatarChanged();continue;
            }
            track(s);
            if(!s.ready || !s.body.level().hasChunkAt(s.body.blockPosition()))continue;
            var input=s.raven || s.predictsGround()?s.pending.pollFirst():server.getTickCount()-s.lastInput<=5?s.input:null;
            // Consume each predicted input once, at most one per server tick. Repeating
            // or skipping batched inputs breaks client replay when the network jitters.
            if(s.raven && input==null) {sendFlight(s);continue;}
            if(s.predictsGround() && input==null) {sendGround(s);continue;}
            if(input!=null)s.sequence=input.sequence();
            if(input!=null) {
                s.body.setYRot(input.yaw());s.body.setXRot(input.pitch());
                s.body.setYHeadRot(input.yaw());s.body.setYBodyRot(input.yaw());
            }
            float side=input==null?0:input.sideways(),forward=input==null?0:input.forward();
            int buttons=input==null?0:input.buttons();
            s.viewer.setYRot(s.body.getYRot());s.viewer.setXRot(s.body.getXRot());
            s.viewer.xxa=side;s.viewer.zza=forward;
            s.viewer.setSprinting((buttons&4)!=0);
            s.viewer.setLastClientInput(new net.minecraft.world.entity.player.Input(
                    forward>0,forward<0,side>0,side<0,(buttons&1)!=0,(buttons&2)!=0,(buttons&4)!=0));
            s.body.setPose((buttons&2)!=0 && !s.raven?Pose.CROUCHING:Pose.STANDING);
            s.body.setSprinting((buttons&4)!=0);
            if(s.body.isPassenger()) {
                if ((buttons & 2) != 0) s.body.stopRiding();
                else {
                    if (s.body.getVehicle() instanceof net.minecraft.world.entity.PlayerRideableJumping mount && mount.canJump()) {
                        if ((buttons & 1)!=0) s.jumpCharge=Math.min(10,s.jumpCharge+1);
                        else if (s.jumpCharge>0) {
                            int charge=s.jumpCharge*9;
                            mount.onPlayerJump(charge);mount.handleStartJump(charge);
                            s.jumpCharge=0;
                        }
                    }
                    s.body.rideTick();
                }
            } else if(s.raven) {
                s.jumpCharge=0;
                s.body.xxa=side;s.body.zza=forward;
                // The existing Raven multiplier is 2: vanilla .05 flying speed becomes .1.
                int vertical=((buttons&1)!=0?1:0)-((buttons&2)!=0?1:0);
                s.body.setDeltaMovement(s.body.getDeltaMovement().add(0,vertical*0.3*s.body.flightScale,0));
                s.body.tick();s.body.setOnGround(false);
            } else if(s.predictsGround()) {
                s.jumpCharge=0;
                s.body.stepGround(input,s.owner.getAttributeValue(Attributes.MOVEMENT_SPEED));
            } else {
                s.jumpCharge=0;
                s.body.xxa=side*((buttons&2)!=0?0.3F:1);s.body.zza=forward*((buttons&2)!=0?0.3F:1);
                s.body.setJumping((buttons&1)!=0);
                s.body.setSpeed((float)s.owner.getAttributeValue(Attributes.MOVEMENT_SPEED)*((buttons&4)!=0?1.3F:1));
                s.body.tick();
            }
            if(!s.body.level().getWorldBorder().isWithinBounds(s.body.getBoundingBox())) {
                s.body.absSnapTo(s.lastSafe.x,s.lastSafe.y,s.lastSafe.z);s.body.setDeltaMovement(Vec3.ZERO);
                s.epoch++;
            } else s.lastSafe=s.body.position();
            if(tags.contains("dreamRemote")) {
                s.owner.setYRot(s.body.getYRot());s.owner.setXRot(s.body.getXRot());
                s.owner.setYBodyRot(s.body.yBodyRot);s.owner.setYHeadRot(s.body.getYHeadRot());
            }
            if(s.projectionKind<0)s.owner.setPose(tags.contains("dreamRemote") ? s.body.getPose() : s.meetingPose);
            if(s.raven)sendFlight(s);
            if(s.predictsGround())sendGround(s);
            // Scene samples preserve a moving player body rather than a hidden camera.
            DreamStandInServer.avatarChanged();
        }
    }
    private static int meetingPhase(MinecraftServer server) {
        var objective = server.getScoreboard().getObjective("ggdSession");
        var value = objective == null ? null : server.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("#MeetingPhase"), objective);
        return value == null ? -1 : value.value();
    }
    private static void sendGround(Session s) {
        ServerPlayNetworking.send(s.owner,new DreamAvatarPackets.Ground(s.id,
                s.owner.level().getServer().getTickCount(),s.sequence,s.epoch,
                s.body.position(),s.body.getDeltaMovement(),s.body.onGround(),!s.body.isPassenger(),
                s.owner.getAttributeValue(Attributes.MOVEMENT_SPEED),s.body.getAttributeValue(Attributes.STEP_HEIGHT),
                ((DreamJumpState)(Object)s.body).dream$getJumpDelay(),s.body.fallDistance));
    }
    private static void sendFlight(Session s) {
        ServerPlayNetworking.send(s.owner,new DreamAvatarPackets.Flight(s.id,
                s.owner.level().getServer().getTickCount(),s.sequence,s.epoch,
                s.body.position(),s.body.getDeltaMovement(),!s.body.isPassenger()));
    }
    private static void track(Session s) {
        if(s.projectionKind>=0) {
            ChunkPos bodyChunk=s.owner.chunkPosition();
            if(s.bodyChunk!=null && !s.bodyChunk.equals(bodyChunk))
                s.owner.level().getChunkSource().removeTicketWithRadius(BODY_TICKET,s.bodyChunk,2);
            s.bodyChunk=bodyChunk;
            s.owner.level().getChunkSource().addTicketWithRadius(BODY_TICKET,bodyChunk,2);
        }
        ChunkPos center=s.body.chunkPosition();
        int radius=Math.min(12,Math.max(3,s.owner.requestedViewDistance()))+1;
        if(s.chunk!=null && (!s.chunk.equals(center)||s.radius!=radius))
            s.body.level().getChunkSource().removeTicketWithRadius(VIEW_TICKET,s.chunk,s.radius);
        s.chunk=center;s.radius=radius;
        s.body.level().getChunkSource().addTicketWithRadius(VIEW_TICKET,center,radius);
        s.viewer.absSnapTo(s.body.getX(), s.body.getY(), s.body.getZ(), s.body.getYRot(), s.body.getXRot());
        ((DreamChunkTracking)(Object)s.body.level().getChunkSource().chunkMap).dream$updateChunkTracking(s.viewer);
        s.body.level().getChunkSource().chunkMap.move(s.viewer);
    }
    private static final class Session {
        final long id;final ServerPlayer owner;final Body body; final ServerPlayer viewer; final boolean raven;final int started;
        final Pose meetingPose;final int projectionKind;UUID targetId;
        long sequence=-1,receivedSequence=-1,epoch;int lastInput=-100,jumpCharge;boolean ready;DreamAvatarPackets.Input input;
        final ArrayDeque<DreamAvatarPackets.Input> pending=new ArrayDeque<>();
        ChunkPos chunk,bodyChunk;int radius;Vec3 lastSafe;
        boolean predictsGround() {return !raven && (projectionKind<0 || projectionKind==3);}
        Session(long id,ServerPlayer owner,Body body,boolean raven,int projectionKind) {
            this.id=id;this.owner=owner;this.body=body;this.raven=raven;this.projectionKind=projectionKind;
            meetingPose = owner.getPose();
            viewer = new ViewPlayer(owner, body.level());
            started=owner.level().getServer().getTickCount();lastSafe=body.position();
        }
    }
    private static final class ViewPlayer extends ServerPlayer {
        ViewPlayer(ServerPlayer owner, ServerLevel level) {
            super(owner.level().getServer(), level, owner.getGameProfile(), owner.clientInformation());
            // PlayerList caches advancements by UUID; super binds their rewards to this viewer.
            // Inventory/skill rewards must keep executing as the authenticated meeting player.
            owner.getAdvancements().setPlayer(owner);
        }
        @Override public boolean isClientAuthoritative() { return false; }
    }
    /** Detached vanilla humanoid physics: never spawned, tracked or saved as a visible mannequin. */
    private static final class Body extends DreamGroundBody {
        final ServerPlayer owner;final boolean raven;final float flightScale;final int projectionKind;
        Body(ServerLevel level,ServerPlayer owner,boolean raven,int projectionKind) {
            super(level);this.owner=owner;this.raven=raven;noPhysics=raven;setNoGravity(raven);
            this.projectionKind=projectionKind;
            flightScale=projectionKind==0?0.2F:projectionKind==1?0.25F:1.0F;
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(owner.getAttributeValue(Attributes.MOVEMENT_SPEED));
            getAttribute(Attributes.STEP_HEIGHT).setBaseValue(owner.getAttributeValue(Attributes.STEP_HEIGHT));
        }
        @Override public ServerLevel level() { return (ServerLevel)super.level(); }
        @Override public void travel(Vec3 input) {
            if (!raven) {super.travel(input);return;}
            // Match native flying acceleration and drag, without floor/fluid slowdown for noclip.
            var motion=DreamFlightMotion.travel(position(),getDeltaMovement(),input,getYRot(),isSprinting(),flightScale);
            move(net.minecraft.world.entity.MoverType.SELF,motion.position().subtract(position()));
            setDeltaMovement(motion.velocity());
        }
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
            if(raven || projectionKind>=0)return false;
            return DreamSpatialContext.run(false,()->owner.hurtServer(level,source,amount));
        }
        @Override public boolean isPushable() {return false;}
        @Override public boolean isAttackable() {return false;}
        @Override public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output) { return false; }
        @Override protected void playStepSound(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState b) {}
    }
}
