package com.goosethings.tools.task;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class PowerStationPackets {
    private PowerStationPackets(){}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String name){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID,"station_"+name+"_v1"));}
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);PayloadTypeRegistry.serverboundPlay().register(Input.TYPE,Input.CODEC);}
    public record Input(long sessionId,int sequence,int action,int item,double a,double b) implements CustomPacketPayload {
        public static final Type<Input> TYPE=id("input");
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((b,v)->{b.writeLong(v.sessionId);b.writeVarInt(v.sequence);b.writeVarInt(v.action);b.writeVarInt(v.item);b.writeDouble(v.a);b.writeDouble(v.b);},
                b->new Input(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble()));
        public boolean valid(){return sessionId>0&&sequence>=0&&action>=0&&action<=PowerStationSession.ENERGIZE&&item>= -1&&item<=15&&Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a)<=100_000&&Math.abs(b)<=100_000;}
        @Override public Type<Input> type(){return TYPE;}
    }
    public record State(long sessionId,PowerStationSnapshot state) implements CustomPacketPayload {
        public static final Type<State> TYPE=id("state");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,v)->{
            b.writeLong(v.sessionId);var s=v.state;b.writeVarInt(s.stage());b.writeVarInt(s.feedback());b.writeVarInt(s.errors());b.writeBoolean(s.started());b.writeLong(s.elapsed());b.writeLong(s.clock());b.writeLong(s.event());
            b.writeDouble(s.voltage());b.writeDouble(s.power());b.writeDouble(s.initialPf());b.writeDouble(s.capacitorUnit());b.writeVarInt(s.capacitors());b.writeVarInt(s.capacity());
            b.writeDouble(s.generatorVoltage());b.writeDouble(s.frequency());b.writeDouble(s.phase());b.writeVarInt(s.phaseSequence());b.writeLong(s.stable());b.writeDouble(s.phaseLimit());
            for(double d:s.loadP())b.writeDouble(d);for(double d:s.loadQ())b.writeDouble(d);for(int d:s.assignments())b.writeVarInt(d);for(double d:s.phaseI())b.writeDouble(d);
        },b->new State(b.readLong(),read(b)));
        public State{if(sessionId<1||state==null)throw new IllegalArgumentException("Invalid station envelope");}
        private static double[] doubles(RegistryFriendlyByteBuf b,int n){double[] a=new double[n];for(int i=0;i<n;i++)a[i]=b.readDouble();return a;}
        private static PowerStationSnapshot read(RegistryFriendlyByteBuf b){int stage=b.readVarInt(),feedback=b.readVarInt(),errors=b.readVarInt();boolean started=b.readBoolean();long elapsed=b.readLong(),clock=b.readLong(),event=b.readLong();
            double voltage=b.readDouble(),power=b.readDouble(),pf=b.readDouble(),unit=b.readDouble();int caps=b.readVarInt(),capacity=b.readVarInt();double u=b.readDouble(),f=b.readDouble(),phase=b.readDouble();int seq=b.readVarInt();long stable=b.readLong();double limit=b.readDouble();
            double[] p=doubles(b,6),q=doubles(b,6);int[] assignment=new int[6];for(int i=0;i<6;i++)assignment[i]=b.readVarInt();
            return new PowerStationSnapshot(stage,feedback,errors,started,elapsed,clock,event,voltage,power,pf,unit,caps,capacity,u,f,phase,seq,stable,limit,p,q,assignment,doubles(b,3));}
        @Override public Type<State> type(){return TYPE;}
    }
}
