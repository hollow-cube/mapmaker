package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.event.MapPlayerResetTriggerEvent;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.minestom.server.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ExitSpectatorModeItem extends ItemHandler {

    public static final String ID = "mapmaker:exit_spectator";
    public static final ExitSpectatorModeItem INSTANCE = new ExitSpectatorModeItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_spectator"));

    private ExitSpectatorModeItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = (InternalMapWorld) MapWorld.forPlayer(player);
        //todo should not depend on implementation details of InternalMapWorld

        world.removePlayer(player);
        if (world instanceof PlayingMapWorld playingWorld) {
            CompletableFuture.runAsync(() -> {
                playingWorld.removePlayer(player, false);
                playingWorld.acceptPlayer(player, true);
                if (world.map().settings().isOnlySprint()) {
                    var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
                    if (checkpoint != null) {
                        player.teleport(checkpoint);
                    } else {
                        EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
                    }
                    player.sendMessage(Component.translatable("map.spectator_mode.only_sprint"));
                }
                player.removeTag(SPECTATOR_CHECKPOINT);
            });
        }
    }
}
