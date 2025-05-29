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

vec2[4] corners = vec2[](vec2(0, 0), vec2(0, 1), vec2(1, 1), vec2(1, 0));
vec2[8] sizes = vec2[](vec2(1, 21./18.), vec2(2, 1), vec2(3, 21./18.), vec2(3, 2), vec2(3, 3), vec2(4, 3), vec2(1, 1), vec2(1, 1));

void main() {
    vec4 color = Color;
    vec3 pos = vec3(Position.xyz);
    ivec4 icol = ivec4(round(Color * 255));

    // More book handling
    if (Color == vec4(78/255., 92/255., 38/255., Color.a)) {
        pos.x -= 9;
    } else if (Color == vec4(78/255., 90/255., Color.b, Color.a)) {
        // TODO: need to also offset the shadow
        pos.y += (Color.b * 255) - 50;
    }

    // Hover icon handling
    if (icol.x == 78 && icol.y >> 2 == 11) {
        color = vec4(1);// Remove our marker color

        //        vec2 center = vec2(floor(ceil(2.0/ProjMat[0][0] - 0.1)/2.0), floor(ceil(2.0/(-ProjMat[1][1]) + 0.1)/2.0));        center -= vec2(81, 94);
        vec2 center = vec2(
        trunc((ceil(2.0/ProjMat[0][0] - 0.001) - 176.0)/2.0)+88.0,
        trunc((ceil(2.0/(-ProjMat[1][1]) - 0.001) - 222.0)/2.0)+111.0
        );
        center -= vec2(81, 94);

        int sizeIndex = ((icol.y & 3) << 1) | ((icol.z >> 7) & 1);
        vec2 offset = (corners[gl_VertexID % 4] * sizes[sizeIndex]) * 18;

        int slotIndex = icol.z & 0x7F;
        vec2 slotPos = vec2(slotIndex % 9, slotIndex / 9) * 18;
        if (slotPos.y > 5 * 18) {
            // offset between main and player slots
            slotPos += vec2(0, 13);
        }
        // TODO: offset between player and hotbar slots.

        pos = vec3(center + round(offset + slotPos), pos.z - 1);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    // More book handling
    if (Color == vec4(78/255., 92/255., 38/255., Color.a)) {
        vertexColor = vec4(texelFetch(Sampler2, UV2 / 16, 0).xyz, vertexColor.a);
        return;
    }

    // Remove the color from the vertical shift color so it only reflects the underlying texture.
    if (Color == vec4(78/255., 90/255., Color.b, Color.a)) {
        if (Color.a == (80./255.)) {
            vertexColor = vec4(0, 0, 0, Color.a);
        } else {
            vertexColor = vec4(texelFetch(Sampler2, UV2 / 16, 0).xyz, vertexColor.a);
        }
    } else if (false) {
        // TODO: we need to also handle the shadow color here.
        vertexColor = vec4(0);
    }
}
