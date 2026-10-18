#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vertexColor;
out vec2 texCoord0;

// MAPMAKER START
uniform sampler2D Sampler0;

vec2 cornerOf(vec2 texel) {
    return step(0.5, fract(texel));
}

ivec3 dataPixel(vec2 texel, vec2 cornerDir, int index) {
    ivec2 pos = ivec2(floor(texel));
    pos.x -= index * int(cornerDir.x);
    return ivec3(round(texelFetch(Sampler0, pos, 0).rgb * 255.0));
}

// x: 1 for a line, y: its opacity flags (WireLine#opacity).
flat out vec2 wireInfo;
// x: 0 at the start to 1 at the end, y: world distance along the line.
out vec2 wireCoord;
// 0 to 1 across. The width is added in screen space, so this has to interpolate in screen space
// too, otherwise the nearer end's depth drags the middle of the line towards the quad's diagonal.
noperspective out float wireAcross;
// The glyph's style colour, stored in the pixel beside each marker corner.
flat out vec3 wireTint;

const ivec3 WIRE_ID = ivec3(0xFE, 0x4E, 0x2C);
const ivec3 BOX_ID = ivec3(0xFE, 0x4F, 0x2C);
// The line is 2px; the quad is a little wider so its edges can be antialiased.
const float WIRE_WIDTH_PX = 4.0;
const float WIRE_LINE_PX = 2.0;

// Text opacity flags (WireLine#opacity), offset so vanilla never treats the text as invisible.
const int FLAGS_BASE = 240;
const int FLAG_SOLID = 1;
const int FLAG_BLOCK_UNITS = 2;
const int FLAG_XRAY = 4;

// Places this vertex of a line through pos (the start for the glyph's left corners, the end for its
// right ones) and widens it in screen space like rendertype_lines, so both ends offset along the same
// projected normal.
void line(vec3 pos, vec3 dir, vec2 corner, int flags, vec3 tint) {
    vec4 clipPos = ProjMat * ModelViewMat * vec4(pos, 1.0);
    // Sample the direction from a point inside the line, which is in front of the camera
    // whenever the line is, rather than past its end.
    float inward = corner.x < 0.5 ? 1.0 : -1.0;
    vec4 clipAhead = ProjMat * ModelViewMat * vec4(pos + dir * (0.25 * inward), 1.0);
    vec2 screenDir = inward * (clipAhead.xy / clipAhead.w - clipPos.xy / clipPos.w) * ScreenSize;
    screenDir = dot(screenDir, screenDir) < 1e-6 ? vec2(1.0, 0.0) : normalize(screenDir);
    // Text is back face culled, so the glyph's top row (corner.y 0) has to land counter
    // clockwise of the line on screen to keep the quad front facing.
    vec2 normal = vec2(-screenDir.y, screenDir.x);
    // Each end also reaches half the line past its point, so two lines meeting at a corner overlap instead
    // of leaving a notch on its outside.
    vec2 offset = normal * (0.5 - corner.y) * WIRE_WIDTH_PX - inward * screenDir * WIRE_LINE_PX * 0.5;
    gl_Position = vec4(clipPos.xy + offset * 2.0 / ScreenSize * clipPos.w, clipPos.zw);

    vec3 worldPos = pos + vec3(CameraBlockPos) - CameraOffset;
    wireInfo = vec2(1.0, float(flags));
    wireCoord = vec2(corner.x, dot(worldPos, dir));
    wireAcross = corner.y;
    wireTint = tint;
    vertexColor = vec4(1.0);
}
// MAPMAKER END

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;

    // MAPMAKER START
    wireInfo = vec2(0.0, -1.0);
    wireCoord = vec2(0.0);
    wireAcross = 0.0;
    wireTint = vec3(0.0);

    vec2 texel = UV0 * vec2(textureSize(Sampler0, 0));
    vec2 corner = cornerOf(texel);
    vec2 cornerDir = corner * 2.0 - 1.0;
    ivec3 marker = dataPixel(texel, cornerDir, 0);
    if (marker == WIRE_ID || marker == BOX_ID) {
        ivec4 icol = ivec4(round(Color * 255.0));
        int flags = icol.a - FLAGS_BASE;
        vec3 tint = vec3(dataPixel(texel, cornerDir, 1)) / 255.0;
        if (marker == WIRE_ID) {
            // The server squashes the glyph to a zero height quad along the wire and sends the wire's
            // direction as the text color, so every vertex already sits on the wire's axis.
            line(Position, normalize(vec3(icol.rgb) / 127.5 - 1.0), corner, flags, tint);
        } else {
            // All twelve edge glyphs are squashed to a point at the box's min corner and the text color
            // carries its size, so each builds its edge from the index in its data pixel: the axis it runs
            // along, then which side of the box it sits on for each of the other two axes.
            vec3 size = vec3(icol.rgb) * ((flags & FLAG_BLOCK_UNITS) != 0 ? 1.0 : 1.0 / 16.0);
            int edge = dataPixel(texel, cornerDir, 2).r;
            int axis = edge / 4;
            vec3 along = vec3(equal(ivec3(axis), ivec3(0, 1, 2)));
            vec3 u = vec3(equal(ivec3((axis + 1) % 3), ivec3(0, 1, 2)));
            vec3 v = vec3(equal(ivec3((axis + 2) % 3), ivec3(0, 1, 2)));
            vec3 start = Position + (u * float(edge & 1) + v * float((edge >> 1) & 1)) * size;
            line(start + along * size * corner.x, along, corner, flags, tint);
        }
    }
    // MAPMAKER END
}
