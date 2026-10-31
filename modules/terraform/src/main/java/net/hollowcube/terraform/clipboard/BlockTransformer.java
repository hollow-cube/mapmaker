package net.hollowcube.terraform.clipboard;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;

import java.util.*;

/// Transforms blocks under a [Transform]'s linear part: every orientable vanilla state property
/// (`facing`, `axis`, `rotation`, `orientation`, the multiface sides, `half`, `type`, `shape`, `hinge`,
/// `face`, `attachment`). Chiral properties (stair corners, chest `type`, door
/// `hinge`, `half`, `face`, `attachment`) swap only under the matching reflection.
///
/// Direction-valued properties become a unit vector, are transformed, and snap to the nearest allowed
/// direction. Transformed states are memoized by state id. Not thread-safe: one instance per operation.
@SuppressWarnings("DataFlowIssue")
final class BlockTransformer {
    private static final List<String> SIDES = List.of(
        "north",
        "east",
        "south",
        "west",
        "up",
        "down"
    );

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
    };
    private static final Direction[] ALL = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
        Direction.UP,
        Direction.DOWN,
    };

    private static final Vec[] ROTATION_16 = new Vec[16]; // 16 step rotation mapping
    private static final BitSet ORIENTED_STATES = new BitSet(); // States with relevant properties
    // Allowed facing/orientation/shape values of each oriented block, by block id
    private static final Int2ObjectMap<LegalValues> LEGAL_VALUES = new Int2ObjectOpenHashMap<>();

    static {
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i * 22.5);
            ROTATION_16[i] = new Vec(-Math.sin(a), 0, Math.cos(a));
        }

        Set<String> oriented = Set.of(
            "facing",
            "axis",
            "rotation",
            "orientation",
            "north",
            "east",
            "south",
            "west",
            "up",
            "down",
            "half",
            "type",
            "shape",
            "hinge",
            "face",
            "attachment"
        );
        for (Block block : Block.values()) {
            if (Collections.disjoint(block.properties().keySet(), oriented)) continue;

            var facings = new LinkedHashSet<String>();
            var orientations = new HashSet<String>();
            var shapes = new HashSet<String>();
            for (Block state : block.possibleStates()) {
                ORIENTED_STATES.set(state.stateId());
                addValue(state, "facing", facings);
                addValue(state, "orientation", orientations);
                addValue(state, "shape", shapes);
            }

            var facingDirections = new Direction[facings.size()];
            int i = 0;
            for (String facing : facings) facingDirections[i++] = str2dir(facing);
            LEGAL_VALUES.put(
                block.id(),
                new LegalValues(facingDirections, Set.copyOf(orientations), Set.copyOf(shapes))
            );
        }
    }

    private final Transform t;
    private final boolean identity;
    private final boolean horizontalFlip;
    private final boolean verticalFlip;
    private final Int2IntOpenHashMap stateCache = new Int2IntOpenHashMap();

    BlockTransformer(Transform t) {
        this.t = t;
        this.identity = t.isLinearIdentity();
        this.horizontalFlip = t.isHorizontalFlip();
        this.verticalFlip = t.isVerticalFlip();
        this.stateCache.defaultReturnValue(-1);
    }

    int transformState(int stateId) {
        if (identity || !ORIENTED_STATES.get(stateId)) return stateId;

        int cached = stateCache.get(stateId);
        if (cached != -1) return cached;

        int result = transformProperties(Block.fromStateId(stateId)).stateId();
        stateCache.put(stateId, result);
        return result;
    }

    Block transform(Block block) {
        if (identity || !ORIENTED_STATES.get(block.stateId())) return block;

        var result = Block.fromStateId(transformState(block.stateId()));
        if (block.nbt() != null) result = result.withNbt(block.nbt());
        if (block.handler() != null) result = result.withHandler(block.handler());
        return result;
    }

    private Block transformProperties(Block block) {
        Map<String, String> in = block.properties();
        Map<String, String> out = new HashMap<>(in);

        boolean multiface = false;
        for (Map.Entry<String, String> entry : in.entrySet()) {
            String value = entry.getValue();
            switch (entry.getKey()) {
                case "facing" -> facing(block, value, out);
                case "axis" -> out.put("axis", axis(value, t));
                case "rotation" -> out.put(
                    "rotation",
                    Integer.toString(rotation(Integer.parseInt(value), t))
                );
                case "orientation" -> orientation(block, value, out);
                case "north", "east", "south", "west", "up", "down" -> multiface = true;
                case "half" -> {
                    swapOnFlip(verticalFlip, "half", value, "top", "bottom", out);
                    swapOnFlip(verticalFlip, "half", value, "upper", "lower", out);
                }
                case "type" -> {
                    // Slab top/bottom is vertical chirality, chest left/right is horizontal.
                    swapOnFlip(verticalFlip, "type", value, "top", "bottom", out);
                    swapOnFlip(horizontalFlip, "type", value, "left", "right", out);
                }
                case "shape" -> shape(block, value, out);
                case "hinge" -> swapOnFlip(horizontalFlip, "hinge", value, "left", "right", out);
                case "face" -> swapOnFlip(verticalFlip, "face", value, "floor", "ceiling", out);
                case "attachment" -> swapOnFlip(
                    verticalFlip,
                    "attachment",
                    value,
                    "floor",
                    "ceiling",
                    out
                );
                default -> {}
            }
        }
        if (multiface) multiface(in, out);
        return block.withProperties(out);
    }

    private void facing(Block block, String facing, Map<String, String> out) {
        Direction[] legal = legalValues(block).facing();
        out.put("facing", dir2str(snap(str2dir(facing), t, legal)));
    }

    private void orientation(Block block, String orientation, Map<String, String> out) {
        String[] parts = orientation.split("_", 0);
        if (parts.length != 2) return;
        String front = dir2str(snapOmni(str2dir(parts[0]), t));
        String top = dir2str(snapOmni(str2dir(parts[1]), t));
        String candidate = front + "_" + top;
        if (legalValues(block).orientation().contains(candidate)) {
            out.put("orientation", candidate);
        }
    }

    private void multiface(Map<String, String> in, Map<String, String> out) {
        Map<String, String> moved = new HashMap<>();
        for (String side : SIDES) {
            String value = in.get(side);
            if (value == null) continue;
            String newSide = dir2str(snapOmni(str2dir(side), t));
            if (in.containsKey(newSide)) moved.put(newSide, value);
        }
        for (String side : SIDES) {
            if (!in.containsKey(side)) continue;
            out.put(side, moved.getOrDefault(side, in.get(side)));
        }
    }

    private void shape(Block block, String shape, Map<String, String> out) {
        boolean stair = shape.equals("straight")
            || shape.startsWith("inner_")
            || shape.startsWith("outer_");
        if (!stair) {
            out.put("shape", railShape(block, shape));
        } else if (horizontalFlip) {
            out.put("shape", swapLeftRight(shape));
        }
    }

    private String railShape(Block block, String shape) {
        Set<String> legal = legalValues(block).shape();
        String[] parts = shape.split("_", 0);
        if (parts.length != 2) return shape;
        if (parts[0].equals("ascending")) {
            String candidate = "ascending_" + dir2str(snapHorizontal(str2dir(parts[1]), t));
            return legal.contains(candidate) ? candidate : shape;
        }
        String a = dir2str(snapHorizontal(str2dir(parts[0]), t));
        String b = dir2str(snapHorizontal(str2dir(parts[1]), t));
        if (legal.contains(a + "_" + b)) return a + "_" + b;
        if (legal.contains(b + "_" + a)) return b + "_" + a;
        return shape;
    }

    /// Snaps to the nearest of the six directions.
    static Direction snapOmni(Direction d, Transform t) {
        return snap(d, t, ALL);
    }

    /// Snaps to the nearest of the four horizontal directions.
    static Direction snapHorizontal(Direction d, Transform t) {
        return snap(d, t, HORIZONTAL);
    }

    /// Snaps to the nearest of `candidates`, which must not be empty.
    static Direction snap(Direction d, Transform t, Direction[] candidates) {
        Vec v = t.applyDirection(d.normalX(), d.normalY(), d.normalZ());
        Direction best = candidates[0];
        double bestDot = -Double.MAX_VALUE;
        for (Direction candidate : candidates) {
            double dot = candidate.normalX() * v.x()
                + candidate.normalY() * v.y()
                + candidate.normalZ() * v.z();
            if (dot > bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }
        return best;
    }

    static String axis(String axis, Transform t) {
        Direction proxy = switch (axis) {
            case "x" -> Direction.EAST;
            case "y" -> Direction.UP;
            default -> Direction.NORTH;
        };
        return switch (snapOmni(proxy, t)) {
            case EAST, WEST -> "x";
            case UP, DOWN -> "y";
            case NORTH, SOUTH -> "z";
        };
    }

    static int rotation(int rotation, Transform t) {
        Vec from = ROTATION_16[Math.floorMod(rotation, 16)];
        Vec v = t.applyDirection(from.x(), from.y(), from.z());
        int best = 0;
        double bestDot = -Double.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            double dot = ROTATION_16[i].x() * v.x() + ROTATION_16[i].z() * v.z();
            if (dot > bestDot) {
                bestDot = dot;
                best = i;
            }
        }
        return best;
    }

    private static void swapOnFlip(
        boolean flip,
        String key,
        String value,
        String a,
        String b,
        Map<String, String> out
    ) {
        if (!flip) return;
        if (value.equals(a)) out.put(key, b);
        else if (value.equals(b)) out.put(key, a);
    }

    private static String swapLeftRight(String shape) {
        if (shape.endsWith("_left")) return shape.substring(0, shape.length() - 4) + "right";
        if (shape.endsWith("_right")) return shape.substring(0, shape.length() - 5) + "left";
        return shape;
    }

    private static LegalValues legalValues(Block block) {
        return Objects.requireNonNull(LEGAL_VALUES.get(block.id()), "oriented block");
    }

    private static void addValue(Block state, String property, Set<String> values) {
        String value = state.getProperty(property);
        if (value != null) values.add(value);
    }

    private static Direction str2dir(String name) {
        return Direction.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private static String dir2str(Direction d) {
        return d.name().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record LegalValues(Direction[] facing, Set<String> orientation, Set<String> shape) {}
}
