package net.hollowcube.mapmaker.service;

import net.hollowcube.mapmaker.model.Prefix;
import net.hollowcube.mapmaker.permission.PlatformPermission;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerServiceImpl implements PlayerService {
    private final PlayerStorage playerStorage;
    private final PlatformPermissionManager platformPermissions;

    public PlayerServiceImpl(
            @NotNull PlayerStorage playerStorage,
            @NotNull PlatformPermissionManager platformPermissions
    ) {
        this.playerStorage = playerStorage;
        this.platformPermissions = platformPermissions;
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull String playerId) {
        try {
            var playerData = playerStorage.getPlayerByUuid(playerId);
            return Component.text(playerData.getUsername());
        } catch (PlayerStorage.NotFoundError e) {
            throw new NotFoundError();
        }
    }


    // Yoinked from DisplayNameBuilder
    private static String playerToDisplayName(Player player, PlatformPermissionManager permissionManager) {
        StringBuilder displayName = new StringBuilder();
        String uuid = player.getUuid().toString();

        try {
            for (PlatformPermission permission : PlatformPermission.values()) {
                if (permissionManager.checkPermission(uuid, permission)) {
                    displayName.append(Prefix.getDisplayFromPerm(permission));
                }
            }
        } catch (Exception e) {
            displayName = new StringBuilder();
        }

        if (!displayName.isEmpty())
            displayName.append(" ");
        displayName.append(player.getUsername());

        return displayName.toString();
    }

}
