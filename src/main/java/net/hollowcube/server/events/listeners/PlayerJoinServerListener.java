package net.hollowcube.server.events.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;
import omega.mapmaker.MapMaker;
import omega.mapmaker.player.MapMakerPlayerManager;

public class PlayerJoinServerListener {
    public static void onPlayerJoinServer(GlobalEventHandler handler) {
        // Set spawn
        handler.addListener(PlayerLoginEvent.class, event -> {
            final Player player = event.getPlayer();

            player.sendMessage(
                    Component.text("Welcome to ", NamedTextColor.WHITE)
                            .append(Component.text("Map Maker!", NamedTextColor.AQUA)));

            event.setSpawningInstance(MapMaker.getInstance().getWorldInstanceManager().getBaseInstance());
            player.setRespawnPoint(new Pos(0, 60, 0));

            MapMakerPlayerManager.getMapMakerPlayerLoader().loadMapMakerPlayer(player);
        });
    }
}
