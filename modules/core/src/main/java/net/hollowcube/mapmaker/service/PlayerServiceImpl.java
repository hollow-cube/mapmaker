package net.hollowcube.mapmaker.service;

import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.mojang.MojangUtils;
import org.jetbrains.annotations.NotNull;

public class PlayerServiceImpl implements PlayerService {
    private final PlayerStorage playerStorage;

    public PlayerServiceImpl(
            @NotNull PlayerStorage playerStorage
    ) {
        this.playerStorage = playerStorage;
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull String playerId) {
        try {
            var playerData = playerStorage.getPlayerByUuid(playerId);
            return Component.text(playerData.getUsername());
        } catch (PlayerStorage.NotFoundError e) {
            // If the player doesnt exist, fetch their name from Mojang
            var name = MojangUtils.fromUuid(playerId);
            if (name == null) {
                return Component.text("Missing Name");
            }
            return Component.text(name.get("name").getAsString());
        }
    }


    // Yoinked from DisplayNameBuilder
    private static String playerToDisplayName(Player player) {
        StringBuilder displayName = new StringBuilder();
        String uuid = player.getUuid().toString();

        try {
//            for (PlatformPermission permission : PlatformPermission.values()) {
//                if (permissionManager.checkPermission(uuid, permission)) {
//                    displayName.append(Prefix.getDisplayFromPerm(permission));
//                }
//            }
        } catch (Exception e) {
            displayName = new StringBuilder();
        }

        if (!displayName.isEmpty())
            displayName.append(" ");
        displayName.append(player.getUsername());

        return displayName.toString();
    }

}
