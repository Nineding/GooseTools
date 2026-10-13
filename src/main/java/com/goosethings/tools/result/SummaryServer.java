package com.goosethings.tools.result;

import com.goosethings.tools.network.MandatoryHandshake;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Captures paid results, then hands them off once normal lobby cleanup has completed. */
public final class SummaryServer {
    private record Before(int level, int xp, int bank) {}
    private record Session(UUID round, int expires) {}
    private static final Map<UUID,Before> BEFORE = new HashMap<>();
    private static final Map<UUID,SummaryPayload> PENDING = new HashMap<>();
    private static final Map<UUID,Session> ACTIVE = new HashMap<>();
    private static UUID round;
    private static boolean armed, handoff;
    private SummaryServer() {}
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SummaryControlPayload.TYPE,(p,c)->c.server().execute(()->{
            Session s=ACTIVE.get(c.player().getUUID());
            if(s!=null && s.round.equals(p.round())) finish(c.player(),true);
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            // Persist the guard until rejoin so a queued promotion can resume after disconnect.
            ACTIVE.remove(handler.player.getUUID());
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            if(handler.player.removeTag("ggdSummaryActive")&&handler.player.entityTags().contains("lobby"))
                function(handler.player,"lobby:trust/try_schedule_pending_break");
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{ BEFORE.clear(); PENDING.clear(); ACTIVE.clear(); round=null; armed=false; handoff=false; });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(UUID id:List.copyOf(ACTIVE.keySet())) {
                var player=server.getPlayerList().getPlayer(id); var session=ACTIVE.get(id);
                if(player==null) { ACTIVE.remove(id); continue; }
                if(!dlc(server)||!player.entityTags().contains("lobby")||player.entityTags().contains("gamingGGD")
                        ||server.getTickCount()>=session.expires) finish(player,true);
            }
        });
    }
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("summary")
                .then(Commands.literal("before").then(Commands.argument("players",EntityArgument.players())
                        .executes(c->before(c.getSource().getServer(),EntityArgument.getPlayers(c,"players")))))
                .then(Commands.literal("capture").executes(c->capture(c.getSource().getServer())))
                .then(Commands.literal("ready").executes(c->{armed=!PENDING.isEmpty();return armed?1:0;}))
                .then(Commands.literal("stop").executes(c->stop(c.getSource().getServer())))
                .then(Commands.literal("clear").executes(c->{reset(c.getSource().getServer());return 1;}))
                .then(Commands.literal("open").executes(c->open(c.getSource().getServer())));
    }
    /** Public self-only lobby command, also used by the client settings buttons. */
    public static LiteralArgumentBuilder<CommandSourceStack> celebrationCommand() {
        return Commands.literal("goosetools-celebration")
                .requires(s->s.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("wave").executes(c->select(c.getSource().getPlayerOrException(),false)))
                .then(Commands.literal("dance").executes(c->select(c.getSource().getPlayerOrException(),true)));
    }
    private static int select(ServerPlayer player, boolean dance) {
        if(!player.entityTags().contains("lobby") || player.entityTags().contains("gamingGGD")
                || player.entityTags().contains("endGame") || player.entityTags().contains("inTutorial")) {
            player.sendSystemMessage(Component.translatableWithFallback("summary.goosetools.lobby_only","Choose your celebration in the lobby."));
            return 0;
        }
        if(dance) player.addTag("ggdVictoryDance"); else player.removeTag("ggdVictoryDance");
        player.sendSystemMessage(Component.translatableWithFallback("summary.goosetools.saved","Victory celebration saved: %s",
                Component.translatableWithFallback(dance?"summary.goosetools.dance":"summary.goosetools.wave",dance?"Dance":"Wave")));
        return 1;
    }
    public static void reset(MinecraftServer server) {
        cancelQueued(server);
        for(var player:server.getPlayerList().getPlayers()) finish(player,false);
        BEFORE.clear(); PENDING.clear(); round=UUID.randomUUID(); armed=false; handoff=false;
    }
    private static boolean dlc(MinecraftServer server) { return score(server,"ggdadv",ScoreHolder.forNameOnly("FullBloodDLC"))==1; }
    private static int score(MinecraftServer server,String objective,ScoreHolder who) {
        var obj=server.getScoreboard().getObjective(objective);
        var value=obj==null?null:server.getScoreboard().getPlayerScoreInfo(who,obj);
        return value==null?0:value.value();
    }
    private static int score(ServerPlayer player,String objective) { return score(player.level().getServer(),objective,player); }
    private static int before(MinecraftServer server,Collection<ServerPlayer> players) {
        if(!dlc(server)||round==null||!PENDING.isEmpty()) return 0;
        for(var player:players) if(player.entityTags().contains("players")&&!player.entityTags().contains("inTutorial"))
            BEFORE.putIfAbsent(player.getUUID(),new Before(Math.max(0,score(player,"playerLevel")),Math.max(0,score(player,"playerExp")),Math.max(0,score(player,"playerExpBank"))));
        return BEFORE.size();
    }
    private static int capture(MinecraftServer server) {
        MatchResultPayload result=MatchResultServer.snapshot();
        if(!dlc(server)||round==null||result==null||!PENDING.isEmpty()) return 0;
        List<SummaryPolicy.Candidate> candidates=new ArrayList<>();
        for(var appearance:result.roster()) {
            var player=server.getPlayerList().getPlayer(appearance.id()); if(player==null) continue;
            int award=player.entityTags().contains("ggd_SMVP")?2:player.entityTags().contains("ggd_MVP")?1:0;
            candidates.add(new SummaryPolicy.Candidate(appearance.id(),award,score(player,"ggdIndexScore"),appearance.serial()));
        }
        var chosen=SummaryPolicy.spotlight(candidates);
        if(chosen==null) return 0;
        var spotlight=result.roster().stream().filter(p->p.id().equals(chosen.id())).findFirst().orElseThrow();
        var subject=server.getPlayerList().getPlayer(spotlight.id());
        for(var viewer:server.getPlayerList().getPlayers()) {
            if(viewer.entityTags().contains("inTutorial")||!viewer.entityTags().contains("endGame")) continue;
            var self=result.roster().stream().filter(p->p.id().equals(viewer.getUUID())).findFirst().orElse(null);
            var start=BEFORE.get(viewer.getUUID()); boolean participating=start!=null && self!=null;
            int from=participating?start.level:Math.max(0,score(viewer,"playerLevel"));
            int to=Math.max(from,score(viewer,"playerLevel"));
            if(from>10000||to>10000) continue;
            var thresholds=new ArrayList<Integer>(); for(int i=from;i<=to;i++) thresholds.add(SummaryPolicy.needed(i));
            int bonus=viewer.entityTags().contains("match_xp_smvp")?30:viewer.entityTags().contains("match_xp_mvp15")?15:viewer.entityTags().contains("match_xp_mvp")?20:0;
            var data=new SummaryPayload(false,round,new MatchResultPayload(true,result.outcome(),List.of(spotlight)),chosen.award(),
                    subject!=null&&subject.entityTags().contains("ggdVictoryDance")?1:0,chosen.index(),participating,
                    participating?Math.max(0,score(viewer,"matchBaseXP")):0,participating?bonus:0,
                    participating&&viewer.entityTags().contains("match_xp_death")?25:0,participating?Math.max(0,score(viewer,"matchFinalXP")):0,
                    participating?Math.max(0,score(viewer,"matchGooseCoin")):0,from,participating?start.xp:Math.max(0,score(viewer,"playerExp")),
                    to,Math.max(0,score(viewer,"playerExp")),Math.clamp(score(viewer,"xpAnimCap"),0,10000),
                    participating?Math.max(0,score(viewer,"playerExpBank")-start.bank):0,thresholds);
            PENDING.put(viewer.getUUID(),data);
            if(MandatoryHandshake.isVerified(viewer)&&ServerPlayNetworking.canSend(viewer,SummaryPayload.TYPE)) ServerPlayNetworking.send(viewer,data);
        }
        BEFORE.clear(); return PENDING.size();
    }
    private static int stop(MinecraftServer server) {
        for(var player:server.getPlayerList().getPlayers()) finish(player,false);
        handoff=armed && dlc(server); armed=false;
        if(!handoff) { cancelQueued(server); BEFORE.clear(); PENDING.clear(); }
        // The frozen stage can appear while the datapack restores the lobby behind it.
        // Waiting for final cleanup also waited for entity death animations and chunk loading.
        boolean normal=handoff;
        if(normal) open(server);
        return normal?1:0;
    }
    private static int open(MinecraftServer server) {
        if(!handoff) return 0;
        handoff=false; int sent=0;
        if(dlc(server)) for(var player:server.getPlayerList().getPlayers()) {
            var payload=PENDING.get(player.getUUID());
            if(payload==null||(!player.entityTags().contains("lobby")&&!player.entityTags().contains("endGame"))||player.entityTags().contains("inTutorial")
                    ||!MandatoryHandshake.isVerified(player)||!ServerPlayNetworking.canSend(player,SummaryPayload.TYPE)) continue;
            player.addTag("ggdSummaryActive"); ACTIVE.put(player.getUUID(),new Session(payload.round(),server.getTickCount()+2400));
            if(payload.participant()) function(player,"lobby:level/summary_consume_xp");
            ServerPlayNetworking.send(player,payload.forDisplay()); sent++;
        }
        for(var player:server.getPlayerList().getPlayers()) {
            var pending=PENDING.get(player.getUUID());
            if(pending!=null&&!ACTIVE.containsKey(player.getUUID())&&ServerPlayNetworking.canSend(player,SummaryControlPayload.TYPE))
                ServerPlayNetworking.send(player,new SummaryControlPayload(pending.round()));
        }
        PENDING.clear(); return sent;
    }
    private static void finish(ServerPlayer player,boolean resume) {
        var session=ACTIVE.remove(player.getUUID()); player.removeTag("ggdSummaryActive");
        if(session==null) return;
        if(ServerPlayNetworking.canSend(player,SummaryControlPayload.TYPE)) ServerPlayNetworking.send(player,new SummaryControlPayload(session.round));
        if(resume&&player.entityTags().contains("lobby")) function(player,"lobby:trust/try_schedule_pending_break");
    }
    private static void cancelQueued(MinecraftServer server) {
        for(var player:server.getPlayerList().getPlayers()) {
            var pending=PENDING.get(player.getUUID());
            if(pending!=null&&ServerPlayNetworking.canSend(player,SummaryControlPayload.TYPE))
                ServerPlayNetworking.send(player,new SummaryControlPayload(pending.round()));
        }
    }
    private static void function(ServerPlayer player,String id) {
        var server=player.level().getServer();
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(),"function "+id);
    }
}
