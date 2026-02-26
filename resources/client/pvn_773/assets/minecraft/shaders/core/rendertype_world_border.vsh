#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
out vec3 worldPos;

void main() {
    worldPos = Position + ModelOffset + vec3(0.0, 1.0, 0.0);
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
    texCoord0 = UV0.xy;
}
