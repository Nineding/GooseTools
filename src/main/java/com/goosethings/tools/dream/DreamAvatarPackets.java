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
  PayloadTypeRegistry.serverboundPlay().register(Input.TYPE,Input.CODEC);
  PayloadTypeRegistry.serverboundPlay().register(Ready.TYPE,Ready.CODEC);
 }
 public record View(long session,boolean active,double x,double y,double z,boolean raven) implements CustomPacketPayload {
  public static final Type<View> TYPE=DreamAvatarPackets.type("dream_avatar_view_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.of(
   (b,p)->{b.writeLong(p.session);b.writeBoolean(p.active);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeBoolean(p.raven);},
   b->new View(b.readLong(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean()));
  public View {if(session<=0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000||Math.abs(y)>20000)throw new IllegalArgumentException("Invalid dream view");}
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
 public record Ready(long session) implements CustomPacketPayload {
  public Ready {if(session<=0)throw new IllegalArgumentException("Invalid dream ready session");}
  public static final Type<Ready> TYPE=DreamAvatarPackets.type("dream_avatar_ready_v1");
  public static final StreamCodec<RegistryFriendlyByteBuf,Ready> CODEC=StreamCodec.of((b,p)->b.writeLong(p.session),b->new Ready(b.readLong()));
  @Override public Type<Ready> type(){return TYPE;}
 }
}
