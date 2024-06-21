package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

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
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof PlayingMapWorld)) return;

        FutureUtil.submitVirtual(() -> {
            player.removeTag(SPECTATOR_CHECKPOINT);
            world.removePlayer(player); // Remove spectator
            world.addPlayer(player); // Add back as playing player
        });
    }
}
