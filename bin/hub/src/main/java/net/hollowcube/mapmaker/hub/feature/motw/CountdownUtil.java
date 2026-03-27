package net.hollowcube.mapmaker.hub.feature.motw;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class CountdownUtil {
    private static final Point[] NUMBER_POSITIONS = new Point[]{
            new Vec(-30, 53, 59),
            new Vec(-35, 53, 59),
            new Vec(-38, 53, 59),
            new Vec(-43, 53, 59),
            new Vec(-46, 53, 59),
    };

    private static final Schematic[] NUMBER_SCHEMATICS = new Schematic[10];

    static {
        for (int i = 0; i < 10; i++) {
            try (var is = CountdownUtil.class.getResourceAsStream("/motw/motw_num_" + i + ".schem")) {
                NUMBER_SCHEMATICS[i] = SchematicReader.sponge().read(Objects.requireNonNull(is).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void applySchematic(@NotNull Instance instance, int position, int number) {
        var schematic = NUMBER_SCHEMATICS[number];
        var offset = NUMBER_POSITIONS[position];
        schematic.forEachBlock((pos, block) -> instance.setBlock(offset.add(pos), block));
    }
}
