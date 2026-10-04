package com.goosethings.tools.client.camera;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import java.util.*;

/** Temporary upload used only to bake one static monitor terrain frame. */
final class CameraGeometry implements AutoCloseable {
    private record Builder(ByteBufferBuilder memory,BufferBuilder vertices) {}
    private record Mesh(ChunkSectionLayer layer,GpuBuffer vertices,int indices) {}
    private final Map<ChunkSectionLayer,Builder> builders=new EnumMap<>(ChunkSectionLayer.class);
    private final List<Mesh> meshes=new ArrayList<>();
    public VertexConsumer getBuffer(ChunkSectionLayer layer) {
        return builders.computeIfAbsent(layer,l -> {
            var memory=new ByteBufferBuilder(786432);
            return new Builder(memory,new BufferBuilder(memory,PrimitiveTopology.QUADS,l.vertexFormat()));
        }).vertices();
    }
    void upload() {
        try {
            for(var entry:builders.entrySet()) {
                try(var data=entry.getValue().vertices().build()) {
                    if(data==null)continue;
                    var gpu=RenderSystem.getDevice().createBuffer(() -> "GooseTools camera terrain",GpuBuffer.USAGE_VERTEX,data.vertexBuffer());
                    meshes.add(new Mesh(entry.getKey(),gpu,data.drawState().indexCount()));
                }
            }
            meshes.sort(Comparator.comparing(m -> m.layer().translucent()));
        } finally {
            builders.values().forEach(b -> b.memory().close());builders.clear();
        }
    }
    int meshCount() { return meshes.size(); }
    long indexCount() { return meshes.stream().mapToLong(Mesh::indices).sum(); }
    String indexSummary() {
        var totals = new EnumMap<ChunkSectionLayer,Long>(ChunkSectionLayer.class);
        for (var layer : ChunkSectionLayer.values()) totals.put(layer,0L);
        for (var mesh : meshes) totals.merge(mesh.layer(),(long)mesh.indices(),Long::sum);
        return "solid=" + totals.get(ChunkSectionLayer.SOLID)
                + ", cutout=" + totals.get(ChunkSectionLayer.CUTOUT)
                + ", translucent=" + totals.get(ChunkSectionLayer.TRANSLUCENT);
    }
    void draw(RenderPass pass) {
        if (meshes.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        var transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        var lightSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        var auto=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms",transform);
        pass.setUniform("Sampler0",atlas.getTextureView(),atlas.getSampler());
        pass.setUniform("Sampler2",mc.gameRenderer.lightmap(),lightSampler);
        for(var mesh:meshes) {
            var indices=auto.getBuffer(mesh.indices());
            pass.setPipeline(RenderSystem.getCompiledPipeline(CameraRenderPipelines.forLayer(mesh.layer())));
            pass.setVertexBuffer(0,mesh.vertices().slice());pass.setIndexBuffer(indices,auto.type());
            // RenderPearl 26.3: indexCount, instanceCount, firstIndex, vertexOffset, firstInstance.
            pass.drawIndexed(mesh.indices(),1,0,0,0);
        }
    }
    @Override public void close() {
        meshes.forEach(m -> m.vertices().close());meshes.clear();
        builders.values().forEach(b -> b.memory().close());builders.clear();
    }
}
