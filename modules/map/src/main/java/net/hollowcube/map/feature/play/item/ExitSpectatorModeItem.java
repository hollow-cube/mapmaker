package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
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
        player.removeTag(SPECTATOR_CHECKPOINT);
        var world = (InternalMapWorld) MapWorld.forPlayer(player);
        //todo should not depend on implementation details of InternalMapWorld

        world.removePlayer(player);
        if (world instanceof PlayingMapWorld playingWorld) {
            CompletableFuture.runAsync(() -> {
                playingWorld.removePlayer(player, false);
                playingWorld.acceptPlayer(player, true);
            });
        }
    }

}
