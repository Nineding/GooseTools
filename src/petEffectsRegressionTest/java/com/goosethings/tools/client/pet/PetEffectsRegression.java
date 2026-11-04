package com.goosethings.tools.client.pet;

import com.goosethings.tools.pet.PetEffects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
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
            finish(mc, "PASS " + checks.size() + " checks\n" + String.join("\n", checks));
        } catch (Throwable error) { error.printStackTrace(); finish(mc, "FAIL " + error); }
    }
    private void finish(Minecraft mc, String result) {
        finished = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); }
        catch (Exception error) { error.printStackTrace(); }
        mc.stop();
    }
}
