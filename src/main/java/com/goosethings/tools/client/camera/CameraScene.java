package com.goosethings.tools.client.camera;

import com.goosethings.tools.camera.*;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read-only scene adapter for vanilla block models. */
public final class CameraScene implements BlockAndTintGetter {
    public final CameraDefinition camera;
    public CameraBlockFrame blocks;
    public byte[] compressedBlocks;
    public List<CameraPackets.Actor> actors = List.of();
    public Map<UUID, CameraPackets.Actor> previousActors = Map.of();
    public long lastActorsAt;
    public long blockVersion;
    public int skyColor = 0xff78a7ff;
    public CameraScene(CameraDefinition camera) { this.camera = camera; }
    public void acceptActors(List<CameraPackets.Actor> next, long receivedAt) {
        previousActors = actors.stream().collect(Collectors.toUnmodifiableMap(
                CameraPackets.Actor::uuid, actor -> actor, (first, ignored) -> first));
        actors = next;
        lastActorsAt = receivedAt;
    }
    public float actorInterpolation(long now) {
        if (previousActors.isEmpty()) return 1.0F;
        return Math.clamp((now - lastActorsAt) / 50_000_000.0F, 0.0F, 1.0F);
    }
    public int index(BlockPos pos) {
        if (blocks == null) return -1;
        int x = pos.getX()-blocks.x(), y = pos.getY()-blocks.y(), z = pos.getZ()-blocks.z();
        return x < 0 || x >= CameraLimits.SIZE_X || y < 0 || y >= CameraLimits.SIZE_Y || z < 0 || z >= CameraLimits.SIZE_Z
                ? -1 : CameraBlockFrame.index(x, y, z);
    }
    @Override public BlockState getBlockState(BlockPos pos) {
        int i = index(pos); return i < 0 ? Blocks.AIR.defaultBlockState() : Block.stateById(blocks.states()[i]);
    }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public int getHeight() { return CameraLimits.SIZE_Y; }
    @Override public int getMinY() { return blocks == null ? 0 : blocks.y(); }
    @Override public LevelLightEngine getLightEngine() { return LevelLightEngine.EMPTY; }
    @Override public CardinalLighting cardinalLighting() { return CardinalLighting.DEFAULT; }
    @Override public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        int i = index(pos);
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (i < 0 || level == null) return 0x91bd59;
        var biome = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).byId(blocks.biomes()[i]);
        return biome == null ? 0x91bd59 : resolver.getColor(biome,pos.getX(),pos.getZ());
    }
    @Override public int getBrightness(LightLayer layer, BlockPos pos) {
        int i = index(pos); return i < 0 ? 15 : (blocks.lights()[i] >> (layer == LightLayer.SKY ? 20 : 4)) & 15;
    }
    public int light(BlockPos pos) { int i = index(pos); return i < 0 ? 0xf000f0 : blocks.lights()[i]; }
}
