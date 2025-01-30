package net.hollowcube.terraform.util.transformations;

import net.hollowcube.common.types.Axis;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.PropertyUtil;
import net.hollowcube.schem.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a transformation that rotates the schematic around a specified axis in 90-degree increments.
 */
public record RotateSchematicTransformation(@NotNull Axis axis, @NotNull Rotation rotation) implements SchematicTransformation {

    @Override
    public @NotNull Block apply(@NotNull Block block) {
        if (this.axis == Axis.Y) {
            Map<String, String> properties = block.properties();
            Map<String, String> newProperties = new HashMap<>();
            var facing = OpUtils.map(PropertyUtil.getFacing(properties), this.rotation::rotate);
            var axis = OpUtils.map(PropertyUtil.getAxis(properties), this.rotation::rotate);
            var rotation = OpUtils.map(PropertyUtil.getRotation(properties), this.rotation::rotate);
            var connections = OpUtils.map(PropertyUtil.getConnections(properties), this.rotation::rotate);

            if (facing != null) newProperties.put("facing", facing.name().toLowerCase(Locale.ROOT));
            if (axis != null) newProperties.put("axis", axis.name().toLowerCase(Locale.ROOT));
            if (rotation != null) newProperties.put("rotation", rotation.toString());
            if (connections != null) {
                for (var entry : connections.entrySet()) {
                    newProperties.put(entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
                }
            }

            return block.withProperties(newProperties);
        }
        return block;
    }

    @Override
    public @NotNull Point apply(@NotNull Point point, @NotNull Point size, @NotNull Point pivot) {
        int px = pivot.blockX();
        int py = pivot.blockY();
        int pz = pivot.blockZ();

        int x = point.blockX() - px;
        int y = point.blockY() - py;
        int z = point.blockZ() - pz;

        return switch (this.rotation) {
            case CLOCKWISE_90 -> switch (this.axis) {
                case Y -> new Vec(-z + px, y + py, x + pz);
                case X -> new Vec(x + px, -z + py, y + pz);
                case Z -> new Vec(-y + px, x + py, z + pz);
            };
            case CLOCKWISE_180 -> switch (this.axis) {
                case Y -> new Vec(-x + px, y + py, -z + pz);
                case X -> new Vec(-x + px, -z + py, y + pz);
                case Z -> new Vec(-y + px, x + py, z + pz);
            };
            case CLOCKWISE_270 -> switch (this.axis) {
                case Y -> new Vec(z + px, y + py, -x + pz);
                case X -> new Vec(x + px, z + py, y + pz);
                case Z -> new Vec(y + px, -x + py, z + pz);
            };
            default -> point;
        };
    }
}
