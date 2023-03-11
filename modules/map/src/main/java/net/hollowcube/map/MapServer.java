package net.hollowcube.map;

import net.hollowcube.canvas.section.Section;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.WorldManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface MapServer {

    @NotNull WorldManager worldManager();

    @NotNull MapStorage mapStorage();

    @NotNull SaveStateStorage saveStateStorage();

    @NotNull PlatformPermissionManager platformPermissions();

    void openGUIForPlayer(@NotNull Player player, @NotNull Section gui);

}
