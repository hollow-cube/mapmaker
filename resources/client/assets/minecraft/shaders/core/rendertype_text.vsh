#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec2 ScreenSize;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

#define CHAT_TEXT (100.03)
#define OTHER_TEXT (0.03)

#define CHAT_SHADOW (100)
#define OTHER_SHADOW (0)

void main() {
    vec3 pos = vec3(Position.xyz);

    // More book handling
    if (Color == vec4(78/255., 92/255., 38/255., Color.a)) {
        pos.x -= 9;
    } else if (Color == vec4(78/255., 92/255., 40/255., Color.a)) {
        // Cursor icon in center of screen
        int gui_scale = int(round(ScreenSize.x * ProjMat[0][0] / 2));
        vec2 half_screen = ScreenSize / gui_scale / 2;

        pos.x -= 3;
        pos.y += 65 + 15;
        pos.y -= half_screen.y;
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

    // Shadow handling
    if (Color == vec4(78/255., 92/255., 36/255., Color.a) || Color == vec4(78/255., 92/255., 40/255., Color.a)) { //  && (Position.z == CHAT_TEXT || Position.z == OTHER_TEXT)
        vertexColor = vec4(texelFetch(Sampler2, UV2 / 16, 0).xyz, vertexColor.a);// remove color from no shadow marker
    } else if (Color == vec4(19/255., 23/255., 9/255., Color.a) || Color == vec4(19/255., 23/255., 10/255., Color.a)) { //  && (Position.z == CHAT_SHADOW || Position.z == OTHER_SHADOW)
        vertexColor = vec4(0);// remove shadow
    }
}
