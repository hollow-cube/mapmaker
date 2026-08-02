#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// MAPMAKER START
uniform sampler2D Sampler0;

flat out vec4 hoverCutout;
out vec2 screenPos;

const ivec3 HOVER_ICON_ID = ivec3(0xFE, 0x4E, 0x2A);
const float TOOLTIP_EDGE = 4.0;

vec2 cornerOf(vec2 texel) {
    return step(0.5, fract(texel));
}

ivec3 dataPixel(vec2 texel, vec2 cornerDir, int index) {
    ivec2 pos = ivec2(floor(texel));
    pos.x -= index * int(cornerDir.x);
    return ivec3(round(texelFetch(Sampler0, pos, 0).rgb * 255.0));
}
// MAPMAKER END

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;

    // MAPMAKER START
    hoverCutout = vec4(1.0, 1.0, 0.0, 0.0);
    screenPos = Position.xy;

    ivec4 icol = ivec4(round(Color * 255.0));
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 texel = UV0 * texSize;
    vec2 corner = cornerOf(texel);
    vec2 cornerDir = corner * 2.0 - 1.0;
    vec2 scrSize = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]) - 0.001);

    if (icol.r == 0x4E && icol.g == 0xB0 && icol.b == 0x00) {
        vertexColor = vec4(0.0);
    } else if (dataPixel(texel, cornerDir, 0) == HOVER_ICON_ID) {
        vec2 content = vec2(dataPixel(texel, cornerDir, 1).rg) + 1.0;

        vec3 pos = vec3(floor(scrSize * 0.5) + vec2(icol.rg) - 128.0 + corner * content, Position.z);
        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
        texCoord0.y -= cornerDir.y / texSize.y;
        vertexColor = vec4(1.0);

        vec2 textOrigin = Position.xy - corner * (content + vec2(0.0, 2.0)) - vec2(1.0, 8.0);
        int lines = icol.a & 0x1F;
        float tooltipWidth = float(((icol.a >> 5) << 8) | icol.b);
        float tooltipHeight = lines == 1 ? 8.0 : float(10 * lines);
        hoverCutout = vec4(textOrigin - TOOLTIP_EDGE,
                           textOrigin.x + tooltipWidth + TOOLTIP_EDGE,
                           textOrigin.y + tooltipHeight + TOOLTIP_EDGE);
        screenPos = pos.xy;
    } else if (icol.a == 0x4E && (icol.r >> 4) == 0xA) {
        int anchor = icol.r & 15;
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
