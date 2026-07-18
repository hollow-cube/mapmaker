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
    // MapMaker: our text tricks only apply to GUI text. In the GUI the lightmap is full-bright
    // (vanilla drops the sample_lightmap multiply entirely), so white here matches what
    // sample_lightmap would have produced.
    vec3 pos = vec3(Position.xyz);

    // More book handling
    if (Color == vec4(78 / 255., 92 / 255., 38 / 255., Color.a)) {
        pos.x -= 9;
    } else if (Color == vec4(78 / 255., 90 / 255., Color.b, Color.a)) {
        // TODO: need to also offset the shadow
        pos.y += (Color.b * 255) - 50;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;

    // More book handling
    if (Color == vec4(78 / 255., 92 / 255., 38 / 255., Color.a)) {
        vertexColor = vec4(1.0, 1.0, 1.0, vertexColor.a);
        return;
    }

    // Remove the color from the vertical shift color so it only reflects the underlying texture.
    if (Color == vec4(78 / 255., 90 / 255., Color.b, Color.a)) {
        if (Color.a == (80. / 255.)) {
            vertexColor = vec4(0, 0, 0, Color.a);
        } else {
            vertexColor = vec4(1.0, 1.0, 1.0, vertexColor.a);
        }
    }
    // TODO: we need to also handle the shadow color here.
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
