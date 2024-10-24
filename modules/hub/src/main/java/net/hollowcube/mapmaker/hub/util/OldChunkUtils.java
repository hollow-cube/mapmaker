package net.hollowcube.mapmaker.hub.util;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class OldChunkUtils {


    /**
     * New implementation comes from <a href="https://github.com/KryptonMC/Krypton/blob/a9eff5463328f34072cdaf37aae3e77b14fcac93/server/src/main/kotlin/org/kryptonmc/krypton/util/math/Maths.kt#L62">Krypton</a>
     * which comes from kotlin port by <a href="https://github.com/Esophose">Esophose</a>, which comes from <a href="https://stackoverflow.com/questions/398299/looping-in-a-spiral">a stackoverflow answer</a>.
     */
    public static void forChunksInRange(int chunkX, int chunkZ, int range, BiConsumer<Integer, Integer> consumer) {
        // Send in spiral around the center chunk
        // Note: its not really required to start at the center anymore since the chunk queue is sorted by distance,
        //       however we still should send a circle so this method is still fine, and good for any other case a
        //       spiral might be needed.
        consumer.accept(chunkX, chunkZ);
        for (int id = 1; id < (range * 2 + 1) * (range * 2 + 1); id++) {
            var index = id - 1;

            // compute radius (inverse arithmetic sum of 8 + 16 + 24 + ...)
            var radius = (int) Math.floor((Math.sqrt(index + 1.0) - 1) / 2) + 1;

            // compute total point on radius -1 (arithmetic sum of 8 + 16 + 24 + ...)
            var p = 8 * radius * (radius - 1) / 2;

            // points by face
            var en = radius * 2;

            // compute de position and shift it so the first is (-r, -r) but (-r + 1, -r)
            // so the square can connect
            var a = (1 + index - p) % (radius * 8);

            switch (a / (radius * 2)) {
                // find the face (0 = top, 1 = right, 2 = bottom, 3 = left)
                case 0 -> consumer.accept(a - radius + chunkX, -radius + chunkZ);
                case 1 -> consumer.accept(radius + chunkX, a % en - radius + chunkZ);
                case 2 -> consumer.accept(radius - a % en + chunkX, radius + chunkZ);
                case 3 -> consumer.accept(-radius + chunkX, radius - a % en + chunkZ);
                default -> throw new IllegalStateException("unreachable");
            }
        }
    }

    public static void forChunksInRange(@NotNull Point point, int range, BiConsumer<Integer, Integer> consumer) {
        forChunksInRange(point.chunkX(), point.chunkZ(), range, consumer);
    }
}
