package net.hollowcube.mapmaker.model;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.permission.PlatformPermission;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.entity.Player;

import java.util.concurrent.ExecutionException;

public class DisplayNameBuilder {
    private static PlayerStorage playerStorage;

    public static void init(PlayerStorage playerStorage) {
        DisplayNameBuilder.playerStorage = playerStorage;
    }

    //TODO move to a better class with diff name
    public static String getDisplayName(String uuid) {
        try {
            return playerStorage.getPlayerByUuid(uuid).get().getDisplayName();
        } catch (Exception e) {
            return uuid;
        }
    }

    public static String getDisplayName(Player player) {
        return PlayerData.fromPlayer(player).getDisplayName();
    }

    public static String playerToDisplayName(Player player, PlatformPermissionManager permissionManager) {
        String display_name = "";
        String uuid = player.getUuid().toString();

        try {
            for (PlatformPermission permission : PlatformPermission.values()) {
                if (permissionManager.checkPermission(uuid, permission).get()) {
                    display_name += Prefix.getDisplayFromPerm(permission);
                }
            }
        } catch (Exception e) {
            display_name = "";
        }

        display_name += " " + player.getUsername();

        return display_name;
    }
}
