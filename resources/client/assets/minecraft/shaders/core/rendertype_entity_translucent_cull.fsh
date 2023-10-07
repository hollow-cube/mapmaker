#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;

out vec4 fragColor;

// color.a*100-99 < 0.7

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    if (vertexDistance < 1600 && color.a*100-99 < 0.7) {
        discard;
    } else if (vertexDistance >= 1600 && color.a*100-99 < 0.7) {
        fragColor = color;
        return;
    }
    //    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    //    if (color.a < 0.1 || (vertexDistance < 1600 && (color.a > 0.90))) {
    //        discard;
    //    } else if (vertexDistance >= 1600 && abs(color.a*100-99) < 0.7) {
    //        fragColor = vec4(texture(Sampler0, texCoord0).xyz, 1);
    //        //        fragColor = ve;
    //        //        discard;
    //    } else {
    //        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    //    }

    //    if (vertexDistance < 1600) {
    //        fragColor = vec4(1, 0, 0, 1);
    //    }

    color *= vertexColor * ColorModulator;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
