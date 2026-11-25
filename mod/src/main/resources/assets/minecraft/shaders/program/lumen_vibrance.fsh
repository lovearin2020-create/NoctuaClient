#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;
uniform float Vibrance;

// Pushes colours away from grey. Colours that are already strong are pushed less, so nothing clips to neon.
void main() {
    vec3 c = texture2D(DiffuseSampler, texCoord).rgb;
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    float strength = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
    gl_FragColor = vec4(clamp(mix(vec3(grey), c, 1.0 + Vibrance * (1.0 - 0.6 * strength)), 0.0, 1.0), 1.0);
}
