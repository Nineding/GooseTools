package com.goosethings.tools.client.pet;

import com.goosethings.tools.pet.PetEffects;
import com.goosethings.tools.client.nametag.NameTagClientState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.scores.Team;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** Real client transformations and native effect queues; never included in the runtime JAR. */
public final class PetEffectsRegression implements ClientModInitializer {
    public static int sounds;
    private boolean opened, finished;
    private int ticks;
    private final List<String> checks = new ArrayList<>();
    @Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }

    private void check(boolean value, String text) { if (!value) throw new AssertionError(text); checks.add(text); }
    private int particles(Minecraft mc) throws Exception {
        var field = ParticleEngine.class.getDeclaredField("particlesToAdd"); field.setAccessible(true);
        return ((Queue<?>) field.get(mc.particleEngine)).size();
    }
    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true; mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("pet-effects-" + System.currentTimeMillis(),
                        new LevelSettings("Pet effects regression", GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if (mc.player == null || mc.level == null || !mc.player.connection.hasClientLoaded() || ++ticks < 30) return;
            var floor = mc.player.blockPosition().below();
            mc.level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            var team = mc.level.getScoreboard().addPlayerTeam("ggdPetNoCollide"); team.setCollisionRule(Team.CollisionRule.NEVER);
            var emission = Entity.class.getDeclaredMethod("getMovementEmission"); emission.setAccessible(true);
            var sprint = Entity.class.getDeclaredMethod("spawnSprintParticle"); sprint.setAccessible(true);
            for (Entity entity : List.of(new Rabbit(EntityTypes.RABBIT, mc.level), new Chicken(EntityTypes.CHICKEN, mc.level))) {
                entity.snapTo(floor.getX()+.5, floor.getY()+1, floor.getZ()+.5, 0, 0);
                int before = particles(mc); sprint.invoke(entity);
                check(particles(mc) > before, entity.getType()+" ordinary foot debris still appears");
                before = sounds; entity.playSound(SoundEvents.STONE_STEP, 1, 1);
                check(sounds > before, entity.getType()+" ordinary sound still reaches client world");
                mc.level.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), team);
                check(PetEffects.suppress(entity) && emission.invoke(entity) == Entity.MovementEmission.NONE, entity.getType()+" native team suppresses only pet movement effects");
                before = particles(mc); sprint.invoke(entity);
                if (entity instanceof Rabbit rabbit) {
                    rabbit.handleEntityEvent((byte)1);
                    check(rabbit.getJumpCompletion(.5f) > 0, "pet rabbit native jump animation still starts");
                }
                check(particles(mc) == before, entity.getType()+" pet footsteps and jump event produce no debris");
                before = sounds; entity.playSound(SoundEvents.STONE_STEP, 1, 1);
                check(sounds == before, entity.getType()+" pet local sound is suppressed even without Silent metadata");
                mc.level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
                check(!PetEffects.suppress(entity) && emission.invoke(entity) == Entity.MovementEmission.ALL, entity.getType()+" removing team restores ordinary effects");
            }
            mc.level.getScoreboard().addPlayerToTeam(mc.player.getScoreboardName(), team);
            check(!PetEffects.suppress(mc.player), "player sharing the team is not classified as a pet");
            attachedMiniMe(mc, team);
            finish(mc, "PASS " + checks.size() + " checks\n" + String.join("\n", checks));
        } catch (Throwable error) { error.printStackTrace(); finish(mc, "FAIL " + error); }
    }

    private void attachedMiniMe(Minecraft mc, net.minecraft.world.scores.PlayerTeam team) {
        var mini = new ClientMannequin(mc.level, mc.playerSkinRenderCache());
        mini.setId(2_000_000_101);
        NameTagClientState.registerLocalAlias(mini.getUUID(), mc.player.getUUID());
        check(mc.player.getUUID().equals(NameTagClientState.sourcePlayerId(mini)), "ordinary stand-ins keep their labels");
        mc.level.getScoreboard().addPlayerToTeam(mini.getScoreboardName(), team);
        check(NameTagClientState.sourcePlayerId(mini) == null, "Mini Me never resolves to a player nameplate");
        NameTagClientState.unregisterLocalAlias(mini.getUUID());
        var seat = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
        seat.setId(2_000_000_102);
        seat.snapTo(mc.player.getX()-5, mc.player.getY(), mc.player.getZ(), 0, 0);
        seat.setCustomName(Component.literal("ggdPetAnchor:" + mc.player.getId() + ":1:-1.0"));
        check(mini.startRiding(seat,true,false), "Mini Me receives a native seated pose");
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        float partial = .4F;
        var state = mc.getEntityRenderDispatcher().extractEntity(mini, partial);
        var owner = mc.player.getPosition(partial);
        check(Math.abs(state.y-owner.y-mc.player.getEyeHeight()+.30)<1e-6
                && Math.abs(Math.hypot(state.x-owner.x,state.z-owner.z)-.43)<1e-6,
                "shoulder renders from the owner's current frame even when the seat lags");
        seat.setCustomName(Component.literal("ggdPetAnchor:" + mc.player.getId() + ":0:-1.0"));
        state = mc.getEntityRenderDispatcher().extractEntity(mini, partial);
        check(Math.abs(state.x-owner.x)<1e-6 && Math.abs(state.z-owner.z)<1e-6
                && Math.abs(state.y-owner.y-mc.player.getEyeHeight()-.2)<1e-6,
                "head placement is directly above the owner's interpolated position");
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        state = mc.getEntityRenderDispatcher().extractEntity(mini, partial);
        var camera = mc.gameRenderer.mainCamera().position();
        var towardPet = new net.minecraft.world.phys.Vec3(state.x,state.y,state.z).subtract(camera);
        check(PetAttachmentRenderer.firstPersonPet(mini)&&towardPet.dot(mc.player.getViewVector(partial))>.9,
                "owner's pet renders ahead of the first-person near plane");
    }
    private void finish(Minecraft mc, String result) {
        finished = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); }
        catch (Exception error) { error.printStackTrace(); }
        mc.stop();
    }
}
