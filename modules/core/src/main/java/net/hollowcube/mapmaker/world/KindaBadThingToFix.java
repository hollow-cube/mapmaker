package net.hollowcube.mapmaker.world;

import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public class KindaBadThingToFix {
    public static Function<Player, MapData> badbadbad = null;

    public static @Nullable MapData getMapFromCurrentWorld(@NotNull Player player) {
        return Objects.requireNonNull(badbadbad).apply(player);
    }
}
