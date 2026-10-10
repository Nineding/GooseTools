package com.goosethings.tools.dream;

import com.goosethings.tools.network.MandatoryHandshake;
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
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Independent dream physics. The authenticated player never leaves their meeting chair. */
public final class DreamAvatarServer {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final TicketType VIEW_TICKET = new TicketType(40L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
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
        Session session=new Session(++nextSession, owner, new Body(level, owner, raven), raven);
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
        ServerPlayNetworking.send(owner,new DreamAvatarPackets.View(session.id,true,position.x,position.y,position.z,raven));
        return true;
    }
    public static Entity actor(Entity owner) {
        if(!(owner instanceof ServerPlayer))return null;
        Session s=SESSIONS.get(owner.getUUID());return s==null?null:s.body;
    }
    public static boolean active(ServerPlayer owner) { return SESSIONS.containsKey(owner.getUUID()); }
    public static boolean isSeatedOwner(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.owner == player;
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
        DreamStandInServer.retainAvatarSnapshot(owner);
        Session s=SESSIONS.remove(owner.getUUID());
        if(s==null)return null;
        s.body.stopRiding();
        if(s.chunk!=null) s.body.level().getChunkSource().removeTicketWithRadius(VIEW_TICKET,s.chunk,s.radius);
        ((DreamChunkTracking)(Object)s.body.level().getChunkSource().chunkMap).dream$removeViewer(s.viewer);
        if (s.body.level() != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                owner.createCommonSpawnInfo(owner.level()), (byte)3));
        ((DreamChunkTracking)(Object)owner.level().getChunkSource().chunkMap).dream$addViewer(owner, true);
        owner.level().getChunkSource().chunkMap.move(owner);
        if (s.body.level() != owner.level()) owner.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                net.minecraft.network.protocol.game.ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0));
        if(owner.connection!=null && ServerPlayNetworking.canSend(owner,DreamAvatarPackets.View.TYPE))
            ServerPlayNetworking.send(owner,new DreamAvatarPackets.View(s.id,false,owner.getX(),owner.getY(),owner.getZ(),s.raven));
        return s.body;
    }
    public static boolean move(ServerPlayer owner, ServerLevel level, Vec3 position, float yaw, float pitch) {
        Session s=SESSIONS.get(owner.getUUID());
        if(s==null || level!=s.body.level() || !Double.isFinite(position.lengthSqr())
                || !level.getWorldBorder().isWithinBounds(position.x,position.z))return false;
        s.body.absSnapTo(position.x,position.y,position.z,yaw,pitch);
        s.body.setDeltaMovement(Vec3.ZERO);s.body.fallDistance=0;track(s);return true;
    }
    private static void input(ServerPlayer owner,DreamAvatarPackets.Input input) {
        Session s=SESSIONS.get(owner.getUUID());
        if(s==null || !s.ready || input.session()!=s.id || input.sequence()<=s.sequence
                || !MandatoryHandshake.isVerified(owner))return;
        s.sequence=input.sequence();s.input=input;s.lastInput=owner.level().getServer().getTickCount();
    }
    private static void tick(MinecraftServer server) {
        for(Session s:List.copyOf(SESSIONS.values())) {
            var tags=s.owner.entityTags();
            if(s.owner.hasDisconnected() || tags.contains("spectator") || tags.contains("endGame")
                    || !tags.contains("inDream") && server.getTickCount()-s.started>2
                    || s.owner.getVehicle()==null || s.owner.getVehicle().isRemoved()) {end(s.owner);continue;}
            if(!s.ready && server.getTickCount()-s.started>200) {
                server.getCommands().performPrefixedCommand(s.owner.createCommandSourceStack().withSuppressedOutput(),"function ggd:skill/dream/wake");
                continue;
            }
            track(s);
            if(!s.ready || !s.body.level().hasChunkAt(s.body.blockPosition()))continue;
            var input=server.getTickCount()-s.lastInput<=5?s.input:null;
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
            s.body.setSprinting((buttons&4)!=0 && !s.raven);
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
                double yaw=Math.toRadians(s.body.getYRot());
                Vec3 direction=new Vec3(side*Math.cos(yaw)-forward*Math.sin(yaw),
                        ((buttons&1)!=0?1:0)-((buttons&2)!=0?1:0),side*Math.sin(yaw)+forward*Math.cos(yaw));
                if(direction.lengthSqr()>1)direction=direction.normalize();
                s.body.setDeltaMovement(direction.scale(0.2));
                s.body.tick();s.body.setOnGround(false);
            } else {
                s.jumpCharge=0;
                s.body.xxa=side*((buttons&2)!=0?0.3F:1);s.body.zza=forward*((buttons&2)!=0?0.3F:1);
                s.body.setJumping((buttons&1)!=0);
                s.body.setSpeed((float)s.owner.getAttributeValue(Attributes.MOVEMENT_SPEED)*((buttons&4)!=0?1.3F:1));
                s.body.tick();
            }
            if(!s.body.level().getWorldBorder().isWithinBounds(s.body.getBoundingBox())) {
                s.body.absSnapTo(s.lastSafe.x,s.lastSafe.y,s.lastSafe.z);s.body.setDeltaMovement(Vec3.ZERO);
            } else s.lastSafe=s.body.position();
            if(tags.contains("dreamRemote")) {
                s.owner.setYRot(s.body.getYRot());s.owner.setXRot(s.body.getXRot());
                s.owner.setYBodyRot(s.body.yBodyRot);s.owner.setYHeadRot(s.body.getYHeadRot());
            }
            // Scene samples preserve a moving player body rather than a hidden camera.
            DreamStandInServer.avatarChanged();
        }
    }
    private static void track(Session s) {
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
        long sequence=-1;int lastInput=-100,jumpCharge;boolean ready;DreamAvatarPackets.Input input;
        ChunkPos chunk;int radius;Vec3 lastSafe;
        Session(long id,ServerPlayer owner,Body body,boolean raven) {
            this.id=id;this.owner=owner;this.body=body;this.raven=raven;
            viewer = new ViewPlayer(owner, body.level());
            started=owner.level().getServer().getTickCount();lastSafe=body.position();
        }
    }
    private static final class ViewPlayer extends ServerPlayer {
        ViewPlayer(ServerPlayer owner, ServerLevel level) {
            super(owner.level().getServer(), level, owner.getGameProfile(), owner.clientInformation());
        }
        @Override public boolean isClientAuthoritative() { return false; }
    }
    /** Detached vanilla humanoid physics: never spawned, tracked or saved as a visible mannequin. */
    private static final class Body extends Mannequin {
        final ServerPlayer owner;final boolean raven;
        Body(ServerLevel level,ServerPlayer owner,boolean raven) {
            super(level);this.owner=owner;this.raven=raven;noPhysics=raven;setNoGravity(raven);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(owner.getAttributeValue(Attributes.MOVEMENT_SPEED));
            getAttribute(Attributes.STEP_HEIGHT).setBaseValue(owner.getAttributeValue(Attributes.STEP_HEIGHT));
        }
        @Override public ServerLevel level() { return (ServerLevel)super.level(); }
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
            if(raven)return false;
            return DreamSpatialContext.run(false,()->owner.hurtServer(level,source,amount));
        }
        @Override public boolean isPushable() {return false;}
        @Override public boolean isAttackable() {return false;}
        @Override public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output) { return false; }
        @Override protected void playStepSound(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState b) {}
    }
}
