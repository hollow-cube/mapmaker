package net.hollowcube.mapmaker.misc;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.ipc.session.GameServer;
import net.hollowcube.mapmaker.player.JoinMapResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ProxySupport {

    public static void transfer(@NotNull Player player, @NotNull JoinMapResponse server) {
        transfer(player, server.server(), server.serverClusterIp(), server.protocolVersion());
    }

    public static void transfer(@NotNull Player player, @NotNull GameServer server) {
        transfer(player, server.id(), server.clusterIp(), server.protocolVersion());
    }

    /// The proxy hands the protocol to ViaVersion (0 is unknown, and its configured default), and
    /// seals the server id into the cookie if it is draining.
    private static void transfer(@NotNull Player player, @NotNull String server, @NotNull String address, int protocolVersion) {
        var message = new JsonObject();
        message.addProperty("server", server);
        message.addProperty("address", address);
        message.addProperty("protocolVersion", protocolVersion);
        player.sendPluginMessage("mapmaker:transfer", message.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static <T> void transferWithData(@NotNull Player player, @NotNull GameServer server, @NotNull T metadata) {
        // Use a map here so that when we read it back we can check the key when querying, also makes it a bit more stable with changes in the future.
        var transferData = AbstractHttpService.GSON.toJson(Map.of(metadata.getClass().getName(), metadata)).getBytes(StandardCharsets.UTF_8);
        player.getPlayerConnection().storeCookie("mapmaker:transfer_data", transferData);
        transfer(player, server);
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
