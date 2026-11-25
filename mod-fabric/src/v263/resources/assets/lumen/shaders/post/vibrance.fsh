#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform VibranceConfig {
    float Vibrance;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

// Pushes colours away from grey. Colours that are already strong are pushed less, so nothing clips to neon.
void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    float strength = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
    fragColor = vec4(clamp(mix(vec3(grey), c, 1.0 + Vibrance * (1.0 - 0.6 * strength)), 0.0, 1.0), 1.0);
}
