package net.hollowcube.mapmaker.map.util.debug;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Not thread safe, use from player tick thread.
public final class DebugDraw {
    private static final Tag<DebugDraw> TAG = Tag.Transient("mapmaker:debug_draw");

    public static DebugDraw of(Player player) {
        FutureUtil.assertTickThread(player.acquirable());

        var draw = player.getTag(TAG);
        if (draw == null || draw.instance != player.getInstance()) {
            // Pending expiries of the old one find nothing left to remove.
            if (draw != null) draw.shapes.clear();
            draw = new DebugDraw(player);
            player.setTag(TAG, draw);
        }
        return draw;
    }

    private final Player player;
    private final @Nullable Instance instance;
    private final Map<String, IntList> shapes = new HashMap<>();

    private DebugDraw(Player player) {
        this.player = player;
        this.instance = player.getInstance();
    }

    public void draw(String key, DebugShape shape) {
        drawInternal(key, shape);
    }

    public void draw(String key, DebugShape shape, int ticks) {
        var entityIds = drawInternal(key, shape);

        player.scheduler()
            .buildTask(() -> {
                // Only if it hasn't been redrawn or removed already
                if (shapes.get(key) != entityIds) return;

                remove(key);
            })
            .delay(TaskSchedule.tick(ticks))
            .schedule();
    }

    public void remove(String key) {
        var entityIds = shapes.remove(key);
        if (entityIds == null) return;

        player.sendPacket(new DestroyEntitiesPacket(entityIds));
    }

    public void clear(String prefix) {
        var ids = new IntArrayList();
        shapes.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix))
                return false;

            ids.addAll(entry.getValue());
            return true;
        });
        if (ids.isEmpty()) return;

        player.sendPacket(new DestroyEntitiesPacket(ids));
    }

    private IntArrayList drawInternal(String key, DebugShape shape) {
        // This "should" be bundled but kinda dont care if metadata draws late
        // because the text display will otherwise be invisible on the first tick.

        remove(key);

        var packets = new ArrayList<SendablePacket>();
        var entityIds = new IntArrayList();
        Encoding.spawn(shape, entityIds, packets);

        shapes.put(key, entityIds);
        player.sendPackets(packets);
        return entityIds;
    }

    /// How shapes become text displays for the wire branch of the text.vsh overlays.
    static final class Encoding {
        static final int EDGES = 12;
        static final Key FONT = Key.key("mapmaker", "wire");

        // The wire glyph is 8x8 at 1/40 block per pixel, 9 wide with its advance, and the text display centers that
        // advance, so the glyph spans x -0.0875..0.1125 and y 0.025..0.225 locally.
        static final double GLYPH_SIZE = 0.2;
        static final Vec GLYPH_START = new Vec(-0.0875, 0.125, 0);
        private static final double FLAT = 0.001;
        private static final double POINT = 0.001;
        private static final double MIN_LENGTH = 0.01;
        private static final float[] NO_ROTATION = {0, 0, 0, 1};

        // Text opacity flags, offset so vanilla never treats the text as invisible.
        private static final int FLAGS_BASE = 240;
        private static final int FLAG_SOLID = 1;
        private static final int FLAG_BLOCK_UNITS = 2;
        private static final int FLAG_XRAY = 4;

        private static final int MAX_UNITS = 255;
        private static final int SIXTEENTHS = 16;

        private static final byte DISPLAY_SEE_THROUGH = 0x02;

        /// Appends the packets spawning `shape`, and the ids of the entities they spawn.
        static void spawn(DebugShape shape, IntList entityIds, List<SendablePacket> packets) {
            switch (shape) {
                case DebugShape.Line(var start, var end, var color, var dashed, var xray, var viewRange) ->
                    line(entityIds, packets, start, end, color, flags(dashed, xray ? FLAG_XRAY : 0), xray, viewRange);
                case DebugShape.Box(var min, var max, var color, var dashed, var xray, var viewRange) -> {
                    var extent = max.sub(min).asVec();
                    if (unitsPerBlock(extent) != 0) {
                        box(entityIds, packets, min, max, color, dashed, xray, viewRange);
                        return;
                    }
                    for (int edge = 0; edge < EDGES; edge++) {
                        spawn(new DebugShape.Line(corner(min, extent, edge, 0), corner(min, extent, edge, 1), color, dashed, xray, viewRange),
                            entityIds, packets);
                    }
                }
            }
        }

        /// The same edge numbering as the shader: the axis it runs along, then which side of the box it sits on for each
        /// of the other two axes.
        static Point corner(Point min, Vec extent, int edge, int end) {
            int axis = edge / 4;
            double[] offset = new double[3];
            offset[axis] = end;
            offset[(axis + 1) % 3] = edge & 1;
            offset[(axis + 2) % 3] = (edge >> 1) & 1;
            return min.add(offset[0] * extent.x(), offset[1] * extent.y(), offset[2] * extent.z());
        }

        static void line(IntList entityIds, List<SendablePacket> packets, Point start, Point end, WireColor color, byte opacity, boolean xray, float viewRange) {
            var delta = end.sub(start).asVec();
            var direction = delta.isZero() ? new Vec(1, 0, 0) : delta.normalize();
            var length = Math.max(delta.length(), MIN_LENGTH);
            var rotation = rotationFromX(direction);
            var scale = new Vec(length / GLYPH_SIZE, FLAT / GLYPH_SIZE, 1);
            var position = start.add(end).asVec().div(2).asPos();
            var translation = start.sub(position).asVec().sub(rotate(rotation, GLYPH_START.mul(scale)));
            var text = Component.text(color.lineGlyph()).font(FONT).color(encodeDirection(direction));
            display(entityIds, packets, position, translation, rotation, scale, text, opacity, xray, viewRange);
        }

        static void box(IntList entityIds, List<SendablePacket> packets, Point min, Point max, WireColor color, boolean dashed, boolean xray, float viewRange) {
            var extent = max.sub(min).asVec();
            int perBlock = unitsPerBlock(extent);
            var size = encodeSize(extent, perBlock);
            var opacity = flags(dashed, (perBlock == 1 ? FLAG_BLOCK_UNITS : 0) | (xray ? FLAG_XRAY : 0));
            var position = min.add(max).asVec().div(2).asPos();
            var text = Component.text(color.boxGlyphs()).font(FONT).color(size);
            display(entityIds, packets, position, min.sub(position).asVec(), NO_ROTATION, new Vec(POINT), text, opacity, xray, viewRange);
        }

        // Through walls is a second, see-through copy; the shader draws it fainter so the normal copy still reads
        // as the real outline where nothing is in front of it.
        //
        // Width and height are left at 0, which turns off the client's frustum culling for the display. The client
        // still skips any entity whose position is in a render section it isn't drawing (eg underground, behind cave
        // culling), so callers place it at the middle of the shape rather than somewhere it could be hidden.
        private static void display(IntList entityIds, List<SendablePacket> packets, Pos position, Vec translation,
                                    float[] rotation, Vec scale, Component text, byte opacity, boolean xray, float viewRange) {
            for (boolean seeThrough : xray ? new boolean[]{false, true} : new boolean[]{false}) {
                var meta = new HashMap<Integer, Metadata.Entry<?>>();
                meta.put(MetadataDef.TextDisplay.BACKGROUND_COLOR.index(), Metadata.VarInt(0));
                meta.put(MetadataDef.TextDisplay.TEXT_DISPLAY_FLAGS.index(), Metadata.Byte(seeThrough ? DISPLAY_SEE_THROUGH : 0));
                meta.put(MetadataDef.TextDisplay.TEXT_OPACITY.index(), Metadata.Byte(opacity));
                meta.put(MetadataDef.TextDisplay.TEXT.index(), Metadata.Component(text));
                meta.put(MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(translation));
                meta.put(MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(rotation));
                meta.put(MetadataDef.Display.SCALE.index(), Metadata.Vector3(scale));
                meta.put(MetadataDef.Display.VIEW_RANGE.index(), Metadata.Float(viewRange));

                int entityId = Entity.generateId();
                entityIds.add(entityId);
                packets.add(new SpawnEntityPacket(entityId, UUID.randomUUID(), EntityType.TEXT_DISPLAY, position, 0, 0, Vec.ZERO));
                packets.add(new EntityMetaDataPacket(entityId, meta));
            }
        }

        static byte flags(boolean dashed, int flags) {
            return (byte) (FLAGS_BASE | flags | (dashed ? 0 : FLAG_SOLID));
        }

        /// The unit the box size is sent in, as units per block, or 0 if it is too big for either.
        static int unitsPerBlock(Vec extent) {
            if (fits(extent, SIXTEENTHS)) return SIXTEENTHS;
            if (fits(extent, 1)) return 1;
            return 0;
        }

        private static boolean fits(Vec extent, int perBlock) {
            return Math.round(extent.x() * perBlock) <= MAX_UNITS && Math.round(extent.y() * perBlock) <= MAX_UNITS
                && Math.round(extent.z() * perBlock) <= MAX_UNITS;
        }

        private static TextColor encodeSize(Vec extent, int perBlock) {
            return TextColor.color(units(extent.x(), perBlock), units(extent.y(), perBlock), units(extent.z(), perBlock));
        }

        private static int units(double value, int perBlock) {
            return (int) Math.clamp(Math.round(value * perBlock), 0, MAX_UNITS);
        }

        private static TextColor encodeDirection(Vec direction) {
            return TextColor.color(axis(direction.x()), axis(direction.y()), axis(direction.z()));
        }

        private static int axis(double value) {
            return (int) Math.round(Math.clamp((value + 1) * 127.5, 0, 255));
        }

        private static float[] rotationFromX(Vec direction) {
            var dot = direction.x();
            if (dot < -0.9999) return new float[]{0, 1, 0, 0};
            // Shortest arc from +X: axis = X cross d = (0, -dz, dy), w = 1 + X dot d.
            var y = -direction.z();
            var z = direction.y();
            var w = 1 + dot;
            var norm = Math.sqrt(y * y + z * z + w * w);
            return new float[]{0, (float) (y / norm), (float) (z / norm), (float) (w / norm)};
        }

        static Vec rotate(float[] q, Vec v) {
            var u = new Vec(q[0], q[1], q[2]);
            var w = q[3];
            var t = u.cross(v).mul(2);
            return v.add(t.mul(w)).add(u.cross(t));
        }

        private Encoding() {
        }
    }
}
