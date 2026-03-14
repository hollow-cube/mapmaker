package net.hollowcube.compat.moulberrytweaks.debugrender;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.List;

public sealed interface DebugShape {
    NetworkBuffer.Type<DebugShape> NETWORK_TYPE = NetworkBuffer.STRING.unionType(DebugShape::typeFromName, DebugShape::type);

    int FLAG_SHOW_THROUGH_WALLS = 1;
    int FLAG_FULL_OPACITY_BEHIND_WALLS = 2;
    int FLAG_WIREFRAME = 4;
    int FLAG_NO_SHADE = 8;
    int FLAG_SHOW_AXIS = 16;

    record Box(
        Point center, Point size, float[] rotation,
        int faceArgb, int lineArgb, float lineThickness
    ) implements DebugShape {
        public static final String TYPE = "box";
        public static final NetworkBuffer.Type<Box> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.VECTOR3D, Box::center,
            NetworkBuffer.VECTOR3D, Box::size,
            NetworkBuffer.QUATERNION, Box::rotation,
            NetworkBuffer.INT, Box::faceArgb,
            NetworkBuffer.INT, Box::lineArgb,
            NetworkBuffer.FLOAT, Box::lineThickness,
            Box::new);

        @Override
        public String type() {
            return TYPE;
        }
    }

    record Ellipsoid(
        Point center, Point size, float[] rotation,
        int argb, int detail
    ) implements DebugShape {
        public static final String TYPE = "ellipsoid";
        public static final NetworkBuffer.Type<Ellipsoid> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.VECTOR3D, Ellipsoid::center,
            NetworkBuffer.VECTOR3D, Ellipsoid::size,
            NetworkBuffer.QUATERNION, Ellipsoid::rotation,
            NetworkBuffer.INT, Ellipsoid::argb,
            NetworkBuffer.VAR_INT, Ellipsoid::detail,
            Ellipsoid::new);

        @Override
        public String type() {
            return TYPE;
        }
    }

    record LineStrip(List<Point> points, int argb, float lineThickness) implements DebugShape {
        public static final String TYPE = "line_strip";
        public static final NetworkBuffer.Type<LineStrip> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.VECTOR3D.list(), LineStrip::points,
            NetworkBuffer.INT, LineStrip::argb,
            NetworkBuffer.FLOAT, LineStrip::lineThickness,
            LineStrip::new);

        @Override
        public String type() {
            return TYPE;
        }
    }

    record Quad(
        Point one, Point two,
        Point three, Point four,
        int argb
    ) implements DebugShape {
        public static final String TYPE = "quad";
        public static final NetworkBuffer.Type<Quad> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.VECTOR3D, Quad::one,
                NetworkBuffer.VECTOR3D, Quad::two,
                NetworkBuffer.VECTOR3D, Quad::three,
                NetworkBuffer.VECTOR3D, Quad::four,
                NetworkBuffer.INT, Quad::argb,
                Quad::new);

        @Override
        public String type() {
            return TYPE;
        }
    }

    record Text(
        Point position, Component component,
        boolean shadow, int backgroundColor
    ) implements DebugShape {
        public static final String TYPE = "text";
        public static final NetworkBuffer.Type<Text> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.VECTOR3D, Text::position,
                NetworkBuffer.COMPONENT, Text::component,
                NetworkBuffer.BOOLEAN, Text::shadow,
                NetworkBuffer.INT, Text::backgroundColor,
                Text::new);

        @Override
        public String type() {
            return TYPE;
        }
    }

    String type();

    @SuppressWarnings({"unchecked", "RedundantCast"})
    private static NetworkBuffer.Type<DebugShape> typeFromName(String type) {
        // Gross cast will be fixed once minestom correctly takes ? extends DebugShape
        return (NetworkBuffer.Type<DebugShape>) (Object) switch (type) {
            case Box.TYPE -> Box.NETWORK_TYPE;
            case Ellipsoid.TYPE -> Ellipsoid.NETWORK_TYPE;
            case LineStrip.TYPE -> LineStrip.NETWORK_TYPE;
            case Quad.TYPE -> Quad.NETWORK_TYPE;
            case Text.TYPE -> Text.NETWORK_TYPE;
            default -> throw new IllegalArgumentException("Unknown debug shape type: " + type);
        };
    }
}
