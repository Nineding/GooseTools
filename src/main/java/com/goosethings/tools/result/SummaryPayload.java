package com.goosethings.tools.result;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Public spotlight plus only this recipient's authoritative reward snapshot. */
public record SummaryPayload(boolean open, UUID round, MatchResultPayload appearance, int award, int action,
        int spotlightIndex, boolean participant, int baseXp, int bonusPercent, int deathPercent,
        int gainedXp, int coins, int startLevel, int startXp, int endLevel, int endXp,
        int cap, int bankGain, List<Integer> thresholds) implements CustomPacketPayload {
    public static final Type<SummaryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "summary_s2c_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SummaryPayload> CODEC = StreamCodec.of(SummaryPayload::write, SummaryPayload::read);
    public SummaryPayload {
        thresholds = List.copyOf(thresholds);
        if (round == null || appearance == null || !appearance.active() || appearance.roster().size()!=1
                || award<0 || award>2 || action<0 || action>1 || baseXp<0 || gainedXp<0 || coins<0
                || bonusPercent<0 || bonusPercent>30 || deathPercent<0 || deathPercent>25
                || startLevel<0 || endLevel<startLevel || endLevel>10000 || startXp<0 || endXp<0
                || cap<0 || cap>10000 || bankGain<0 || thresholds.size()!=endLevel-startLevel+1
                || thresholds.stream().anyMatch(n->n<1 || n>1000000)) throw new IllegalArgumentException("Invalid summary snapshot");
    }
    private static void write(RegistryFriendlyByteBuf b, SummaryPayload p) {
        b.writeBoolean(p.open);
        b.writeUUID(p.round); MatchResultPayload.CODEC.encode(b,p.appearance);
        b.writeVarInt(p.award); b.writeVarInt(p.action); b.writeInt(p.spotlightIndex); b.writeBoolean(p.participant);
        for (int n : new int[]{p.baseXp,p.bonusPercent,p.deathPercent,p.gainedXp,p.coins,p.startLevel,p.startXp,
                p.endLevel,p.endXp,p.cap,p.bankGain}) b.writeVarInt(n);
        b.writeVarInt(p.thresholds.size()); for (int n:p.thresholds) b.writeVarInt(n);
    }
    private static SummaryPayload read(RegistryFriendlyByteBuf b) {
        boolean open=b.readBoolean();
        UUID round=b.readUUID(); var appearance=MatchResultPayload.CODEC.decode(b);
        int award=b.readVarInt(),action=b.readVarInt(),index=b.readInt(); boolean participant=b.readBoolean();
        int[] n=new int[11]; for(int i=0;i<n.length;i++) n[i]=b.readVarInt();
        int count=b.readVarInt(); if(count<1||count>10001) throw new IllegalArgumentException("Too many summary levels");
        List<Integer> thresholds=new ArrayList<>(); for(int i=0;i<count;i++) thresholds.add(b.readVarInt());
        return new SummaryPayload(open,round,appearance,award,action,index,participant,n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],thresholds);
    }
    public SummaryPayload forDisplay() {
        return new SummaryPayload(true,round,appearance,award,action,spotlightIndex,participant,baseXp,bonusPercent,deathPercent,
                gainedXp,coins,startLevel,startXp,endLevel,endXp,cap,bankGain,thresholds);
    }
    @Override public Type<SummaryPayload> type() { return TYPE; }
}
