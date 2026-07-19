#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    // MAPMAKER START
    ivec4 icol = ivec4(round(Color * 255.0));
    if (icol.r == 0x4E && icol.g == 0xB0 && icol.b == 0x00) {
        vertexColor = vec4(0.0);
    } else if (icol.a == 0x4E && (icol.r >> 4) == 0xA) {
        int anchor = icol.r & 15;
        vec2 scrSize = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]) - 0.001);
        vec2 target = vec2(float(anchor % 3), float(anchor / 3)) * 0.5 * scrSize;
        vec2 origin = vec2(floor(scrSize.x * 0.5), 3.0);
        vec3 pos = Position;
        pos.xy += target - origin + vec2(-1.0, float(icol.g - 128) - 1.0);
        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
        vertexColor = vec4(vec3(icol.b >> 5, (icol.b >> 2) & 7, icol.b & 3) / vec3(7.0, 7.0, 3.0), 1.0);
    } else if (icol.a == 0x4E && (icol.r >> 4) == 0xB) {
        vec3 pos = Position;
        pos.xy += vec2(-1.0, float(((icol.r & 15) << 8 | icol.g) - 2048) - 1.0);
        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
        vertexColor = vec4(vec3(icol.b >> 5, (icol.b >> 2) & 7, icol.b & 3) / vec3(7.0, 7.0, 3.0), 1.0);
    }
    // MAPMAKER END
}
