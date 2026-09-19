package com.goosethings.tools.client.camera;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.*;

/** Upload static terrain once per scene revision; subsequent camera frames reuse GPU buffers. */
final class CameraGeometry implements AutoCloseable {
    private record Builder(ByteBufferBuilder memory,BufferBuilder vertices) {}
    private record Mesh(RenderType type,GpuBuffer vertices,int indices) {}
    private final Map<RenderType,Builder> builders=new LinkedHashMap<>();
    private final List<Mesh> meshes=new ArrayList<>();
    public VertexConsumer getBuffer(RenderType type) {
        return builders.computeIfAbsent(type,t -> {
            var memory=new ByteBufferBuilder(786432);
            return new Builder(memory,new BufferBuilder(memory,t.primitiveTopology(),t.format()));
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
            meshes.sort(Comparator.comparing(m -> m.type().hasBlending()));
        } finally {
            builders.values().forEach(b -> b.memory().close());builders.clear();
        }
    }
    void draw(TextureTarget target) {
        for(var mesh:meshes) {
            var prepared=mesh.type().prepare();
            var auto=RenderSystem.getSequentialBuffer(mesh.type().primitiveTopology());
            var indices=auto.getBuffer(mesh.indices());
            try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "GooseTools camera terrain",target.getColorTextureView(),Optional.empty(),
                    target.getDepthTextureView(),OptionalDouble.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(prepared.pipeline()));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms",prepared.dynamicTransforms());
                prepared.textures().forEach(t -> pass.setUniform(t.name(),t.textureView(),t.sampler()));
                pass.setVertexBuffer(0,mesh.vertices().slice());pass.setIndexBuffer(indices,auto.type());
                pass.drawIndexed(0,0,mesh.indices(),1,0);
            }
        }
    }
    @Override public void close() {
        meshes.forEach(m -> m.vertices().close());meshes.clear();
        builders.values().forEach(b -> b.memory().close());builders.clear();
    }
}
