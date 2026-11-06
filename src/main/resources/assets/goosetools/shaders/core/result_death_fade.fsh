#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 0) out vec4 fragColor;

void main() {
    // The completed entity texture is flipped vertically: 0 is at the feet.
    // Keep the upper body opaque and smoothly disappear from waist to soles.
    float opacity = smoothstep(0.02, 0.48, texCoord0.y);
    // Entity textures use premultiplied alpha; fade RGB and alpha together.
    fragColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator * opacity;
}
