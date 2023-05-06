package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.permission.PlatformPermission;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisplayNameBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DisplayNameBuilder.class);
    private static PlayerStorage playerStorage;

    public static void init(PlayerStorage playerStorage) {
        DisplayNameBuilder.playerStorage = playerStorage;
    }

    //TODO move to a better class with diff name
    public static String getDisplayName(String uuid) {
        //todo just cache the player display name on join once and have a reload event/kafka message when something changes that can affect the player.
        String name;
        if ((name = playerStorage.getPlayerByUuid(uuid).getDisplayName()) != null)
            return name;
        return uuid;
    }

    // This might be totally useless
    public static String getDisplayName(Player player) {
        String name;
        if ((name = PlayerData.fromPlayer(player).getDisplayName()) != null)
            return name;
        if ((name = getDisplayName(player.getUuid().toString())) != player.getUuid().toString())
            return name;
        return player.getUsername();
    }

    public static String playerToDisplayName(Player player, PlatformPermissionManager permissionManager) {
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
