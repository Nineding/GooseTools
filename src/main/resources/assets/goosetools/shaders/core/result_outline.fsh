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

float silhouette(vec2 uv) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return 0.0;
    return step(0.001, texture(Sampler0, uv).a);
}

void main() {
    // Minecraft's entity_sobel outline compares the four neighbouring alpha samples.
    // Normalize the mask so translucent deceased players retain the same bright contour.
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    float center = silhouette(texCoord0);
    float edge = clamp(abs(center - silhouette(texCoord0 - vec2(pixel.x, 0.0)))
                     + abs(center - silhouette(texCoord0 + vec2(pixel.x, 0.0)))
                     + abs(center - silhouette(texCoord0 - vec2(0.0, pixel.y)))
                     + abs(center - silhouette(texCoord0 + vec2(0.0, pixel.y))), 0.0, 1.0);
    if (edge == 0.0) discard;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * edge) * ColorModulator;
}
