package com.goosethings.tools.dream;
import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public final class DreamAvatarPackets {
 private DreamAvatarPackets() {}
 private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID,name));}
 public static void register() {
  PayloadTypeRegistry.clientboundPlay().register(View.TYPE,View.CODEC);
  PayloadTypeRegistry.clientboundPlay().register(Flight.TYPE,Flight.CODEC);
  PayloadTypeRegistry.clientboundPlay().register(Ground.TYPE,Ground.CODEC);
  PayloadTypeRegistry.clientboundPlay().register(Body.TYPE,Body.CODEC);
  PayloadTypeRegistry.serverboundPlay().register(Input.TYPE,Input.CODEC);
  PayloadTypeRegistry.serverboundPlay().register(Ready.TYPE,Ready.CODEC);
 }
 public record View(long session,boolean active,double x,double y,double z,boolean raven,int projectionKind) implements CustomPacketPayload {
  public View(long session,boolean active,double x,double y,double z,boolean raven) {this(session,active,x,y,z,raven,-1);}
  public static final Type<View> TYPE=DreamAvatarPackets.type("dream_avatar_view_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeBoolean(p.active);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeBoolean(p.raven);b.writeVarInt(p.projectionKind);},
   b->new View(b.readLong(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean(),b.readVarInt()));
  public View {if(session<=0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000||Math.abs(y)>20000||projectionKind< -1||projectionKind>3)throw new IllegalArgumentException("Invalid dream view");}
  public float flightScale(){return projectionKind==0?0.2F:projectionKind==1?0.25F:1.0F;}
  @Override public Type<View> type(){return TYPE;}
 }
 public record Input(long session,long sequence,float sideways,float forward,float yaw,float pitch,int buttons) implements CustomPacketPayload {
  public static final Type<Input> TYPE=DreamAvatarPackets.type("dream_avatar_input_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeLong(p.sequence);b.writeFloat(p.sideways);b.writeFloat(p.forward);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeVarInt(p.buttons);},
   b->new Input(b.readLong(),b.readLong(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readVarInt()));
  public Input {if(session<=0||sequence<0||!Float.isFinite(sideways)||Math.abs(sideways)>1||!Float.isFinite(forward)||Math.abs(forward)>1||!Float.isFinite(yaw)||Math.abs(yaw)>1000000||!Float.isFinite(pitch)||Math.abs(pitch)>90||(buttons&~15)!=0)throw new IllegalArgumentException("Invalid dream input");}
  @Override public Type<Input> type(){return TYPE;}
 }
 /** Authoritative retained physical body; separate from the independently controlled actor. */
 public record Body(long session,long tick,net.minecraft.world.phys.Vec3 position,
                    float yaw,float pitch,float bodyYaw,float headYaw,
                    net.minecraft.world.entity.Pose pose,boolean onGround) implements CustomPacketPayload {
  public static final Type<Body> TYPE=DreamAvatarPackets.type("dream_retained_body_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Body> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeLong(p.tick);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
    b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeFloat(p.bodyYaw);b.writeFloat(p.headYaw);b.writeEnum(p.pose);b.writeBoolean(p.onGround);},
   b->new Body(b.readLong(),b.readLong(),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
    b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readEnum(net.minecraft.world.entity.Pose.class),b.readBoolean()));
  public Body {
   if(session<=0||tick<0||position==null||!Double.isFinite(position.lengthSqr())
    ||Math.abs(position.x)>30000000||Math.abs(position.z)>30000000||Math.abs(position.y)>20000
    ||!Float.isFinite(yaw)||!Float.isFinite(pitch)||!Float.isFinite(bodyYaw)||!Float.isFinite(headYaw)||pose==null)
    throw new IllegalArgumentException("Invalid retained body state");
  }
  @Override public Type<Body> type(){return TYPE;}
 }
 public record Ready(long session) implements CustomPacketPayload {
  public Ready {if(session<=0)throw new IllegalArgumentException("Invalid dream ready session");}
  public static final Type<Ready> TYPE=DreamAvatarPackets.type("dream_avatar_ready_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Ready> CODEC=StreamCodec.of((b,p)->b.writeLong(p.session),b->new Ready(b.readLong()));
  @Override public Type<Ready> type(){return TYPE;}
 }
 public record Ground(long session,long tick,long sequence,long epoch,
                      net.minecraft.world.phys.Vec3 position,net.minecraft.world.phys.Vec3 velocity,
                      boolean onGround,boolean walking,double speed,double stepHeight,int jumpDelay,double fallDistance)
                      implements CustomPacketPayload {
  public static final Type<Ground> TYPE=DreamAvatarPackets.type("dream_avatar_ground_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Ground> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeLong(p.tick);b.writeLong(p.sequence);b.writeLong(p.epoch);
    b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
    b.writeDouble(p.velocity.x);b.writeDouble(p.velocity.y);b.writeDouble(p.velocity.z);
    b.writeBoolean(p.onGround);b.writeBoolean(p.walking);b.writeDouble(p.speed);b.writeDouble(p.stepHeight);
    b.writeVarInt(p.jumpDelay);b.writeDouble(p.fallDistance);},
   b->new Ground(b.readLong(),b.readLong(),b.readLong(),b.readLong(),
    new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
    new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
    b.readBoolean(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readDouble()));
  public Ground {
   if(session<=0||tick<0||sequence< -1||epoch<0||position==null||velocity==null
    ||!Double.isFinite(position.lengthSqr())||!Double.isFinite(velocity.lengthSqr())
    ||Math.abs(position.x)>30000000||Math.abs(position.z)>30000000||Math.abs(position.y)>20000
    ||!Double.isFinite(speed)||speed<0||speed>1024||!Double.isFinite(stepHeight)||stepHeight<0||stepHeight>64
    ||jumpDelay<0||jumpDelay>1000||!Double.isFinite(fallDistance)||fallDistance<0)
    throw new IllegalArgumentException("Invalid dream ground state");
  }
  @Override public Type<Ground> type(){return TYPE;}
 }
 public record Flight(long session,long tick,long sequence,long epoch,
                      net.minecraft.world.phys.Vec3 position,net.minecraft.world.phys.Vec3 velocity,
                      boolean flying) implements CustomPacketPayload {
  public static final Type<Flight> TYPE=DreamAvatarPackets.type("dream_avatar_flight_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Flight> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeLong(p.tick);b.writeLong(p.sequence);b.writeLong(p.epoch);
    b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
    b.writeDouble(p.velocity.x);b.writeDouble(p.velocity.y);b.writeDouble(p.velocity.z);b.writeBoolean(p.flying);},
   b->new Flight(b.readLong(),b.readLong(),b.readLong(),b.readLong(),
    new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
    new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readBoolean()));
  public Flight {
   if(session<=0||tick<0||sequence< -1||epoch<0||position==null||velocity==null
    ||!Double.isFinite(position.lengthSqr())||!Double.isFinite(velocity.lengthSqr())
    ||Math.abs(position.x)>30000000||Math.abs(position.z)>30000000||Math.abs(position.y)>20000)
    throw new IllegalArgumentException("Invalid dream flight state");
  }
  @Override public Type<Flight> type(){return TYPE;}
 }
}
