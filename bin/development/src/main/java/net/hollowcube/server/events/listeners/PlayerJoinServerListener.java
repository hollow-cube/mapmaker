package net.hollowcube.server.events.listeners;

import net.hollowcube.server.MapMaker;
import net.hollowcube.server.player.MapMakerPlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoginEvent;

public class PlayerJoinServerListener {
    public static void onPlayerJoinServer(PlayerLoginEvent event) {
        // Set spawn
        final Player player = event.getPlayer();

        player.sendMessage(
                Component.text("Welcome to ", NamedTextColor.WHITE)
                        .append(Component.text("Map Maker!", NamedTextColor.AQUA)));

        event.setSpawningInstance(MapMaker.getInstance().getWorldInstanceManager().getBaseInstance());
        player.setRespawnPoint(new Pos(0, 60, 0));

        //TODO: Probably should do MapMaker.getInstance().loadMapMakerPlayer(player) instead so we have less static things
        MapMakerPlayerManager.getMapMakerPlayerLoader().loadMapMakerPlayer(player);
    }
}
