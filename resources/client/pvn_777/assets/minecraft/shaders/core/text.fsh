#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// MAPMAKER START
#ifdef IS_GUI
layout(location = 4) flat in vec4 hoverCutout;
layout(location = 5) in vec2 screenPos;
#else
#include <minecraft:globals.glsl>

layout(location = 4) flat in vec2 wireInfo;
layout(location = 5) in vec2 wireCoord;
layout(location = 6) noperspective in float wireAcross;
layout(location = 7) flat in vec3 wireTint;

const float WIRE_WIDTH_PX = 4.0;
const float WIRE_LINE_PX = 2.0;

vec4 wireColor() {
    float px = abs(wireAcross - 0.5) * WIRE_WIDTH_PX;
    float line = 1.0 - smoothstep(WIRE_LINE_PX * 0.5 - 0.5, WIRE_LINE_PX * 0.5 + 0.5, px);

    // 0.5 block dashes flowing start to end at a block a second. 24000 ticks moves the pattern a
    // whole number of dashes, so the day wrap in GameTime doesn't jump. The dash edges are antialiased over
    // a pixel, and the pattern fades to its average once a dash is only a few pixels long, otherwise far
    // wires alias into a shimmering blur.
    float phase = (wireCoord.y - GameTime * 24000.0 * 0.05) / 0.5;
    float perPixel = fwidth(phase);
    // Dashes are the [0.5, 1) half of each period; shifted so they centre on whole numbers, the distance to
    // the nearest one gives a signed distance to the dash edge.
    float shifted = phase + 0.25;
    float inside = 0.25 - abs(shifted - round(shifted));
    float dash = clamp(0.5 + inside / max(perPixel, 1e-4), 0.0, 1.0);
    dash = mix(dash, 0.5, smoothstep(0.1, 0.3, perPixel));
    int flags = int(wireInfo.y + 0.5);
    if ((flags & 1) != 0) dash = 1.0;
    vec3 rgb = wireTint * mix(0.6, 1.0, dash);

    float alpha = line;
    #ifdef IS_SEE_THROUGH
    // Wires are faint through walls; the selection's outline (flag 4) stays clearly visible.
    alpha *= (flags & 4) != 0 ? 0.7 : 0.25;
    #endif
    return vec4(rgb, alpha);
}
#endif
// MAPMAKER END

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif

    #if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)

    #ifdef OIT_ACCUMULATE
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
    #endif

    return color;
}

void main() {
    #ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
    #else
    vec4 texColor = texture(Sampler0, texCoord0);
    #endif

    vec4 color = texColor * vertexColor * ColorModulator;

    // MAPMAKER START
    #ifndef IS_GUI
    if (wireInfo.x > 0.5) {
        color = wireColor();
        if (color.a < 0.01) {
            discard;
        }
    } else
    #endif
    // MAPMAKER END
    if (color.a < 0.1) {
        discard;
    }

    // MAPMAKER START
    #ifdef IS_GUI
    // Hide the part of a repositioned hover icon that sits behind the tooltip it rode in on.
    if (all(greaterThan(screenPos, hoverCutout.xy)) && all(lessThan(screenPos, hoverCutout.zw))) {
        discard;
    }
    #endif
    // MAPMAKER END

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
