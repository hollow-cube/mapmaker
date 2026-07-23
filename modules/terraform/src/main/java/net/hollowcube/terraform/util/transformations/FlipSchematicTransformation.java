package net.hollowcube.terraform.util.transformations;

import net.hollowcube.common.util.PropertyUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a transformation that flips the schematic across specified axes.
 */
public record FlipSchematicTransformation(boolean x, boolean y, boolean z) implements SchematicTransformation {

    @Override
    public @NotNull Block apply(@NotNull Block block) {
        Map<String, String> properties = block.properties();
        if (properties.isEmpty()) return block;
        Map<String, String> newProperties = new HashMap<>();

        var facing = PropertyUtil.getFacing(properties);
        if (facing != null) newProperties.put("facing", mirror(facing).name().toLowerCase(Locale.ROOT));

        var rotation = PropertyUtil.getRotation(properties);
        if (rotation != null) newProperties.put("rotation", String.valueOf(mirror((int) rotation)));

        var connections = PropertyUtil.getConnections(properties);
        if (connections != null) {
            for (var entry : mirror(connections).entrySet()) {
                newProperties.put(entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }

        var shape = properties.get("shape");
        if (shape != null) newProperties.put("shape", mirrorShape(shape));

        // A single horizontal mirror swaps left and right handed states; mirroring
        // both horizontal axes is a 180 degree rotation, which preserves them.
        if (this.x ^ this.z) {
            swap(properties, newProperties, "hinge", "left", "right");
            swap(properties, newProperties, "type", "left", "right");
        }
        if (this.y) {
            swap(properties, newProperties, "half", "top", "bottom");
            swap(properties, newProperties, "half", "upper", "lower");
            swap(properties, newProperties, "type", "top", "bottom");
            swap(properties, newProperties, "face", "floor", "ceiling");
            swap(properties, newProperties, "attachment", "floor", "ceiling");
            var up = properties.get("up");
            var down = properties.get("down");
            if (up != null && down != null) {
                newProperties.put("up", down);
                newProperties.put("down", up);
            }
        }

        // Apply properties individually since a mirrored value may not exist for a
        // given block (e.g. hoppers cannot face up); such values are kept as-is.
        var result = block;
        for (var entry : newProperties.entrySet()) {
            try {
                result = result.withProperty(entry.getKey(), entry.getValue());
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    @Override
    public @NotNull Point apply(@NotNull Point point, @NotNull Point size, @NotNull Point pivot) {
        int px = pivot.blockX();
        int py = pivot.blockY();
        int pz = pivot.blockZ();

        return new Vec(
                this.x ? 2 * px - point.blockX() : point.blockX(),
                this.y ? 2 * py - point.blockY() : point.blockY(),
                this.z ? 2 * pz - point.blockZ() : point.blockZ()
        );
    }

    private @NotNull Direction mirror(@NotNull Direction direction) {
        return switch (direction) {
            case EAST, WEST -> this.x ? direction.opposite() : direction;
            case UP, DOWN -> this.y ? direction.opposite() : direction;
            case NORTH, SOUTH -> this.z ? direction.opposite() : direction;
        };
    }

    private int mirror(int rotation) {
        var result = rotation;
        if (this.x) result = (16 - result) % 16;
        if (this.z) result = (24 - result) % 16;
        return result;
    }

    private @NotNull Map<Direction, String> mirror(@NotNull Map<Direction, String> connections) {
        return Map.of(
                Direction.NORTH, connections.get(mirror(Direction.NORTH)),
                Direction.EAST, connections.get(mirror(Direction.EAST)),
                Direction.SOUTH, connections.get(mirror(Direction.SOUTH)),
                Direction.WEST, connections.get(mirror(Direction.WEST))
        );
    }

    private @NotNull String mirrorShape(@NotNull String shape) {
        var result = shape;
        // Stair shapes are chiral, see the left/right handling above.
        if (this.x ^ this.z) {
            result = switch (result) {
                case "inner_left" -> "inner_right";
                case "inner_right" -> "inner_left";
                case "outer_left" -> "outer_right";
                case "outer_right" -> "outer_left";
                default -> result;
            };
        }
        // Rail shapes name absolute directions and mirror per axis.
        if (this.x) {
            result = switch (result) {
                case "ascending_east" -> "ascending_west";
                case "ascending_west" -> "ascending_east";
                case "north_east" -> "north_west";
                case "north_west" -> "north_east";
                case "south_east" -> "south_west";
                case "south_west" -> "south_east";
                default -> result;
            };
        }
        if (this.z) {
            result = switch (result) {
                case "ascending_north" -> "ascending_south";
                case "ascending_south" -> "ascending_north";
                case "north_east" -> "south_east";
                case "south_east" -> "north_east";
                case "north_west" -> "south_west";
                case "south_west" -> "north_west";
                default -> result;
            };
        }
        if (this.y) {
            result = switch (result) {
                case "ascending_east" -> "ascending_west";
                case "ascending_west" -> "ascending_east";
                case "ascending_north" -> "ascending_south";
                case "ascending_south" -> "ascending_north";
                default -> result;
            };
        }
        return result;
    }

    private static void swap(Map<String, String> properties, Map<String, String> newProperties, String key, String a, String b) {
        var value = newProperties.getOrDefault(key, properties.get(key));
        if (a.equals(value)) newProperties.put(key, b);
        else if (b.equals(value)) newProperties.put(key, a);
    }
}
