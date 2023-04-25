package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.permission.PlatformPermission;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class DisplayNameBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DisplayNameBuilder.class);
    private static PlayerStorage playerStorage;

    public static void init(PlayerStorage playerStorage) {
        DisplayNameBuilder.playerStorage = playerStorage;
    }

    //TODO move to a better class with diff name
    public static String getDisplayName(String uuid) {
        try {
            String name;
            if ((name = playerStorage.getPlayerByUuid(uuid).get().getDisplayName()) != null)
                return name;
        } catch (PlayerStorage.NotFoundError e) {
            logger.info("Tried to get display name for player not in storage" + uuid);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
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
                if (permissionManager.checkPermission(uuid, permission).get()) {
                    displayName.append(Prefix.getDisplayFromPerm(permission));
                }
            }
        } catch (Exception e) {
            displayName = new StringBuilder();
        }

        if (!displayName.isEmpty())
            displayName.append(" ").append(player.getUsername());

        return displayName.toString();
    }
}
