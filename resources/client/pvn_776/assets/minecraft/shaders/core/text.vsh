#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    #ifdef IS_GUI
    // MapMaker anchored text: the payload rides the shadow quad's color
    // (A=0x4E sentinel, R=0xA0|anchor, G=yOffset+128, B=RGB 3:3:2 tint); the paired
    // main glyph carries the kill color, so exactly one quad survives per glyph.
    vec3 pos = vec3(Position.xyz);
    vec4 color = Color;
    ivec4 icol = ivec4(round(Color * 255.0));

    if (icol.r == 0x4E && icol.g == 0xB0 && icol.b == 0x00) {
        // Main glyph of an anchored run; the alpha test discards it.
        color = vec4(0.0);
    } else if (icol.a == 0x4E && (icol.r >> 4) == 0xA) {
        // Shadow glyph of an anchored run; this is the visible glyph. The carrier is
        // the first boss bar's name composed to net-zero advance, so the pre-move
        // glyph sits at (floor(guiWidth / 2) + advance, 3). Translate origin -> anchor
        // and cancel the vanilla +1,+1 shadow displacement.
        int anchor = icol.r & 15;
        vec2 scrSize = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]) - 0.001);
        vec2 target = vec2(float(anchor % 3), float(anchor / 3)) * 0.5 * scrSize;
        vec2 origin = vec2(floor(scrSize.x * 0.5), 3.0);
        pos.xy += target - origin + vec2(-1.0, float(icol.g - 128) - 1.0);

        color = vec4(vec3(icol.b >> 5, (icol.b >> 2) & 7, icol.b & 3) / vec3(7.0, 7.0, 3.0), 1.0);
    } else if (icol.a == 0x4E && (icol.r >> 4) == 0xB) {
        // Relative-offset glyph (container titles, toasts): move down by a 12-bit y offset
        // from wherever vanilla drew it, canceling the shadow displacement. No anchoring.
        pos.xy += vec2(-1.0, float(((icol.r & 15) << 8 | icol.g) - 2048) - 1.0);

        color = vec4(vec3(icol.b >> 5, (icol.b >> 2) & 7, icol.b & 3) / vec3(7.0, 7.0, 3.0), 1.0);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexColor = color;
    texCoord0 = UV0;
    #else
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    #if !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    #else
    vertexColor = Color;
    #endif
    texCoord0 = UV0;
    #endif
}
