package net.hollowcube.mapmaker.misc;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ProxySupport {

    public static void transfer(@NotNull Player player, @NotNull String address) {
        player.sendPluginMessage("mapmaker:transfer", address.getBytes(StandardCharsets.UTF_8));
    }

    public static <T> void transferWithData(@NotNull Player player, @NotNull String address, @NotNull T metadata) {
        // Use a map here so that when we read it back we can check the key when querying, also makes it a bit more stable with changes in the future.
        var transferData = AbstractHttpService.GSON.toJson(Map.of(metadata.getClass().getName(), metadata)).getBytes(StandardCharsets.UTF_8);
        player.getPlayerConnection().storeCookie("mapmaker:transfer_data", transferData);
        transfer(player, address);
    }

    public static <T> @Nullable T getTransferData(@NotNull Player player, @NotNull Class<T> type) {
        FutureUtil.assertThread();
        var rawTransferData = FutureUtil.getUnchecked(player.getPlayerConnection().fetchCookie("mapmaker:transfer_data"));
        if (rawTransferData == null || rawTransferData.length == 0) return null;

        var transferData = AbstractHttpService.GSON.fromJson(new String(rawTransferData, StandardCharsets.UTF_8), JsonObject.class);
        if (!transferData.has(type.getName())) return null;

        return AbstractHttpService.GSON.fromJson(transferData.get(type.getName()), type);
    }

}
