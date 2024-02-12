#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform mat4 ProjMat;
uniform vec2 ScreenSize;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 position;
in vec4 normal;

out vec4 fragColor;

const float epsilon = 0.001;
const float overlayTextureAlpha = 172.0 / 255.0;
const float baseTextureAlpha = 238.0 / 255.0;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    
    if (abs(color.a - overlayTextureAlpha) < epsilon) {
        bool is_in_gui = ProjMat[3][3] != 0.0;
        float pixel_width = ProjMat[0][0] / 2.0;
        if (is_in_gui && position.x * pixel_width < 0.35) {
            color = vec4(color.rgb, 1);
            color * vertexColor * ColorModulator;
            color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
            color *= lightMapColor;
            fragColor = color;
            return;
        } else {
            discard;
        }
    } else if (abs(color.a - baseTextureAlpha) < epsilon) {
        bool is_in_gui = ProjMat[3][3] != 0.0;
        float pixel_width = ProjMat[0][0] / 2.0;
        if (is_in_gui && position.x * pixel_width < 0.35) {
            discard;
        } else {
            color = vec4(color.rgb, 1);
            color * vertexColor * ColorModulator;
            color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
            color *= lightMapColor;
            fragColor = color;
            return;
        }
    }

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
