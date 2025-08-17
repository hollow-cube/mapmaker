package net.hollowcube.mapmaker.dev;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class DevServerBridge implements ServerBridge {
    private final DevServer server;

    public DevServerBridge(@NotNull DevServer server) {
        this.server = server;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
        FutureUtil.assertThread();
        var playerId = PlayerData.fromPlayer(player).id();
        var map = server.mapService().getMap(playerId, mapId);

        var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
        if (playerProtocolVersion < map.protocolVersion()) {
            player.sendMessage(Component.translatable("map_join.wrongversion",
                    Component.text(map.name()), Component.text(ProtocolVersions.getProtocolName(map.protocolVersion()))));
            return;
        }

        joinMapInternal(player, mapId, joinMapState);
    }

    @Override
    public void joinHub(@NotNull Player player) {
        joinMapInternal(player, HubServer.HUB_MAP_DATA.id(), JoinMapState.PLAYING);
    }

    private void joinMapInternal(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        var playerId = PlayerData.fromPlayer(player).id();

        server.addPendingJoin(playerId, mapId, joinMapState.name().toLowerCase(Locale.ROOT));

        // We need to remove the player from the map before entering configuration, because by the time we get
        // remove from instance event, the player already had their position reset (ie they are at 0,0,0).
        var world = MapWorld.forPlayer(player);
        if (world == null) {
            MinecraftServer.getSchedulerManager().scheduleEndOfTick(player::startConfigurationPhase);
        } else {
            world.scheduleRemovePlayer(player).thenRun(player::startConfigurationPhase);
        }
    }

}
