package net.hollowcube.mapmaker.map.block;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.debug.WireColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
@FunctionalInterface
public interface Trigger {

    void onEnter(MapWorld world, Player player);

    default void onExit(MapWorld world, Player player) {
    }

    default @Nullable WireColor debugColor() {
        return null;
    }

}
