package net.hollowcube.mapmaker.map.util.debug;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class LineGeometryTest {

    @Test
    void wireGlyphSpansStartToEnd() {
        var random = new Random(42);
        var cases = new ArrayList<Point[]>(List.of(
            new Point[]{new Vec(0, 0, 0), new Vec(1, 0, 0)},
            new Point[]{new Vec(0, 0, 0), new Vec(-3, 0, 0)}, // the 180 degree rotation
            new Point[]{new Vec(5, 64, 5), new Vec(5, 70, 5)},
            new Point[]{new Vec(1000.5, 40, -2000.25), new Vec(1004, 39, -1990)}
        ));
        for (int i = 0; i < 100; i++) {
            cases.add(new Point[]{randomPoint(random), randomPoint(random)});
        }

        for (var points : cases) {
            var packets = new ArrayList<SendablePacket>();
            DebugDraw.Encoding.spawn(new DebugShape.Line(points[0], points[1], WireColor.WHITE), new IntArrayList(), packets);
            var spawn = (SpawnEntityPacket) packets.get(0);
            var meta = ((EntityMetaDataPacket) packets.get(1)).entries();
            var glyphStart = DebugDraw.Encoding.GLYPH_START;
            var glyphEnd = glyphStart.add(DebugDraw.Encoding.GLYPH_SIZE, 0, 0);
            assertClose(points[0], world(spawn.position(), meta, glyphStart));
            assertClose(points[1], world(spawn.position(), meta, glyphEnd));
        }
    }

    @Test
    void boxEdgesCoverEveryEdgeOnce() {
        var extent = new Vec(1, 2, 3);
        var edges = new HashSet<String>();
        for (int edge = 0; edge < 12; edge++) {
            var start = DebugDraw.Encoding.corner(Vec.ZERO, extent, edge, 0);
            var end = DebugDraw.Encoding.corner(Vec.ZERO, extent, edge, 1);
            var delta = end.sub(start);
            int axis = edge / 4;
            assertEquals(axisValue(extent, axis), axisValue(delta, axis), 1e-9);
            assertEquals(axisValue(extent, axis), delta.distance(Vec.ZERO), 1e-9, "only moves along its axis");
            edges.add(start + "->" + end);
        }
        assertEquals(12, edges.size());
    }

    @Test
    void boxSizeUnits() {
        assertEquals(16, DebugDraw.Encoding.unitsPerBlock(new Vec(15.9375, 0.5, 14 / 16.0)));
        assertEquals(16, DebugDraw.Encoding.unitsPerBlock(new Vec(0.6, 1.8, 0.6)), "rounded to sixteenths");
        assertEquals(1, DebugDraw.Encoding.unitsPerBlock(new Vec(16.5, 0.5, 1)));
        assertEquals(1, DebugDraw.Encoding.unitsPerBlock(new Vec(255, 1, 1)));
        assertEquals(0, DebugDraw.Encoding.unitsPerBlock(new Vec(256, 1, 1)));
    }

    @Test
    void stylesMatchFontDefinition() throws Exception {
        // The packer builds the mapmaker:wire glyphs in this file's order, which the glyph code points rely on.
        var definition = Files.readString(Path.of("../../resources/wire_font.json5"));
        var names = Pattern.compile("\"name\":\\s*\"([a-z_]+)\"").matcher(definition).results()
            .map(match -> match.group(1).toUpperCase(Locale.ROOT)).toList();
        assertEquals(Arrays.stream(WireColor.values()).map(Enum::name).toList(), names);
    }

    private static Vec world(Pos position, Map<Integer, Metadata.Entry<?>> meta, Vec local) {
        var translation = (Point) meta.get(MetadataDef.Display.TRANSLATION.index()).value();
        var rotation = (float[]) meta.get(MetadataDef.Display.ROTATION_LEFT.index()).value();
        var scale = (Point) meta.get(MetadataDef.Display.SCALE.index()).value();
        var scaled = new Vec(local.x() * scale.x(), local.y() * scale.y(), local.z() * scale.z());
        return position.asVec().add(translation).add(DebugDraw.Encoding.rotate(rotation, scaled));
    }

    private static double axisValue(Point point, int axis) {
        return switch (axis) {
            case 0 -> point.x();
            case 1 -> point.y();
            default -> point.z();
        };
    }

    private static Point randomPoint(Random random) {
        return new Vec(random.nextDouble(-50, 50), random.nextDouble(0, 100), random.nextDouble(-50, 50));
    }

    private static void assertClose(Point expected, Point actual) {
        // The rotation is sent as floats.
        assertTrue(expected.distance(actual) < 1e-3, () -> "expected " + expected + " but was " + actual);
    }
}
