package com.goosethings.tools.task.profession;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProfessionPackets {
    private ProfessionPackets(){}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String name){
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID,"profession_"+name+"_v1"));
    }
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);PayloadTypeRegistry.serverboundPlay().register(Input.TYPE,Input.CODEC);}
    public record Input(long sessionId,int sequence,int stage,int action,int slot,double a,double b,double c) implements CustomPacketPayload {
        public static final Type<Input> TYPE=id("input");
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((buf,v)->{
            buf.writeLong(v.sessionId);buf.writeVarInt(v.sequence);buf.writeVarInt(v.stage);buf.writeVarInt(v.action);buf.writeVarInt(v.slot);
            buf.writeDouble(v.a);buf.writeDouble(v.b);buf.writeDouble(v.c);
        },buf->new Input(buf.readLong(),buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readDouble(),buf.readDouble(),buf.readDouble()));
        public boolean valid(){return sessionId>0&&sequence>=0&&sequence<=100_000&&stage>=0&&stage<4&&action>=0&&action<=2&&slot>=0&&slot<16
                &&Double.isFinite(a)&&Double.isFinite(b)&&Double.isFinite(c)&&Math.abs(a)<=1_000_000&&Math.abs(b)<=1_000_000&&Math.abs(c)<=1_000_000;}
        @Override public Type<Input> type(){return TYPE;}
    }
    public record State(long sessionId,ProfessionSnapshot state) implements CustomPacketPayload {
        public static final Type<State> TYPE=id("state");
        public State{if(sessionId<=0||state==null)throw new IllegalArgumentException("Invalid profession envelope");}
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((buf,v)->{
            buf.writeLong(v.sessionId);var s=v.state;buf.writeVarInt(s.stage());buf.writeVarInt(s.feedback());buf.writeVarInt(s.errors());buf.writeBoolean(s.started());
            buf.writeLong(s.elapsed());buf.writeLong(s.event());buf.writeLong(s.stable());buf.writeLong(s.testElapsed());buf.writeDouble(s.temperature());
            for(int p:s.choices())buf.writeVarInt(p);for(double d:s.dials())buf.writeDouble(d);
        },buf->{long id=buf.readLong();int stage=buf.readVarInt(),feedback=buf.readVarInt(),errors=buf.readVarInt();boolean started=buf.readBoolean();
            long elapsed=buf.readLong(),event=buf.readLong(),stable=buf.readLong(),test=buf.readLong();double temperature=buf.readDouble();
            int[] p=new int[16];double[] d=new double[16];for(int i=0;i<16;i++)p[i]=buf.readVarInt();for(int i=0;i<16;i++)d[i]=buf.readDouble();
            return new State(id,new ProfessionSnapshot(stage,feedback,errors,started,elapsed,event,stable,test,temperature,p,d));});
        @Override public Type<State> type(){return TYPE;}
    }
}
