package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapDetailsView extends View {
    private static final System.Logger logger = System.getLogger(MapDetailsView.class.getName());

    private @ContextObject Handler handler;

    private MapData map;

    public MapDetailsView(@NotNull Context context, @NotNull MapData map) {
        super(context);
        this.map = map;
    }

    @Action(value = "play_map", async = true)
    public void handlePlayMap(@NotNull Player player) {
        try {
            handler.playMap(player, map.getId());
            player.closeInventory();
        } catch (Exception e) {
            // If an error occurs here the player is still here, it is our responsibility to handle this (with an error)
            logger.log(System.Logger.Level.ERROR, "failed to join map {} for {}: {}",
                    map.getId(), PlayerData.fromPlayer(player).getId(), e.getMessage());
            player.sendMessage(Component.translatable("command.generic.unknown_error"));
        }
    }

}
