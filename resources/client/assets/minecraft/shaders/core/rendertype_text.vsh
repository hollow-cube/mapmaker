#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = vec3(Position.xyz);

    // More book handling
    if (Color == vec4(78/255., 92/255., 38/255., Color.a)) {
        pos.x -= 9;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    // More book handling
    if (Color == vec4(78/255., 92/255., 38/255., Color.a)) {
        vertexColor = vec4(texelFetch(Sampler2, UV2 / 16, 0).xyz, vertexColor.a);
        return;
    }
}
