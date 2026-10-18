#version 150

#define IS_SEE_THROUGH

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// MAPMAKER START
flat in vec2 wireInfo;
in vec2 wireCoord;
noperspective in float wireAcross;
flat in vec3 wireTint;

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
// MAPMAKER END

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    // MAPMAKER START
    if (wireInfo.x > 0.5) {
        color = wireColor();
        if (color.a < 0.01) {
            discard;
        }
    } else
    // MAPMAKER END
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}
