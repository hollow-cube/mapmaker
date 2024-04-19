#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform mat4 ProjMat;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec3 position;
in vec4 normal;

out vec4 fragColor;

bool is_alpha(vec4 color, float alpha) {
    return abs(color.a - (alpha / 255.0)) < 0.01;
}

const float ALPHA_2D = 221.0;
const float ALPHA_3D = 225.0;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }

    bool is_in_gui = ProjMat[3][3] != 0.0;
    bool in_player_model = is_in_gui && position.z < 125.0;
    if (is_in_gui && !in_player_model) {
        if (is_alpha(color, ALPHA_3D)) discard;
        if (is_alpha(color, ALPHA_2D)) {
            fragColor = vec4(color.rgb, 1);
            return;
        }
    } else {
        if (is_alpha(color, ALPHA_2D)) discard;
        if (is_alpha(color, ALPHA_3D)) {
            fragColor = vec4(color.rgb, 1);
            return;
        }
    }

    color *= vertexColor * ColorModulator;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
