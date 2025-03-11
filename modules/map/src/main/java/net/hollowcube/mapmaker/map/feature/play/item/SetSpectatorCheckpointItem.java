package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.util.GenericTempActionBarProvider;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetSpectatorCheckpointItem extends ItemHandler {

    public static final String ID_SPECTATOR = "mapmaker:set_spectator_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE_SPECTATOR = new SetSpectatorCheckpointItem(ID_SPECTATOR);
    public static final String ID_TESTING = "mapmaker:set_testing_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE_TESTING = new SetSpectatorCheckpointItem(ID_TESTING);

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/set_checkpoint"));

    private SetSpectatorCheckpointItem(@NotNull String id) {
        super(id, RIGHT_CLICK_ANY);
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
        Player player = click.player();
        ActionBar actionBar = ActionBar.forPlayer(player);
//        if (player.isSneaking()) {
//            SpectateHandler.setCheckpoint(player, null);
//            actionBar.addProvider(new GenericTempActionBarProvider("Cleared temporary checkpoint!", 1000));
//        } else {
            SpectateHandler.setCheckpoint(player, player.getPosition());
            actionBar.addProvider(new GenericTempActionBarProvider("Added temporary checkpoint!", 1000));
//        }
    }


}
